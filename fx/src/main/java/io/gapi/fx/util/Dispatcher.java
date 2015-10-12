// Copyright 2012 Google Inc. All Rights Reserved.

package io.gapi.fx.util;

import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * A helper class for {@link GenericVisitor} which implements dispatching
 * of methods based on parameter type and annotations.
 */
final class Dispatcher<BaseType> {

  /**
   * A helper class defining a key to lookup a dispatcher in a cache.
   */
  @AutoValue
  abstract static class Key {

    abstract Class<?> baseType();
    abstract Class<? extends Annotation> marker();
    abstract Class<?> provider();

    static Key create(Class<?> baseType, Class<? extends Annotation> marker, Class<?> provider) {
      return new AutoValue_Dispatcher_Key(baseType, marker, provider);
    }
  }

  /**
   * A loading cache for dispatchers. We cache dispatchers as building up the dispatching
   * table is expensive; also, dispatchers 'learn' over time and we want to preserve this
   * knowledge.
   */
  private static final LoadingCache<Key, Dispatcher<?>>
    CACHE = CacheBuilder.newBuilder().build(new CacheLoader<Key, Dispatcher<?>>() {
      @Override
      public Dispatcher<?> load(Key key) throws Exception {
        return createDispatcher(key.baseType(), key.marker(), key.provider());
      }
    });

  // A helper to go from ? to some T
  private static <T> Dispatcher<T> createDispatcher(Class<T> baseType,
      Class<? extends Annotation> marker, Class<?> provider) {
    return new Dispatcher<T>(baseType, marker, provider);
  }

  private final Class<BaseType> baseType;
  private final LoadingCache<Class<? extends BaseType>, Optional<FastMethod>> dispatchTable;

  /**
   * Creates a dispatcher which dispatches over methods which are annotated with
   * {@code marker} and have an instance of {@code BaseType} as their only parameter.
   * This method will reuse an existing dispatcher from a cache or construct a new one.
   */
  @SuppressWarnings("unchecked") // valid by construction
  static <T> Dispatcher<T> getDispatcher(Class<T> baseType,
      Class<? extends Annotation> marker, Class<?> provider) {
    try {
      return (Dispatcher<T>) CACHE.get(Key.create(baseType, marker, provider));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  private Dispatcher(Class<BaseType> baseType, Class<? extends Annotation> marker,
      Class<?> provider) {
    this.baseType = baseType;
    this.dispatchTable = CacheBuilder.newBuilder().build(new IncrementalTableLoader());
    initialize(marker, provider);
  }

  /**
   * Initializes the dispatcher table. This maps every type
   * to precisely the method which handles it. At lookup time we
   * will incrementally populate the table with additional entries
   * for super-types.
   */
  private void initialize(Class<? extends Annotation> marker, Class<?> provider) {
    Class<?> providerSuper = provider.getSuperclass();
    if (providerSuper != null && providerSuper != Object.class) {
      // First get methods from super class. They can be overridden later.
      initialize(marker, providerSuper);
    }
    // Now get methods from this provider.
    FastClass fastProvider = FastClass.create(provider);
    for (Method method : provider.getDeclaredMethods()) {
      Annotation dispatched = method.getAnnotation(marker);
      if (dispatched == null) {
        continue;
      }

      Preconditions.checkState((method.getModifiers() & Modifier.STATIC) == 0,
          "%s must not be static", method);
      Preconditions.checkState(method.getParameterTypes().length == 1,
          "%s must have exactly one parameter", method);
      @SuppressWarnings("unchecked") // checked at runtime
      Class<? extends BaseType> dispatchedOn =
          (Class<? extends BaseType>) method.getParameterTypes()[0];
      Preconditions.checkState(baseType.isAssignableFrom(dispatchedOn),
          "%s parameter must be assignable to %s", method, baseType);
      dispatchTable.getIfPresent(dispatchedOn);

      Optional<FastMethod> oldMethod = dispatchTable.getIfPresent(dispatchedOn);
      if (oldMethod != null && oldMethod.get().getDeclaringClass() == provider) {
        throw new IllegalStateException(String.format(
            "%s clashes with already configured %s from same class %s",
            method, oldMethod.get().getJavaMethod(), provider));
      }
      dispatchTable.put(dispatchedOn, Optional.of(fastProvider.getMethod(method)));
    }
  }

  /**
   * Delivers the {@link FastMethod} which can handle an object of given type.
   * Delivers the method with most specific type, or null, if none exists.
   */
  FastMethod getMethod(Class<? extends BaseType> type) {
    Optional<FastMethod> result = dispatchTable.getUnchecked(type);
    return result.isPresent() ? result.get() : null;
  }

  /**
   * A helper class which populates the dispatch table with mappings for
   * methods defined by super types.
   */
  private class IncrementalTableLoader
      extends CacheLoader<Class<? extends BaseType>, Optional<FastMethod>> {

    @Override
    public Optional<FastMethod> load(Class<? extends BaseType> type) throws Exception {
      // try to use super type
      Class<?> rawSuperType = type.getSuperclass();
      if (rawSuperType == null || !baseType.isAssignableFrom(rawSuperType)) {
        return Optional.absent();
      }
      @SuppressWarnings("unchecked") // checked at runtime
      Class<? extends BaseType> superType = (Class<? extends BaseType>) rawSuperType;
      return dispatchTable.getUnchecked(superType);
    }
  }
}

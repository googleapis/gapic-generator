package io.gapi.fx.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Key;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Base class for all model elements.
 *
 * <p>
 * Elements allow to attach dynamic attributes based on a typed key, which are typically established
 * by a {@link Processor}. The decision whether to use a dynamic attribute or a plain getter/setter
 * which is hard-wired into the object model depends on whether the expressed information is
 * globally relevant for many processor or only used locally by a tool. For example, derived type
 * information is considered to be globally relevant and hard-wired into the object model, so it is
 * easy discoverable.
 */
public abstract class Element {

  private final Map<Key<?>, Object> attributes = Maps.newHashMap();

  /**
   * Returns the model associated with this element.
   */
  public abstract Model getModel();

  /**
   * Returns the location associated with this element.
   */
  public abstract Location getLocation();

  /**
   * Returns a fully qualified name for the element.
   */
  public abstract String getFullName();

  /**
   * Returns a simple name for the element.
   */
  public abstract String getSimpleName();


  /**
   * Puts an attribute value, returning the old one.
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public <T> T putAttribute(Key<T> key, T value) {
    return (T) attributes.put(key, Preconditions.checkNotNull(value));
  }

  /**
   * Adds an attribute value.
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public <T> void addAttribute(Key<List<T>> key, T value) {
    List<T> list = getAttribute(key);
    if (list == null) {
      list = Lists.newArrayList();
      putAttribute(key, list);
    }
    list.add(value);
  }

  /**
   * Removes an attribute value, returning the old one.
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public <T> T removeAttribute(Key<T> key) {
    return (T) attributes.remove(key);
  }

  /**
   * Gets an attribute value.
   */
  @SuppressWarnings("unchecked")
  public <T> T getAttribute(Key<T> key) {
    return (T) attributes.get(key);
  }

  /**
   * Gets an attribute value if it exists, otherwise the given default value.
   */
  @SuppressWarnings("unchecked")
  public <T> T getAttributeOrDefault(Key<T> key, T defaultValue) {
    T value = (T) attributes.get(key);
    if (value != null) {
      return value;
    }
    return defaultValue;
  }

  /**
   * Checks whether an attribute is present.
   */
  @Nullable
  public boolean hasAttribute(Key<?> key) {
    return attributes.containsKey(key);
  }
}

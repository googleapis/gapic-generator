package io.gapi.fx.snippet;

import io.gapi.fx.snippet.SnippetSet.EvalException;
import io.gapi.fx.snippet.SnippetSet.Issue;
import com.google.auto.value.AutoValue;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Internal representation of the syntax of a snippet body.
 */
abstract class Elem {

  /**
   * Represents a key for a loading cache for method lookup.
   */
  @AutoValue
  protected abstract static class ReflectedMethodKey {
    protected abstract Class<?> clazz();
    protected abstract String name();
    protected abstract int arity();

    protected static ReflectedMethodKey create(Class<?> clazz, String name, int arity) {
      return new AutoValue_Elem_ReflectedMethodKey(clazz, name, arity);
    }
  }

  /**
   * A global loading cache which maps by class, method name, and arity to the matching public
   * methods.
   */
  private static final LoadingCache<ReflectedMethodKey, List<Method>> METHOD_CACHE =
      CacheBuilder.newBuilder().build(new CacheLoader<ReflectedMethodKey, List<Method>>() {

        @Override public List<Method> load(ReflectedMethodKey key) throws Exception {
          ImmutableList.Builder<Method> builder = ImmutableList.builder();
          String name = key.name();
          int arity = key.arity();
          for (Method method : key.clazz().getMethods()) {
            if (method.getName().equals(name) && method.getParameterTypes().length == arity) {
              method.setAccessible(true);
              builder.add(method);
            }
          }
          return builder.build();
        }
  });

  abstract Location location();
  abstract Object eval(Context context);

  /**
   * Evaluates the given arguments.
   */
  private static List<Object> evalArgs(Context context, Iterable<Elem> elems) {
    ImmutableList.Builder<Object> argsBuilder = ImmutableList.builder();
    for (Elem elem : elems) {
      argsBuilder.add(elem.eval(context));
    }
    return argsBuilder.build();
  }

  @AutoValue
  abstract static class Block extends Elem {
    abstract boolean breakBeforeIfNotEmpty();
    abstract List<Elem> elems();

    static Block create(boolean breakBefore, Elem elem) {
      return new AutoValue_Elem_Block(Location.UNUSED, breakBefore, ImmutableList.of(elem));
    }

    static Block create(boolean breakBefore, List<Elem> elems) {
      return new AutoValue_Elem_Block(Location.UNUSED, breakBefore, elems);
    }

    @Override
    Object eval(Context context) {
      Doc content = Snippet.evalElems(context, elems()).align();
      if (breakBeforeIfNotEmpty() && !content.isWhitespace()) {
        return Doc.BREAK.add(content);
      }
      return content;
    }
  }

  /**
   * Represents a literal.
   */
  @AutoValue
  abstract static class Lit extends Elem {

    abstract Doc doc();

    static Lit create(Location location, Doc doc) {
      return new AutoValue_Elem_Lit(location, doc);
    }

    @Override
    Object eval(Context context) {
      return doc();
    }
  }

  /**
   * Represents a variable reference.
   */
  @AutoValue
  abstract static class Ref extends Elem {

    abstract String name();

    static Ref create(Location location, String name) {
      return new AutoValue_Elem_Ref(location, name);
    }

    @Override
    Object eval(Context context) {
      Object result = context.getVar(name());
      if (result == null) {
        throw new EvalException(location(), "unbound variable '%s'", name());
      }
      return result;
    }
  }

  /**
   * Represents a reflected value, either a field or a method invocation.
   */
  @AutoValue
  abstract static class Reflect extends Elem {

    abstract Elem target();
    abstract String name();
    abstract ImmutableList<Elem> args();

    static Reflect create(Location location, Elem target, String name,
        Iterable<Elem> args) {
      return new AutoValue_Elem_Reflect(location, target, name,
          ImmutableList.copyOf(args));
    }

    @Override
    Object eval(Context context) {

      // Evaluate the target element.
      Object target = target().eval(context);
      if (target == null) {
        throw new EvalException(location(), "access target for '%s' undefined.", name());
      }

      // Evaluate arguments.
      List<Object> args = evalArgs(context, args());

      // If zero arguments, try field access.
      Class<?> clazz = target.getClass();
      if (args.isEmpty()) {
        Field field;
        try {
          field = clazz.getField(name());
        } catch (NoSuchFieldException e) {
          field = null;
        }
        if (field != null) {
          try {
            field.setAccessible(true);
            return field.get(target);
          } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new EvalException(location(),
                "exception when accessing field '%s': %s.",
                name(),
                e.getMessage());
          }
        }
      }

      // Try as method.
      Object result = tryEvalAsMethod(clazz, target, args);

      // If the target is iterable, try wrapping it in a fluent iterable. This enables
      // instance methods like append, first, etc.
      if (result == null
          && target instanceof Iterable<?> && (!(target instanceof FluentIterable<?>))) {
        target = FluentIterable.from((Iterable<?>) target);
        result = tryEvalAsMethod(target.getClass(), target, args);
      }

      if (result != null) {
        return result;
      }

      throw new EvalException(location(),
          "field or method '%s' unknown in value of type '%s', or has ambigious overloads.",
          name(), target.getClass().getSimpleName());
    }

    /**
     * Try to evaluate method.
     */
    private Object tryEvalAsMethod(Class<?> clazz, Object target, List<Object> args) {
      Method method = findMethod(clazz, args);
      if (method == null) {
        return null;
      }
      try {
        return Values.ensureNotNull(location(), method.invoke(target,
            Values.convertArgs(location(), method.getParameterTypes(), args)));
      } catch (IllegalAccessException | IllegalArgumentException e) {
        throw new EvalException(location(),
            "exception when accessing method '%s': %s.",
            name(),
            e.getMessage());
      } catch (InvocationTargetException e) {
        throw new EvalException(Issue.create(location(),
            "exception when invoking method '%s': %s.", name(),
            e.getCause()));
      }
    }

    /**
     * Try to find method. If there is a unique match based on name and arity, we return that
     * one, and attempts will be made to convert the arguments to the parameter types. If there
     * are multiple matches, we require that only one matches the provided parameter types,
     * without any conversions involved.
     */
    private Method findMethod(Class<?> clazz, final List<Object> args) {

      // Get candidate methods from loading cache.
      List<Method> cands;
      try {
        cands = METHOD_CACHE.get(ReflectedMethodKey.create(clazz, name(), args.size()));
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }

      switch (cands.size()) {
        case 0:
          // No matching method found
          return null;
        case 1:
          // Found exactly one method. Use this one. Parameter conversions will be applied.
          return cands.get(0);
        default:
          // There is more than one method. There must be one which uniquely matches the
          // parameter types before we apply any conversions.
          // TODO(wgg): we could consider doing best fit here considering parameter subtyping,
          //   reflecting the Java source language resolution rules.
          cands = FluentIterable.from(cands).filter(new Predicate<Method>() {

            @Override public boolean apply(Method cand) {
              int i = 0;
              for (Object arg : args) {
                Values.ensureNotNull(location(),  arg);
                if (!cand.getParameterTypes()[i++].isAssignableFrom(arg.getClass())) {
                  return false;
                }
              }
              return true;
            }
          }).toList();
          return cands.size() == 1 ? cands.get(0) : null;
      }
    }
  }

  /**
   * Represents a snippet call.
   */
  @AutoValue
  abstract static class Call extends Elem {

    abstract String name();
    abstract ImmutableList<Elem> args();

    static Call create(Location location, String name,
        Iterable<Elem> args) {
      return new AutoValue_Elem_Call(location, name,
          ImmutableList.copyOf(args));
    }

    @Override
    Object eval(Context context) {
      Snippet snippet = context.getSnippet(location(), name(), args().size());
      if (snippet == null) {
        throw new EvalException(location(),
            "snippet '%s(%s)' unknown.", name(), args().size());
      }
      return SnippetSet.tryEval(location(), Object.class,
          snippet, context, evalArgs(context, args()));
    }
  }

  /**
   * Represents an operator.
   */
  @AutoValue
  abstract static class Operator extends Elem {

    enum Kind {
      EQUALS,
      NOT_EQUALS,
      LESS,
      LESS_EQUAL,
      GREATER,
      GREATER_EQUAL
    }

    abstract Kind kind();
    abstract Elem left();
    abstract Elem right();

    static Operator create(Location location, Kind kind, Elem left, Elem right) {
      return new AutoValue_Elem_Operator(location, kind, left, right);
    }

    @Override
    Object eval(Context context) {
      Object leftValue = left().eval(context);
      Object rightValue = right().eval(context);
      switch (kind()) {
        case EQUALS:
          return Values.equal(leftValue, rightValue);
        case NOT_EQUALS:
          return !Values.equal(leftValue, rightValue);
        case LESS:
          return Values.less(leftValue, rightValue);
        case LESS_EQUAL:
          return Values.lessEqual(leftValue, rightValue);
        case GREATER:
          return Values.less(rightValue, leftValue);
        case GREATER_EQUAL:
          return Values.lessEqual(rightValue, leftValue);
        default:
          throw new IllegalStateException("Unknow operator kind: " + kind());
      }
    }
  }

  /**
   * Represents a conditional.
   */
  @AutoValue
  abstract static class Cond extends Elem {
    abstract Elem cond();
    abstract ImmutableList<Elem> thenElems();
    @Nullable abstract ImmutableList<Elem> elseElems();

    static Cond create(Location location, Elem cond, List<Elem> thenElems,
        List<Elem> elseElems) {
      return new AutoValue_Elem_Cond(location, cond,
          ImmutableList.copyOf(thenElems),
          elseElems == null ? null : ImmutableList.copyOf(elseElems));
    }

    @Override
    Object eval(Context context) {
      if (Values.isTrue(cond().eval(context))) {
        return Snippet.evalElems(context, thenElems());
      }
      if (elseElems() != null) {
        return Snippet.evalElems(context, elseElems());
      }
      return Doc.EMPTY;
    }
  }

  /**
   * Represents an iterator loop.
   */
  @AutoValue
  abstract static class Join extends Elem {
    abstract String var();
    abstract Elem generator();
    abstract Layout layout();
    abstract ImmutableList<Elem> elems();

    static Join create(Location location, String var, Elem generator, Layout layout,
        List<Elem> bodyElems) {
      return new AutoValue_Elem_Join(location, var,
          generator, layout, ImmutableList.copyOf(bodyElems));
    }

    @Override
    Object eval(final Context context) {
      Object gen = generator().eval(context);
      if (!(gen instanceof Iterable)) {
        throw new EvalException(location(),
            "generator for variable '%s' is not iterable.", var());
      }
      Doc result = Doc.EMPTY;
      boolean first = true;
      for (Object val : (Iterable<?>) gen) {
        try {
          context.enterScope();
          context.bind(var(), val);
          if (first) {
            first = false;
          } else {
            result = result.add(layout().separator());
          }
          result = result.add(Snippet.evalElems(context, elems()));
        } finally {
          context.exitScope();
        }
      }
      return result.group(layout().groupKind()).nest(layout().nest());
    }
  }

  /**
   * Represents a let binding.
   */
  @AutoValue
  abstract static class Let extends Elem {
    abstract String var();
    abstract Elem value();
    abstract ImmutableList<Elem> elems();

    static Let create(Location location, String var, Elem value,
        List<Elem> bodyElems) {
      return new AutoValue_Elem_Let(location, var,
          value, ImmutableList.copyOf(bodyElems));
    }

    @Override
    Object eval(final Context context) {
      Object value = value().eval(context);
      try {
        context.enterScope();
        context.bind(var(), value);
        return Snippet.evalElems(context, elems());
      } finally {
        context.exitScope();
      }
    }
  }

  /**
   * Represents a switch.
   */
  @AutoValue
  abstract static class Switch extends Elem {
    abstract Elem selector();
    abstract ImmutableList<Elem.Case> cases();
    @Nullable abstract ImmutableList<Elem> defaultElems();

    static Switch create(Location location, Elem selector,
        Iterable<Case> cases, @Nullable List<Elem> defaultElems) {
      return new AutoValue_Elem_Switch(location, selector,
          ImmutableList.copyOf(cases),
          defaultElems != null ? ImmutableList.copyOf(defaultElems) : null);
    }

    @Override
    Object eval(Context context) {
      Object selector = selector().eval(context);
      for (Elem.Case acase : cases()) {
        Object value = acase.value().eval(context);
        if (Values.equal(selector, value)) {
          return Snippet.evalElems(context, acase.elems());
        }
      }
      if (defaultElems() != null) {
        return Snippet.evalElems(context, defaultElems());
      }
      throw new EvalException(location(), "no case evaluates to value '%s'", selector);
    }
  }

  /**
   * Represents a case for a switch.
   */
  @AutoValue
  abstract static class Case {
    abstract Elem value();
    abstract ImmutableList<Elem> elems();

    static Case create(Elem value, List<Elem> caseElems) {
      return new AutoValue_Elem_Case(value, ImmutableList.copyOf(caseElems));
    }
  }
}
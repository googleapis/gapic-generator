package io.gapi.fx.snippet;

import io.gapi.fx.snippet.SnippetSet.EvalException;
import com.google.common.base.Strings;
import com.google.common.primitives.Primitives;

import java.lang.reflect.Array;
import java.util.Objects;

/**
 * Helpers for dealing with values.
 */
class Values {

  static final Object TRUE = true;
  static final Object FALSE = false;

  /**
   * Determines whether a value is 'true', where true is interpreted depending on the values
   * type.
   */
  @SuppressWarnings("rawtypes")
  static boolean isTrue(Object v1) {
    if (v1 instanceof Number) {
      // For numbers, compare with  the default value (zero), which we obtain by
      // creating an array.
      return !Array.get(Array.newInstance(Primitives.unwrap(v1.getClass()), 1), 0).equals(v1);
    }
    if (v1 instanceof Boolean) {
      return (Boolean) v1;
    }
    if (v1 instanceof Doc) {
      return !((Doc) v1).isWhitespace();
    }
    if (v1 instanceof String) {
      return !Strings.isNullOrEmpty((String) v1);
    }
    if (v1 instanceof Iterable) {
      return ((Iterable) v1).iterator().hasNext();
    }
    return false;
  }

  /**
   * Determines equality between two values, converting docs to strings as needed.
   */
  static boolean equal(Object v1, Object v2) {
    v1 = maybeConvertToString(v1);
    v2 = maybeConvertToString(v2);
    return Objects.equals(v1,  v2);
  }

  /**
   * Compares whether one value is less then the other. Both values must have the same
   * type (after converting doc to string) and implement Comparable to succeed.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  static boolean less(Object v1, Object v2) {
    v1 = maybeConvertToString(v1);
    v2 = maybeConvertToString(v2);
    if (v1.getClass() != v2.getClass()) {
      // Can't compare values of different type.
      return false;
    }
    if (v1 instanceof Comparable) {
      return ((Comparable) v1).compareTo(v2) < 0;
    }
    return false;
  }

  /**
   * Compares whether one value is less or equal than the other.
   */
  static boolean lessEqual(Object v1, Object v2) {
    v1 = maybeConvertToString(v1);
    v2 = maybeConvertToString(v2);
    if (Objects.equals(v1,  v2)) {
      return true;
    }
    return less(v1, v2);
  }

  /**
   * Converts the given value to the given type if possible. Throws an evaluation exception if not.
   *
   * @throws EvalException
   */
  static Object convert(Location location, Class<?> type, Object value) {
    ensureNotNull(location, value);
    if (type == Object.class || type.isInstance(value)) {
      return value;
    }
    String stringValue;
    if (value instanceof Doc) {
      stringValue = ((Doc) value).prettyPrint(Integer.MAX_VALUE);
    } else {
      stringValue = value.toString();
    }
    if (type == String.class) {
      return stringValue;
    }
    if (type == Doc.class) {
      return Doc.text(stringValue);
    }
    if (type.isPrimitive()) {
      type = Primitives.wrap(type);
    }
    if (type == Integer.class) {
      try {
        return Integer.parseInt(stringValue);
      } catch (NumberFormatException e) {
        throw new EvalException(location,
            "Conversion to int32 failed for: %s", stringValue);
      }
    }
    if (type == Long.class) {
      try {
        return Long.parseLong(stringValue);
      } catch (NumberFormatException e) {
        throw new EvalException(location,
            "Conversion to int64 failed for: %s", stringValue);
      }
    }
    if (type == Boolean.class) {
      return Boolean.parseBoolean(stringValue);
    }
    if (type.isEnum()) {
      @SuppressWarnings({"unchecked", "rawtypes"})
      Class enumType = type;
      @SuppressWarnings("unchecked")
      Object result = Enum.valueOf(enumType, stringValue);
      return result;
    }
    throw new EvalException(location,
        "Do not know how to make a '%s' from: %s", type.getSimpleName(), stringValue);
  }

  /**
   * Ensure the given value is not null and return it.
   */
  static Object ensureNotNull(Location location, Object value) {
    if (value == null) {
      throw new EvalException(location, "Snippet runtime does not support null values.");
    }
    return value;
  }

  /**
   * If the value is a doc or an enum, convert it to a string. Use maximal margin width for this.
   */
  private static Object maybeConvertToString(Object value) {
    if (value instanceof Doc) {
      return ((Doc) value).prettyPrint(Integer.MAX_VALUE);
    }
    if (value instanceof Enum) {
      return value.toString();
    }
    return value;
  }

  /**
   * Converts value to {@link Doc}. This always succeeds.
   */
  static Doc convertToDoc(Object value) {
    return (Doc) convert(Location.UNUSED, Doc.class, value);
  }

  /**
   * Convert the given number of objects into the specified types.
   *
   * @throws EvalException
   */
  static Object[] convertArgs(Location location,
      Class<?>[] types, Iterable<Object> values) {
    Object[] result = new Object[types.length];
    int i = 0;
    for (Object value : values) {
      result[i] = convert(location, types[i], value);
      i++;
    }
    return result;
  }
}

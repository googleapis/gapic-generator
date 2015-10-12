package io.gapi.fx.yaml;

import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;

import java.math.BigInteger;
import java.util.regex.Pattern;

/**
 * Utility class that parses values for proto field.
 */
class ProtoFieldValueParser {

  private static final Pattern DOUBLE_INFINITY = Pattern.compile(
      "-?inf(inity)?",
      Pattern.CASE_INSENSITIVE);
    private static final Pattern FLOAT_INFINITY = Pattern.compile(
      "-?inf(inity)?f?",
      Pattern.CASE_INSENSITIVE);
    private static final Pattern FLOAT_NAN = Pattern.compile(
      "nanf?",
      Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER = Pattern.compile("[+-]?[0-9]*");

  /**
   * Parse given text for field and return object of the field value type.
   * Throws {@link UnsupportedTypeException} if the field value type is not supported, and
   * {@link ParseException} if parsing failed.
   */
  static Object parseFieldFromString(FieldDescriptor field, String text) {
    try {
      switch(field.getType()) {
        case STRING:
          return text;
        case INT32:
        case SINT32:
        case SFIXED32:
          return parseInt32(text);
        case INT64:
        case SINT64:
        case SFIXED64:
          return parseInt64(text);
        case UINT32:
        case FIXED32:
          return parseUInt32(text);
        case UINT64:
        case FIXED64:
          return parseUInt64(text);
        case FLOAT:
          return parseFloat(text);
        case DOUBLE:
          return parseDouble(text);
        case BOOL:
          return parseBoolean(text);
        case ENUM:
          return parseEnum(field.getEnumType(), text);
        default:
          throw new UnsupportedTypeException(String.format(
              "Unsupported field type '%s'.", field.getType()));
      }
    } catch (ParseException e) {
      throw new ParseException(String.format(
          "Failed to parse text '%s' for proto field '%s' of type '%s' for reason: %s",
          text, field.getFullName(), field.getType(), e.getMessage()));
    }
  }

  /**
   * Parse Enum value {@link EnumValueDescriptor} associated with given {@link EnumDescriptor} from
   * given text if found. Otherwise, throw a {@link ParseException}.
   *
   * <p>The text could be either enum value name or enum value number.
   */
  static EnumValueDescriptor parseEnum(EnumDescriptor enumType, String text) {
    EnumValueDescriptor value = null;
    if (lookingAtNumber(text)) {
      int number = parseUInt32(text);
      value = enumType.findValueByNumber(number);
      if (value == null) {
        throw new ParseException(String.format(
            "Enum type '%s' has no value with number %d", enumType.getFullName(), number));
      }
    } else {
      value = enumType.findValueByName(text);
      if (value == null) {
        throw new ParseException(String.format(
            "Enum type '%s' has no value with name '%s'", enumType.getFullName(), text));
      }
    }
    return value;
  }

  private static boolean lookingAtNumber(String text) {
    return !text.isEmpty() && NUMBER.matcher(text).matches();
  }

  /**
   * Parse float value from the text if valid. Otherwise, throw a {@link ParseException}.
   */
  static float parseFloat(String text) {
    // We need to parse infinity and nan separately because
    // Float.parseFloat() does not accept "inf", "infinity", or "nan".
    if (FLOAT_INFINITY.matcher(text).matches()) {
      final boolean negative = text.startsWith("-");
      return negative ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
    }
    if (FLOAT_NAN.matcher(text).matches()) {
      return Float.NaN;
    }
    try {
      final float result = Float.parseFloat(text);
      return result;
    } catch (NumberFormatException e) {
      throw new ParseException(e);
    }
  }

  /**
   * Parse double value from the text if valid. Otherwise, throw a {@link ParseException}.
   */
  static double parseDouble(String text) {
    // We need to parse infinity and nan separately because
    // Double.parseDouble() does not accept "inf", "infinity", or "nan".
    if (DOUBLE_INFINITY.matcher(text).matches()) {
      final boolean negative = text.startsWith("-");
      return negative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
    }
    if (text.equalsIgnoreCase("nan")) {
      return Double.NaN;
    }
    try {
      final double result = Double.parseDouble(text);
      return result;
    } catch (NumberFormatException e) {
      throw new ParseException(e);
    }
  }

  /**
   * Parse boolean value from the text if valid. Otherwise, throw a {@link ParseException}.
   */
  static boolean parseBoolean(String text) {
    if (text.equals("true") ||
        text.equals("t") ||
        text.equals("1")) {
      return true;
    } else if (text.equals("false") ||
               text.equals("f") ||
               text.equals("0")) {
      return false;
    } else {
      throw new ParseException("Expected \"true\" or \"false\".");
    }
  }

  /**
   * Parse a 32-bit signed integer from the text.  Unlike the Java standard
   * {@code Integer.parseInt()}, this function recognizes the prefixes "0x"
   * and "0" to signify hexadecimal and octal numbers, respectively.
   */
  static int parseInt32(final String text) {
    try {
      return (int) parseInteger(text, true, false);
    } catch (NumberFormatException e) {
      throw new ParseException(e);
    }
  }

  /**
   * Parse a 32-bit unsigned integer from the text.  Unlike the Java standard
   * {@code Integer.parseInt()}, this function recognizes the prefixes "0x"
   * and "0" to signify hexadecimal and octal numbers, respectively.  The
   * result is coerced to a (signed) {@code int} when returned since Java has
   * no unsigned integer type.
   */
  static int parseUInt32(final String text) {
    try {
      return (int) parseInteger(text, false, false);
    } catch (NumberFormatException e) {
      throw new ParseException(e);
    }
  }

  /**
   * Parse a 64-bit signed integer from the text.  Unlike the Java standard
   * {@code Integer.parseInt()}, this function recognizes the prefixes "0x"
   * and "0" to signify hexadecimal and octal numbers, respectively.
   */
  static long parseInt64(final String text) {
    try {
      return parseInteger(text, true, true);
    } catch (NumberFormatException e) {
      throw new ParseException(e);
    }
  }

  /**
   * Parse a 64-bit unsigned integer from the text.  Unlike the Java standard
   * {@code Integer.parseInt()}, this function recognizes the prefixes "0x"
   * and "0" to signify hexadecimal and octal numbers, respectively.  The
   * result is coerced to a (signed) {@code long} when returned since Java has
   * no unsigned long type.
   */
  static long parseUInt64(final String text) {
    try {
      return parseInteger(text, false, true);
    } catch (NumberFormatException e) {
      throw new ParseException(e);
    }
  }

  private static long parseInteger(final String text, final boolean isSigned,
      final boolean isLong) {
    int pos = 0;

    boolean negative = false;
    if (text.startsWith("-", pos)) {
      if (!isSigned) {
        throw new NumberFormatException("Number must be positive: " + text);
      }
      ++pos;
      negative = true;
    }

    int radix = 10;
    if (text.startsWith("0x", pos)) {
      pos += 2;
      radix = 16;
    } else if (text.startsWith("0", pos)) {
      radix = 8;
    }

    final String numberText = text.substring(pos);

    long result = 0;
    if (numberText.length() < 16) {
      // Can safely assume no overflow.
      result = Long.parseLong(numberText, radix);
      if (negative) {
        result = -result;
      }

      // Check bounds.
      // No need to check for 64-bit numbers since they'd have to be 16 chars
      // or longer to overflow.
      if (!isLong) {
        if (isSigned) {
          if (result > Integer.MAX_VALUE || result < Integer.MIN_VALUE) {
            throw new NumberFormatException(
                "Number out of range for 32-bit signed integer: " + text);
          }
        } else {
          if (result >= (1L << 32) || result < 0) {
            throw new NumberFormatException(
                "Number out of range for 32-bit unsigned integer: " + text);
          }
        }
      }
    } else {
      BigInteger bigValue = new BigInteger(numberText, radix);
      if (negative) {
        bigValue = bigValue.negate();
      }

      // Check bounds.
      if (!isLong) {
        if (isSigned) {
          if (bigValue.bitLength() > 31) {
            throw new NumberFormatException(
                "Number out of range for 32-bit signed integer: " + text);
          }
        } else {
          if (bigValue.bitLength() > 32) {
            throw new NumberFormatException(
                "Number out of range for 32-bit unsigned integer: " + text);
          }
        }
      } else {
        if (isSigned) {
          if (bigValue.bitLength() > 63) {
            throw new NumberFormatException(
                "Number out of range for 64-bit signed integer: " + text);
          }
        } else {
          if (bigValue.bitLength() > 64) {
            throw new NumberFormatException(
                "Number out of range for 64-bit unsigned integer: " + text);
          }
        }
      }

      result = bigValue.longValue();
    }

    return result;
  }

  static class ParseException extends RuntimeException {
    ParseException(String message) {
      super(message);
    }

    ParseException(Throwable e) {
      super(e);
    }
  }

  static class UnsupportedTypeException extends RuntimeException {
    UnsupportedTypeException(String msg) {
      super(msg);
    }
  }
}

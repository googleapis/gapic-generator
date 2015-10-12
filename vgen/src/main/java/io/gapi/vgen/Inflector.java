package io.gapi.vgen;

/**
 * Utility class for manipulating words
 */
public class Inflector {

  // TODO (garrettjones) find an existing function that does this
  /**
   * Gives the singular form of an English word (only works for regular English plurals).
   */
  public static String singularize(String in) {
    if (in.endsWith("ves")) {
      return in.substring(0, in.length() - 3) + "f";

    } else if (in.endsWith("ies")) {
      return in.substring(0, in.length() - 3) + "y";

    } else if (in.charAt(in.length() - 1) == 's') {
      return in.substring(0, in.length() - 1);
    }

    return in;
  }
}

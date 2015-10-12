package io.gapi.fx.snippet;

import com.google.auto.value.AutoValue;

/**
 * Represents a source location.
 */
@AutoValue
public abstract class Location {

  static final Location UNUSED = create("unused", 1);
  static final Location TOP_LEVEL = create("top level", 1);

  /**
   * The name of the input. This is a ':'-separated list of file names,
   * where the last component represents the actual input, and the preceding
   * ones from where this input was included.
   */
  public abstract String inputName();
  public abstract int lineNo();

  static Location create(String inputName, int lineNo) {
    return new AutoValue_Location(inputName, lineNo);
  }

  /**
   * Returns the base input name, without the inclusion context.
   */
  public String baseInputName() {
    String name = inputName();
    int i = name.lastIndexOf(':');
    if (i >= 0) {
      return name.substring(i + 1);
    }
    return name;
  }
}

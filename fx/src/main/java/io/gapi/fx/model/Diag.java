package io.gapi.fx.model;

/**
 * An object representing a diagnostic message (error, warning, hint).
 */
public class Diag { // TODO: MIGRATION extends ValueType {

  /**
   * Creates an error diagnosis.
   */
  public static Diag error(Location location, String message, Object... args) {
    return new Diag(Kind.ERROR, location, String.format(message, args));
  }

  /**
   * Creates a warning diagnosis.
   */
  public static Diag warning(Location location, String message, Object... args) {
    return new Diag(Kind.WARNING, location, String.format(message, args));
  }

  /**
   * Represents diagnosis kind.
   */
  public enum Kind {
    WARNING,
    ERROR,
  }

  private final Kind kind;
  private final Location location;
  private final String message;

  private Diag(Kind kind, Location location, String message) {
    this.kind = kind;
    this.location = location;
    this.message = message;
  }

  /**
   * @return the kind
   */
  public Kind getKind() {
    return kind;
  }

  /**
   * @return the location
   */
  public Location getLocation() {
    return location;
  }

  /**
   * @return the message
   */
  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return String.format("%s: %s: %s",
        kind.toString(),
        location.getDisplayString(),
        message);
  }
}

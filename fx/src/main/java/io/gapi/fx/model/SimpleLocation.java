package io.gapi.fx.model;

import com.google.common.base.Preconditions;
import com.google.protobuf.DescriptorProtos;

/**
 * A simple location with a directly specified display string.
 */
public class SimpleLocation implements Location {

  public static final Location TOPLEVEL = new SimpleLocation("toplevel");
  public static final Location UNKNOWN = new SimpleLocation("unknown location");

  /**
   * <p>Create new instance of {@link SimpleLocation} by converting from instance of
   * {@link com.google.protobuf.DescriptorProtos.SourceCodeInfo.Location} </p>
   *
   * <p>The Display String structure is "proto name:line number:column number". </p>
   *
   * @param location Instance {@link com.google.protobuf.DescriptorProtos.SourceCodeInfo.Location}
   * @param element ProtoElement associated with the given location.
   */
  public static Location convertFrom(final DescriptorProtos.SourceCodeInfo.Location location,
      final ProtoElement element) {
    if (location == null) {
      return UNKNOWN;
    }
    return new Location() {
      @Override
      public String getDisplayString() {
        return String.format("%s:%d:%d", element.getFile().getLocation().getDisplayString(),
            location.getSpan(0) + 1, location.getSpan(1) + 1);
      }
    };
  }

  private final String displayString;

  /**
   * Creates a simple location.
   */
  public SimpleLocation(String displayString) {
    super();
    this.displayString = Preconditions.checkNotNull(displayString);
  }

  @Override
  public String getDisplayString() {
    return displayString;
  }
}

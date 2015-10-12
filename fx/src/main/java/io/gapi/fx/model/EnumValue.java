package io.gapi.fx.model;

import io.gapi.fx.model.stages.Requires;
import io.gapi.fx.model.stages.Resolved;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;

/**
 * Represents an enum value.
 */
public class EnumValue extends ProtoElement {

  /**
   * Creates an enum value backed up by the given proto.
   */
  public static EnumValue create(EnumType parent, EnumValueDescriptorProto proto, String path) {
    return new EnumValue(parent, proto, path);
  }

  private final EnumValueDescriptorProto proto;

  private EnumValue(EnumType parent, EnumValueDescriptorProto proto, String path) {
    super(parent, proto.getName(), path);
    this.proto = proto;
  }

  @Override public String toString() {
    return "value " + getFullName();
  }

  //-------------------------------------------------------------------------
  // Syntax

  private int valueIndex = -1;

  /**
   * Returns the underlying proto representation.
   */
  public EnumValueDescriptorProto getProto() {
    return proto;
  }

  /**
   * Return the number of the enum value.
   */
  public int getNumber() {
    return proto.getNumber();
  }

  /**
   * Get the index position of this value in its parent.
   */
  public int getIndex() {
    if (valueIndex < 0) {
      valueIndex = ((EnumType) getParent()).getValues().indexOf(this);
    }
    return valueIndex;
  }

  //-------------------------------------------------------------------------
  // Attributes belonging to resolved stage

  @Requires(Resolved.class) private TypeRef type;

  /**
   * Gets the type.
   */
  @Requires(Resolved.class) public TypeRef getType() {
    return type;
  }

  /**
   * For setting the type.
   */
  public void setType(TypeRef type) {
    this.type = type;
  }

}

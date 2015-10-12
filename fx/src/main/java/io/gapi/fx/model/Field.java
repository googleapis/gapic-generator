package io.gapi.fx.model;

import com.google.common.base.CaseFormat;
import com.google.common.base.Predicate;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;

import io.gapi.fx.model.ExtensionPool.Extension;
import io.gapi.fx.model.TypeRef.Cardinality;
import io.gapi.fx.model.stages.Requires;
import io.gapi.fx.model.stages.Resolved;

import javax.annotation.Nullable;

/**
 * Represents a field declaration.
 */
public class Field extends ProtoElement {

  public static final int WIRETYPE_VARINT = 0;
  public static final int WIRETYPE_FIXED64 = 1;
  public static final int WIRETYPE_LENGTH_DELIMITED = 2;
  public static final int WIRETYPE_START_GROUP = 3;
  public static final int WIRETYPE_END_GROUP = 4;
  public static final int WIRETYPE_FIXED32 = 5;

  /**
   * Creates a field backed up by the given proto.
   */
  public static Field create(MessageType parent, FieldDescriptorProto proto, String path,
      @Nullable Oneof oneof) {
    return new Field(parent, proto, path, oneof, proto.getName());
  }

  /**
   * Creates a field that represents an extension.
   */
  public static Field createAsExtension(MessageType parent, Extension extension, String path,
      String name) {
    Field field = new Field(parent, extension.getProto(), path, null, name);
    field.getFile().addExtension(extension, field);
    return field;
  }

  private final FieldDescriptorProto proto;
  private Oneof oneof;


  private Field(MessageType parent, FieldDescriptorProto proto, String path, Oneof oneof,
      String name) {
    super(parent, name, path);
    this.proto = proto;
    this.oneof = oneof;
  }

  //-------------------------------------------------------------------------
  // Syntax

  /**
   * Returns the underlying proto representation.
   */
  public FieldDescriptorProto getProto() {
    return proto;
  }

  /**
   * Returns true if field is oneof-scoped.
   */
  public boolean oneofScoped() {
    return oneof != null;
  }

  /**
   * Returns a oneof associated with this field, or null if none.
   */
  @Nullable public Oneof getOneof() {
    return oneof;
  }

  /**
   * For setting the oneof.
   */
  public void setOneof(@Nullable Oneof oneof) {
    this.oneof = oneof;
  }

  /**
   * Returns true if the field is optional.
   */
  public boolean isOptional() {
    return proto.getLabel() == FieldDescriptorProto.Label.LABEL_OPTIONAL;
  }

  /**
   * Returns true if the field is repeated.
   */
  public boolean isRepeated() {
    return proto.getLabel() == FieldDescriptorProto.Label.LABEL_REPEATED;
  }

  /**
   * Returns true if the field is required (proto2).
   */
  public boolean isRequired() {
    return proto.getLabel() == FieldDescriptorProto.Label.LABEL_REQUIRED;
  }

  /**
   * Returns the field number.
   */
  public int getNumber() {
    return proto.getNumber();
  }

  /**
   * Returns true if the field is packed.
   */
  public boolean isPacked() {
    return proto.getOptions().getPacked();
  }

  /**
   * Returns the JSON name of the field.
   */
  public String getJsonName() {
    // TODO(wgg): use upcoming proto descriptor option once present
    return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, getSimpleName());
  }

  /**
   * Returns the wired type encoding for the field.
   */
  public int getWireType() {
    if (proto.getOptions().getPacked()) {
      // Special case of packed primitives.
      return WIRETYPE_LENGTH_DELIMITED;
    }
    switch (proto.getType()) {
      case TYPE_BOOL:
      case TYPE_INT32:
      case TYPE_INT64:
      case TYPE_UINT32:
      case TYPE_UINT64:
      case TYPE_SINT32:
      case TYPE_SINT64:
      case TYPE_ENUM:
        return WIRETYPE_VARINT;
      case TYPE_FIXED64:
      case TYPE_SFIXED64:
      case TYPE_DOUBLE:
        return WIRETYPE_FIXED64;
      case TYPE_FIXED32:
      case TYPE_SFIXED32:
        return WIRETYPE_FIXED32;
      default:
        return WIRETYPE_LENGTH_DELIMITED;
    }
  }

  /**
   * Returns the field tag.
   */
  public int getTag() {
    return getNumber() << 3 | getWireType();
  }

  /**
   * Returns the default value of the field, as specified in the descriptor.proto,
   * or null if none is defined.
   */
  @Nullable public String getDefaultValue() {
    return proto.hasDefaultValue() ? proto.getDefaultValue() : null;
  }

  //-------------------------------------------------------------------------
  // Attributes belonging to resolved stage

  @Requires(Resolved.class) private TypeRef type;

  /**
   * Returns the resolved type
   */
  @Requires(Resolved.class) public TypeRef getType() {
    return type;
  }

  /**
   * For setting the resolved type.
   */
  public void setType(TypeRef type) {
    this.type = type;
  }

  //-------------------------------------------------------------------------
  // Predicates

  /**
   * Determines whether field has message type.
   */
  public static final Predicate<Field> HAS_MESSAGE_TYPE = new Predicate<Field>() {
    @Override public boolean apply(Field field) {
      return field.getType().getKind() == FieldDescriptorProto.Type.TYPE_MESSAGE;
    }
  };

  /**
   * Determines whether field is cyclic.
   */
  public static final Predicate<Field> IS_CYCLIC = new Predicate<Field>() {
    @Override public boolean apply(Field field) {
      return field.getType().isCyclic();
    }
  };

  /**
   * Determines whether field is repeated.
   */
  public static final Predicate<Field> IS_REPEATED = new Predicate<Field>() {
    @Override public boolean apply(Field field) {
      return field.getType().getCardinality() == Cardinality.REPEATED;
    }
  };

  /**
   * Determines whether field is part of a oneof.
   */
  public static final Predicate<Field> IS_ONEOF_SCOPED = new Predicate<Field>() {
    @Override public boolean apply(Field field) {
      return field.getOneof() != null;
    }
  };

  /**
   * Determines whether field is a map.
   */
  public static final Predicate<Field> IS_MAP = new Predicate<Field>() {
    @Override public boolean apply(Field field) {
      return field.getType().isMap();
    }
  };

  //-------------------------------------------------------------------------
  // String conversion

  @Override
  public String toString() {
    return String.format("%s %s", getType(), getFullName());
  }
}

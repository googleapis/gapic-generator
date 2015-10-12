package io.gapi.fx.model;

import io.gapi.fx.model.stages.Requires;
import io.gapi.fx.model.stages.Resolved;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * Represents a reference to a type.
 */
@Requires(Resolved.class)
public class TypeRef {

  /**
   * Represents cardinality of a type.
   */
  public enum Cardinality {
    OPTIONAL,
    REQUIRED,
    REPEATED
  }

  /**
   * Represents an enumeration of well-known types.
   */
  public enum WellKnownType {

    // Marker for indicating no well-known type.
    NONE(false, false, true, true),

    // Wrappers
    DOUBLE(true, true, true, false),
    FLOAT(true, true, true, false),
    INT64(true, true, true, false),
    UINT64(true, true, true, false),
    INT32(true, true, true, false),
    UINT32(true, true, true, false),
    BOOL(true, true, true, false),
    STRING(true, true, true, false),
    BYTES(true, true, true, false),

    // Time
    TIMESTAMP(true, true, false, false),
    DURATION(true, true, false, false),

    // Other
    STRUCT(false, false, true, true),
    VALUE(false, false, true, false),
    LIST_VALUE(false, false, true, false),
    ANY(false, false, true, true),
    MEDIA(false, false, false, false),
    FIELD_MASK(true, true, false, false);

    private final boolean allowedAsPathParameter;
    private final boolean allowedAsHttpParameter;
    private final boolean allowedAsHttpRequestResponse;
    private final boolean allowedAsRequestResponseInCodeGen;

    private WellKnownType(boolean allowedAsPathParameter, boolean allowedAsHttpParameter,
        boolean allowedAsHttpRequestResponse, boolean allowedAsRequestResponseInCodeGen) {
      this.allowedAsPathParameter = allowedAsPathParameter;
      this.allowedAsHttpParameter = allowedAsHttpParameter;
      this.allowedAsHttpRequestResponse = allowedAsHttpRequestResponse;
      this.allowedAsRequestResponseInCodeGen = allowedAsRequestResponseInCodeGen;
    }

    /**
     * Checks whether the WKT can appear in HTTP template path position.
     */
    public boolean allowedAsPathParameter() {
      return allowedAsPathParameter;
    }

    /**
     * Checks whether the WKT can appear in HTTP parameter position.
     */
    public boolean allowedAsHttpParameter() {
      return allowedAsHttpParameter;
    }

    /**
     * Checks whether the WKT can appear as HTTP request/response.
     */
    public boolean allowedAsHttpRequestResponse() {
      return allowedAsHttpRequestResponse;
    }

    /**
     * Checks whether the WKT is allowed as request/response by apiary based codegen.
     */
    public boolean allowedAsRequestResponseInCodeGen() {
      return allowedAsRequestResponseInCodeGen;
    }
  }

  private static final BiMap<String, Type> PRIMITIVE_TYPE_MAP = ImmutableBiMap
      .<String, Type>builder()
      .put("double", Type.TYPE_DOUBLE)
      .put("float", Type.TYPE_FLOAT)
      .put("int32", Type.TYPE_INT32)
      .put("int64", Type.TYPE_INT64)
      .put("uint32", Type.TYPE_UINT32)
      .put("uint64", Type.TYPE_UINT64)
      .put("sint32", Type.TYPE_SINT32)
      .put("sint64", Type.TYPE_SINT64)
      .put("fixed32", Type.TYPE_FIXED32)
      .put("fixed64", Type.TYPE_FIXED64)
      .put("sfixed32", Type.TYPE_SFIXED32)
      .put("sfixed64", Type.TYPE_SFIXED64)
      .put("bool", Type.TYPE_BOOL)
      .put("string", Type.TYPE_STRING)
      .put("bytes", Type.TYPE_BYTES)
      .build();

  private static final Map<String, WellKnownType> WELL_KNOWN_TYPE_MAP = ImmutableMap
      .<String, WellKnownType>builder()

      .put("google.protobuf.DoubleValue", WellKnownType.DOUBLE)
      .put("google.protobuf.FloatValue", WellKnownType.FLOAT)
      .put("google.protobuf.Int64Value", WellKnownType.INT64)
      .put("google.protobuf.UInt64Value", WellKnownType.UINT64)
      .put("google.protobuf.Int32Value", WellKnownType.INT32)
      .put("google.protobuf.UInt32Value", WellKnownType.UINT32)
      .put("google.protobuf.BoolValue", WellKnownType.BOOL)
      .put("google.protobuf.StringValue", WellKnownType.STRING)
      .put("google.protobuf.BytesValue", WellKnownType.BYTES)
      .put("google.protobuf.Timestamp", WellKnownType.TIMESTAMP)
      .put("google.protobuf.Duration", WellKnownType.DURATION)
      .put("google.protobuf.Struct", WellKnownType.STRUCT)
      .put("google.protobuf.Value", WellKnownType.VALUE)
      .put("google.protobuf.ListValue", WellKnownType.LIST_VALUE)
      .put("google.protobuf.Any", WellKnownType.ANY)
      .put("google.protobuf.Media", WellKnownType.MEDIA)
      .put("google.protobuf.FieldMask", WellKnownType.FIELD_MASK)
      .build();

  private static final Interner<TypeRef> interner = Interners.newWeakInterner();

  /**
   * Creates a reference to a primitive type, with default cardinality optional.
   */
  public static TypeRef of(Type primitiveType) {
    Preconditions.checkArgument(
        primitiveType != Type.TYPE_MESSAGE && primitiveType != Type.TYPE_ENUM);
    return interner.intern(new TypeRef(primitiveType, Cardinality.OPTIONAL, null, null));
  }

  /**
   * Creates a reference to a message type, with default cardinality optional.
   */
  public static TypeRef of(MessageType messageType) {
    return interner.intern(new TypeRef(Type.TYPE_MESSAGE, Cardinality.OPTIONAL, messageType, null));
  }

  /**
   * Creates a reference to an enum type, with default cardinality optional.
   */
  public static TypeRef of(EnumType enumType) {
    return interner.intern(new TypeRef(Type.TYPE_ENUM, Cardinality.OPTIONAL, null, enumType));
  }

  /**
   * Creates a reference to a primitive type based on its name in the protocol buffer language,
   * with default cardinality optional.
   */
  @Nullable
  public static TypeRef fromPrimitiveName(String name) {
    Type kind = PRIMITIVE_TYPE_MAP.get(name);
    return kind == null ? null : of(kind);
  }

  private final Type kind;
  private final Cardinality card;
  private final MessageType messageType;
  private final EnumType enumType;

  private TypeRef(Type protoType, Cardinality card,
      @Nullable MessageType messageType, @Nullable EnumType enumType) {
    this.kind = Preconditions.checkNotNull(protoType);
    this.card = Preconditions.checkNotNull(card);
    this.messageType = messageType;
    this.enumType = enumType;
  }

  /**
   * Returns the type kind.
   */
  public Type getKind() {
    return kind;
  }

  /**
   * Returns the type cardinality.
   */
  public Cardinality getCardinality() {
    return card;
  }

  /**
   * Returns true of the field is repeated.
   */
  public boolean isRepeated() {
    return card == Cardinality.REPEATED;
  }

  /**
   * Return trues if this type represents a map.
   */
  public boolean isMap() {
    return card == Cardinality.REPEATED && kind == Type.TYPE_MESSAGE
        && messageType.getProto().getOptions().getMapEntry();
  }

  /**
   * Returns the field for the key of a map type. Error if type is not a map.
   */
  public Field getMapKeyField() {
    Preconditions.checkArgument(isMap());
    return messageType.getFields().get(0);
  }

  /**
   * Returns the field for a value of a map type. Error if type is not a map.
   */
  public Field getMapValueField() {
    Preconditions.checkArgument(isMap());
    return messageType.getFields().get(1);
  }

  /**
   * Returns the well-known type kind, or NONE if its not one.
   */
  public WellKnownType getWellKnownType() {
    if (!isMessage()) {
      return WellKnownType.NONE;
    }
    WellKnownType wkt = WELL_KNOWN_TYPE_MAP.get(messageType.getFullName());
    return wkt == null ? WellKnownType.NONE : wkt;
  }

  /**
   * Makes the given type to have cardinality optional.
   */
  public TypeRef makeOptional() {
    return interner.intern(new TypeRef(kind, Cardinality.OPTIONAL, messageType, enumType));
  }

  /**
   * Makes the given type to have cardinality required.
   */
  public TypeRef makeRequired() {
    return interner.intern(new TypeRef(kind, Cardinality.REQUIRED, messageType, enumType));
  }

  /**
   * Makes the given type to have cardinality repeated.
   */
  public TypeRef makeRepeated() {
    return interner.intern(new TypeRef(kind, Cardinality.REPEATED, messageType, enumType));
  }

  /**
   * If this is a message type, returns the message, otherwise fails.
   */
  public MessageType getMessageType() {
    return Preconditions.checkNotNull(messageType);
  }

  /**
   * If this is a enum type, returns the enum, otherwise fails.
   */
  public EnumType getEnumType() {
    return Preconditions.checkNotNull(enumType);
  }

  /**
   * If this is a primitive type, returns the proto type name, otherwise fails.
   */
  public String getPrimitiveTypeName() {
    Preconditions.checkState(isPrimitive());
    return PRIMITIVE_TYPE_MAP.inverse().get(kind);
  }

  /**
   * Return true of this is a cyclic message type.
   */
  public boolean isCyclic() {
    return getKind() == Type.TYPE_MESSAGE && getMessageType().isCyclic();
  }

  /**
   * If this is a message or enum type, return its declaration location, otherwise
   * SimpleLocation.UNKNOWN.
   */
  public Location getLocation() {
    return messageType != null ? messageType.getLocation()
        : enumType != null ? enumType.getLocation() : SimpleLocation.UNKNOWN;
  }

  /**
   * Returns true of this is a message type.
   */
  public boolean isMessage() {
    return kind == Type.TYPE_MESSAGE;
  }

  /**
   * Returns true of this is an enum type.
   */
  public boolean isEnum() {
    return kind == Type.TYPE_ENUM;
  }

  /**
   * Returns true if this is a primitive type.
   */
  public boolean isPrimitive() {
    return !isMessage() && !isEnum();
  }

  @Override
  public String toString() {
    String result;
    switch (kind) {
      case TYPE_MESSAGE:
        result = getMessageType().getFullName();
        break;
      case TYPE_ENUM:
        result = getEnumType().getFullName();
        break;
      default:
        result = getPrimitiveTypeName();
        break;
    }
    if (card == Cardinality.REPEATED) {
      return "repeated " + result;
    }
    if (card == Cardinality.REQUIRED) {
      return "required " + result;
    }
    return result;
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, card, messageType, enumType);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    TypeRef other = (TypeRef) obj;
    return Objects.equals(kind, other.kind) && Objects.equals(card, other.card)
        && Objects.equals(messageType, other.messageType)
        && Objects.equals(enumType, other.enumType);
  }
}

package io.gapi.fx.model;

import io.gapi.fx.model.ExtensionPool.Extension;
import io.gapi.fx.model.stages.Requires;
import io.gapi.fx.model.stages.Resolved;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.OneofDescriptorProto;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Represents a message declaration.
 */
public class MessageType extends ProtoContainerElement {

  /**
   * Creates a message backed up by the given proto.
   */
  public static MessageType create(ProtoContainerElement parent, DescriptorProto proto,
      String path, ExtensionPool extensionPool) {
    return new MessageType(parent, proto, path, extensionPool);
  }

  private final DescriptorProto proto;
  private final ImmutableList<Field> fields;
  private ImmutableList<Oneof> oneofs;


  private MessageType(ProtoContainerElement parent, DescriptorProto proto, String path,
      ExtensionPool extensionPool) {
    super(parent, proto.getName(), path);
    this.proto = proto;
    buildChildren(proto.getNestedTypeList(),
        proto.getEnumTypeList(),
        path,
        DescriptorProto.NESTED_TYPE_FIELD_NUMBER,
        DescriptorProto.ENUM_TYPE_FIELD_NUMBER,
        extensionPool);

    // Build oneofs.
    ImmutableList.Builder<Oneof> oneofsBuilder = ImmutableList.builder();
    for (int i = 0; i < proto.getOneofDeclCount(); i++) {
      OneofDescriptorProto oneofProto = proto.getOneofDecl(i);
      String childPath = buildPath(path, DescriptorProto.ONEOF_DECL_FIELD_NUMBER, i);
      oneofsBuilder.add(Oneof.create(parent, oneofProto, childPath));
    }
    this.oneofs = oneofsBuilder.build();

    // Build fields.
    ImmutableList.Builder<Field> fieldsBuilder = ImmutableList.builder();
    List<FieldDescriptorProto> fieldProtos = proto.getFieldList();
    for (int i = 0; i < fieldProtos.size(); i++) {
      FieldDescriptorProto fieldProto = fieldProtos.get(i);
      Oneof associatedOneof = null;
      if (fieldProto.hasOneofIndex()) {
        int j = fieldProto.getOneofIndex();
        if (j >= 0 && j < oneofs.size()) {
          // Marked as error in resolver if index is out of range.
          associatedOneof = oneofs.get(j);
        }
      }
      String childPath = buildPath(path, DescriptorProto.FIELD_FIELD_NUMBER, i);
      Field field = Field.create(this, fieldProtos.get(i), childPath, associatedOneof);
      fieldsBuilder.add(field);
      if (associatedOneof != null) {
        associatedOneof.fields.add(field);
      }
    }
    for (Entry<String, Extension> entry :
        extensionPool.getSortedExtensionsByTypeName(getFullName())) {
      fieldsBuilder.add(Field.createAsExtension(
          this, entry.getValue(), entry.getValue().getPath(), entry.getKey()));
    }
    this.fields = fieldsBuilder.build();
  }

  @Override
  public String toString() {
    return "message " + getFullName();
  }

  //-------------------------------------------------------------------------
  // Syntax

  /**
   * Returns the underlying proto representation.
   */
  public DescriptorProto getProto() {
    return proto;
  }

  /**
   * Returns true if this type can be extended.
   */
  public boolean isExtensible() {
    return proto.getExtensionRangeCount() != 0;
  }

  /**
   * Returns true if the type is generated for Map entry.
   */
  public boolean isMapEntry() {
    return getProto().getOptions().getMapEntry();
  }

  /**
   * Returns all fields.
   */
  public ImmutableList<Field> getFields() {
    return fields;
  }

  /**
   * Returns the fields reachable with the active scoper.
   */
  public Iterable<Field> getReachableFields() {
    return getModel().reachable(fields);
  }

  /**
   * Returns all oneofs associated with this type.
   */
  public ImmutableList<Oneof> getOneofs() {
    return oneofs;
  }

  /**
   * For setting the oneofs list.
   */
  public void setOneofs(ImmutableList<Oneof> oneofs) {
    this.oneofs = oneofs;
  }

  //-------------------------------------------------------------------------
  // Attributes belonging to resolved stage

  @Requires(Resolved.class) private ImmutableMap<String, Field> fieldByName;
  private Boolean isCyclic;


  /**
   * Looks up the field by its name.
   */
  @Requires(Resolved.class)
  @Nullable
  public Field lookupField(String name) {
    return fieldByName.get(name);
  }

  /**
   * For setting the field-by-name map.
   */
  public void setFieldByNameMap(ImmutableMap<String, Field> fieldByName) {
    this.fieldByName = fieldByName;
  }

  /**
   * Returns true if this message or one of its sub-messages refers back to this message.
   */
  @Requires(Resolved.class)
  public boolean isCyclic() {
    if (isCyclic != null) {
      return isCyclic;
    }
    return isCyclic = checkCyclic(Sets.<MessageType>newHashSet(), this);
  }

  private boolean checkCyclic(Set<MessageType> visited, MessageType message) {
    if (!visited.add(this)) {
      return false;
    }
    for (Field field : fields) {
      TypeRef type = field.getType();
      if (type.getKind() == FieldDescriptorProto.Type.TYPE_MESSAGE) {
        if (message == type.getMessageType()) {
          return true;
        }
        if (type.getMessageType().checkCyclic(visited, message)) {
          // If there is a cycle to message via this, then this is also cyclic. That's the nature
          // of a cycle.
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns all fields which have message type.
   */
  public Iterable<Field> getMessageFields() {
    return FluentIterable.from(fields).filter(Field.HAS_MESSAGE_TYPE);
  }

  /**
   * Returns all fields which have non-message type.
   */
  public Iterable<Field> getNonMessageFields() {
    return FluentIterable.from(fields).filter(Predicates.not(Field.HAS_MESSAGE_TYPE));
  }

  /**
   * Returns all fields which have cyclic types.
   */
  public Iterable<Field> getCyclicFields() {
    return FluentIterable.from(fields).filter(Field.IS_CYCLIC);
  }

  /**
   * Returns all fields which have non-cyclic types.
   */
  public Iterable<Field> getNonCyclicFields() {
    return FluentIterable.from(fields).filter(Predicates.not(Field.IS_CYCLIC));
  }

  /**
   * Returns all fields which are repeated.
   */
  public Iterable<Field> getRepeatedFields() {
    return FluentIterable.from(fields).filter(Field.IS_REPEATED);
  }

  /**
   * Returns all fields which are not repeated.
   */
  public Iterable<Field> getNonRepeatedFields() {
    return FluentIterable.from(fields).filter(Predicates.not(Field.IS_REPEATED));
  }

  /**
   * Returns all fields which are oneof-scoped.
   */
  public Iterable<Field> getOneofScopedFields() {
    return FluentIterable.from(fields).filter(Field.IS_ONEOF_SCOPED);
  }

  /**
   * Returns all fields which are not oneof-scoped.
   */
  public Iterable<Field> getNonOneofScopedFields() {
    return FluentIterable.from(fields).filter(Predicates.not(Field.IS_ONEOF_SCOPED));
  }

  /**
   * Returns all fields which are maps.
   */
  public Iterable<Field> getMapFields() {
    return FluentIterable.from(fields).filter(Field.IS_MAP);
  }
}

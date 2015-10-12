package io.gapi.fx.model;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.protobuf.DescriptorProtos.OneofDescriptorProto;

import java.util.List;

/**
 * Represents an oneof declaration.
 */
public class Oneof extends ProtoElement {

  final List<Field> fields = Lists.newArrayList();

  private final OneofDescriptorProto proto;

  public static Oneof create(ProtoContainerElement parent, OneofDescriptorProto proto,
      String path) {
    return new Oneof(parent, proto, path);
  }

  private Oneof(ProtoContainerElement parent, OneofDescriptorProto proto, String path) {
    super(parent, proto.getName(), path);
    this.proto = proto;
  }

  /**
   * Gets the name of the oneof.
   */
  public String getName() {
    if (Strings.isNullOrEmpty(proto.getName())) {
      return "oneof";
    }
    return proto.getName();
  }

  /**
   * Gets all fields belonging to this oneof. This includes also unreachable fields.
   */
  public List<Field> getFields() {
    return fields;
  }

  /**
   * Gets all visible fields belonging to this oneof.
   */
  public Iterable<Field> getVisibleFields() {
    return getModel().reachable(fields);
  }

  @Override
  public String toString() {
    return getName();
  }
}

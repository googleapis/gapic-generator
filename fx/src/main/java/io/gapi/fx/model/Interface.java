package io.gapi.fx.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Api;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;

import io.gapi.fx.model.stages.Merged;
import io.gapi.fx.model.stages.Requires;
import io.gapi.fx.model.stages.Resolved;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Represents a interface declarations
 */
public class Interface extends ProtoElement {

  /**
   * Creates a interface backed up by the given proto.
   */
  public static Interface create(ProtoFile parent, ServiceDescriptorProto proto, String path) {
    return new Interface(parent, proto, path);
  }

  private final ServiceDescriptorProto proto;
  private final ImmutableList<Method> methods;

  private Interface(ProtoFile parent, ServiceDescriptorProto proto, String path) {
    super(parent, proto.getName(), path);
    this.proto = proto;

    // Build methods.
    ImmutableList.Builder<Method> methodsBuilder = ImmutableList.builder();
    List<MethodDescriptorProto> methodProtos = proto.getMethodList();
    for (int i = 0; i < methodProtos.size(); i++) {
      String childPath = buildPath(path, ServiceDescriptorProto.METHOD_FIELD_NUMBER, i);
      methodsBuilder.add(Method.create(this, methodProtos.get(i), childPath));
    }

    // TODO: MIGRATION
    /*
    List<StreamDescriptorProto> streamProtos = proto.getStreamList();
    for (int i = 0; i < streamProtos.size(); i++) {
      String childPath = buildPath(path, ServiceDescriptorProto.STREAM_FIELD_NUMBER, i);
      methodsBuilder.add(Method.create(this, streamProtos.get(i), childPath));
    }
    */
    methods = methodsBuilder.build();
  }

  @Override
  public String toString() {
    return "api " + getFullName();
  }

  //-------------------------------------------------------------------------
  // Syntax

  /**
   * Returns the underlying proto representation.
   */
  public ServiceDescriptorProto getProto() {
    return proto;
  }

  /**
   * Returns the methods.
   */
  public ImmutableList<Method> getMethods() {
    return methods;
  }

  /**
   * Returns the methods reachable with the active scoper.
   */
  public Iterable<Method> getReachableMethods() {
    return getModel().reachable(methods);
  }

  //-------------------------------------------------------------------------
  // Attributes belonging to resolved stage

  @Requires(Resolved.class) private ImmutableMap<String, Method> methodByName;

  /**
   * Looks up a method by its name.
   */
  @Requires(Resolved.class)
  @Nullable
  public Method lookupMethod(String name) {
    return methodByName.get(name);
  }

  /**
   * For setting the method-by-name map.
   */
  public void setMethodByNameMap(ImmutableMap<String, Method> methodByName) {
    this.methodByName = methodByName;
  }

  //-------------------------------------------------------------------------
  // Attributes belonging to the merged stage

  private Api config;

  /**
   * Returns the api config.
   */
  @Requires(Merged.class)
  public Api getConfig() {
    return config;
  }

  /**
   * Sets the api config.
   */
  public void setConfig(Api config) {
    this.config = config;
  }
}

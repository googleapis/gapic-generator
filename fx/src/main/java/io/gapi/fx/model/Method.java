package io.gapi.fx.model;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.MethodOptions;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Extension;
import com.google.protobuf.Message;

import io.gapi.fx.model.stages.Requires;
import io.gapi.fx.model.stages.Resolved;

import java.util.Map;

/**
 * Represents a method declaration. The REST accessors correspond to the primary REST binding.
 */
public class Method extends ProtoElement {

  /**
   * Creates a method with {@link MethodDescriptorProto}.
   */
  public static Method create(Interface parent, MethodDescriptorProto proto, String path) {
    return new Method(parent, proto, path);
  }

  /**
   * Creates a method with {@link StreamDescriptorProto}
   */
  // TODO: MIGRATION
  //public static Method create(Interface parent, StreamDescriptorProto proto, String path) {
  //  return new Method(parent, proto, path);
  //}

  private final MethodDescriptor descriptor;
  private final boolean requestStreaming;
  private final boolean responseStreaming;

  private Method(Interface parent, MethodDescriptorProto proto, String path) {
    super(parent, proto.getName(), path);
    this.descriptor = new MethodDescriptor(proto);
    this.requestStreaming = proto.getClientStreaming();
    this.responseStreaming = proto.getServerStreaming();
        //|| proto.getOptions().hasStreamType()
        //|| proto.getOptions().hasLegacyStreamType();
  }

  // TODO: MIGRATION
  //private Method(Interface parent, StreamDescriptorProto proto, String path) {
  //  super(parent, proto.getName(), path);
  //  this.descriptor = new MethodDescriptor(proto);
  //  this.requestStreaming = true;
  //  this.responseStreaming = true;
  //}

  @Override
  public String toString() {
    return "method " + getFullName();
  }

  //-------------------------------------------------------------------------
  // Syntax

  /**
   * Returns the {@link MethodDescriptor}.
   */
  public MethodDescriptor getDescriptor() {
    return descriptor;
  }

  /**
   * Returns true if request is streamed.
   */
  public boolean getRequestStreaming() {
    return requestStreaming;
  }

  /**
   * Returns true if response is streamed.
   */
  public boolean getResponseStreaming() {
    return responseStreaming;
  }


  //-------------------------------------------------------------------------
  // Attributes belonging to resolved stage

  @Requires(Resolved.class) private TypeRef inputType;
  @Requires(Resolved.class) private TypeRef outputType;

  /**
   * Returns the input type.
   */
  @Requires(Resolved.class)
  public TypeRef getInputType() {
    return inputType;
  }

  /**
   * For setting the input type.
   */
  public void setInputType(TypeRef inputType) {
    this.inputType = inputType;
  }

  /**
   * Returns the output type.
   */
  @Requires(Resolved.class)
  public TypeRef getOutputType() {
    return outputType;
  }

  /**
   * For setting the output type.
   */
  public void setOutputType(TypeRef outputType) {
    this.outputType = outputType;
  }

  /**
   * Helper for getting the methods input message.
   */
  // TODO(jcanizales): The majority of calls to getInputType and getOutputType just want the
  // MessageType; substitute them for clarity.
  public MessageType getInputMessage() {
    return inputType.getMessageType();
  }

  /**
   * Helper for getting the methods output message.
   */
  public MessageType getOutputMessage() {
    return outputType.getMessageType();
  }

  //-------------------------------------------------------------------------
  // Adapting for legacy stream types

  /**
   * An adapter type for {@link MethodDescriptorProto} and {@link StreamDescriptorProto}
   */
  public static class MethodDescriptor {

    private final String inputTypeName;
    private final String outputTypeName;
    //private final StreamDescriptorProto streamProto;
    private final MethodDescriptorProto methodProto;

    private final Map<FieldDescriptor, Object> options;


    //private MethodDescriptor(StreamDescriptorProto streamProto) {
    //  this.inputTypeName = streamProto.getClientMessageType();
    //  this.outputTypeName = streamProto.getServerMessageType();
    //  this.options = ImmutableMap.copyOf(streamProto.getOptions().getAllFields());
    //  this.streamProto = streamProto;
    //  this.methodProto = null;
    //}

    private MethodDescriptor(MethodDescriptorProto methodProto) {
      this.inputTypeName = methodProto.getInputType();
      this.outputTypeName = methodProto.getOutputType();
      this.options = ImmutableMap.copyOf(methodProto.getOptions().getAllFields());
      //this.streamProto = null;
      this.methodProto = methodProto;
    }

    /**
     * Returns the type name of the input message
     */
    public String getInputTypeName() {
      return inputTypeName;
    }

    /**
     * Returns the type name of the output message
     */
    public String getOutputTypeName() {
      return outputTypeName;
    }

    /**
     * Returns the method's options as a map
     */
    public Map<FieldDescriptor, Object> getOptions() {
      return options;
    }

    /**
     * Returns true if this method represents a stream.
     */
    //public boolean isStream() {
    //  return streamProto != null;
    //}

    /**
     * Returns a method-level annotation, or null if it is a stream.
     */
    public <T extends Message> T getMethodAnnotation(Extension<MethodOptions, T> extension) {
      return /*isStream() ? null :*/ methodProto.getOptions().getExtension(extension);
    }

    /**
     * Returns a stream-level annotation, or null if it is not a stream.
     */
    //public <T extends Message> T getStreamAnnotation(Extension<StreamOptions, T> extension) {
    //  return /*isStream() ? streamProto.getOptions().getExtension(extension) :*/ null;
    //}
  }
}

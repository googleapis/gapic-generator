package io.gapi.fx.model;


import com.google.common.collect.ImmutableList;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Base class of protocol elements which are containers for messages and enums (proto files and
 * messages).
 */
public abstract class ProtoContainerElement extends ProtoElement {

  private ImmutableList<MessageType> messages;
  private ImmutableList<EnumType> enums;

  /**
   * Creates the container element, given its parent and (simple) name, and a list of messages and
   * enum types. Any child class must call {@link #buildChildren} in the constructor to initialize
   * child elements. The initialization is done in the child so methods like {@link #getFullName}
   * work during initialization.
   */
  protected ProtoContainerElement(@Nullable ProtoElement parent, String name, String path) {
    super(parent, name, path);
  }

  /**
   * Returns the messages.
   */
  public ImmutableList<MessageType> getMessages() {
    return messages;
  }

  /**
   * Returns the enums.
   */
  public ImmutableList<EnumType> getEnums() {
    return enums;
  }

  protected void buildChildren(List<DescriptorProto> messageList,
      List<EnumDescriptorProto> enumList,
      String path,
      int messageTypeNumber,
      int enumTypeNumber,
      ExtensionPool extensionPool) {
    // Build messages.
    ImmutableList.Builder<MessageType> messagesBuilder = ImmutableList.builder();
    for (int i = 0; i < messageList.size(); i++) {
      String childPath = buildPath(path, messageTypeNumber, i);
      messagesBuilder.add(MessageType.create(this, messageList.get(i), childPath, extensionPool));
    }
    messages = messagesBuilder.build();

    // Build enums.
    ImmutableList.Builder<EnumType> enumsBuilder = ImmutableList.builder();
    for (int i = 0; i < enumList.size(); i++) {
      String childPath = buildPath(path, enumTypeNumber, i);
      enumsBuilder.add(EnumType.create(this, enumList.get(i), childPath));
    }
    enums = enumsBuilder.build();
  }
}

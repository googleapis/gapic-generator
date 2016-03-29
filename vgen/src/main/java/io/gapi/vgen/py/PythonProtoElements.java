package io.gapi.vgen.py;

import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.ProtoElement;

public class PythonProtoElements {
  /**
   * Return name of protobuf file for the given ProtoElement.
   */
  public static String getPbFileName(ProtoElement element) {
    // FileDescriptorProto.name returns file name, relative to root of the source tree. Return the
    // last segment of the file name without proto extension appended by "_pb2".
    String filename = element.getFile().getProto().getName().substring(
        element.getFile().getProto().getName().lastIndexOf("/") + 1);
    return filename.substring(0, filename.length() - ".proto".length()) + "_pb2";
  }

  /**
   * Returns whether the given MessageType has no fields
   */
  public static boolean isEmptyMessage(MessageType msgType) {
    return msgType.getFields().size() == 0;
  }

  /**
   * The dot-separated nested messages that contain an element in a Proto file.
   * Returns null if the element is top-level or a proto file itself.
   */
  public static String prefixInFile(ProtoElement elt) {
    ProtoElement parent = elt.getParent();
    // elt is a proto file itself, or elt is top-level
    if (parent == null || parent.getParent() == null) {
      return "";
    }
    String prefix = parent.getSimpleName();
    for (parent = parent.getParent(); parent.getParent() != null; parent = parent.getParent()) {
      prefix = parent.getSimpleName() + "." + prefix;
    }
    return prefix;
  }
}

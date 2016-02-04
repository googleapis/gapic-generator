package io.gapi.vgen.py;

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
}
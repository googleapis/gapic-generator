/* Copyright 2016 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.py;

import com.google.api.tools.framework.model.ProtoElement;

public class PythonProtoElements {
  /** Return name of protobuf file for the given ProtoElement. */
  public static String getPbFileName(ProtoElement element) {
    // FileDescriptorProto.name returns file name, relative to root of the source tree. Return the
    // last segment of the file name without proto extension appended by "_pb2".
    String filename =
        element
            .getFile()
            .getProto()
            .getName()
            .substring(element.getFile().getProto().getName().lastIndexOf("/") + 1);
    return filename.substring(0, filename.length() - ".proto".length()) + "_pb2";
  }

  /**
   * The dot-separated nested messages that contain an element in a Proto file. Returns null if the
   * element is top-level or a proto file itself.
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

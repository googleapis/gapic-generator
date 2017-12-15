/* Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen;

import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * A file-based view of model, consisting of a strategy for getting the protocol buffer files from
 * which the model was constructed.
 */
public class ProtoFileView implements InputElementView<ProtoFile> {

  /**
   * Gets the ProtoFile objects in which the fields of the reachable methods in the model are
   * defined.
   */
  @Override
  public Iterable<ProtoFile> getElementIterable(Model model) {
    Set<ProtoFile> files = new HashSet<>();
    for (Interface apiInterface : new InterfaceView().getElementIterable(model)) {
      for (Method method : apiInterface.getMethods()) {
        if (!method.isReachable()) {
          continue;
        }
        for (Field field : method.getInputType().getMessageType().getFields()) {
          files.addAll(protoFiles(field));
        }
        for (Field field : method.getOutputType().getMessageType().getFields()) {
          files.addAll(protoFiles(field));
        }
      }
    }
    return files;
  }

  private Set<ProtoFile> protoFiles(Field field) {
    Set<ProtoFile> fields = new HashSet<ProtoFile>();
    if (field.getType().getKind() != Type.TYPE_MESSAGE) {
      return fields;
    }
    MessageType messageType = field.getType().getMessageType();
    fields.add(messageType.getFile());
    for (Field f : messageType.getNonCyclicFields()) {
      fields.addAll(protoFiles(f));
    }
    return fields;
  }
}

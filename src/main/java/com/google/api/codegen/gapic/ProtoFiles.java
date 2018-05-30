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
package com.google.api.codegen.gapic;

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProtoTypeRef;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import java.util.Set;
import java.util.TreeSet;

/**
 * A file-based view of model, consisting of a strategy for getting the protocol buffer files from
 * which the model was constructed.
 */
public class ProtoFiles {

  private ProtoFiles() {}

  /**
   * Gets the ProtoFile objects in which the fields of the reachable methods in the model are
   * defined.
   */
  public static Iterable<ProtoFile> getProtoFiles(GapicProductConfig productConfig) {
    Set<ProtoFile> files = newFileSet();
    for (InterfaceConfig interfaceConfig : productConfig.getInterfaceConfigMap().values()) {
      for (MethodConfig methodConfig : interfaceConfig.getMethodConfigs()) {
        MethodModel method = methodConfig.getMethodModel();
        files.addAll(
            getFilesForMessage(
                ((ProtoTypeRef) method.getInputType()).getProtoType().getMessageType(), false));
        files.addAll(
            getFilesForMessage(
                ((ProtoTypeRef) method.getOutputType()).getProtoType().getMessageType(), false));
      }
    }
    return files;
  }

  private static Set<ProtoFile> getFilesForMessage(MessageType messageType, boolean messageOnly) {
    Set<ProtoFile> files = newFileSet();
    files.add(messageType.getFile());
    if (messageOnly) {
      return files;
    }

    for (Field field : messageType.getFields()) {
      TypeRef type = field.getType();
      if (type.isMessage()) {
        files.addAll(getFilesForMessage(type.getMessageType(), type.isCyclic()));
      }
    }
    return files;
  }

  private static Set<ProtoFile> newFileSet() {
    return new TreeSet<>((fileA, fileB) -> getPath(fileA).compareTo(getPath(fileB)));
  }

  private static String getPath(ProtoFile file) {
    return file.getFullName() + "." + file.getSimpleName();
  }
}

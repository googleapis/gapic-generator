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
package com.google.api.codegen.transformer;

import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.viewmodel.ImportTypeView;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Oneof;
import com.google.api.tools.framework.model.ProtoElement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

public class ImportTypeTransformer {

  public List<ImportTypeView> generateImports(Map<String, TypeAlias> imports) {
    List<ImportTypeView> generatedImports = new ArrayList<>();
    for (String key : imports.keySet()) {
      TypeAlias value = imports.get(key);
      generatedImports.add(
          ImportTypeView.newBuilder()
              .fullName(key)
              .nickname(value.getNickname())
              .type(value.getImportType())
              .build());
    }
    return generatedImports;
  }

  public List<ImportTypeView> generateServiceFileImports(SurfaceTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    Set<String> fullNames = new TreeSet<>();

    fullNames.add(namer.getProtoFileImportFromService(context.getInterface()));

    for (Method method : context.getSupportedMethods()) {
      Interface targetInterface = context.asMethodContext(method).getTargetInterface();
      fullNames.add(namer.getProtoFileImportFromService(targetInterface));
    }

    return createImportFilesFromNames(fullNames);
  }

  public List<ImportTypeView> generateProtoFileImports(SurfaceTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    Set<String> fullNames = new TreeSet<>();
    Queue<ProtoElement> elementQueue = new LinkedList<>();

    BfsHandler handler = new BfsHandler(elementQueue);

    elementQueue.add(context.getInterface());

    while (!elementQueue.isEmpty()) {
      ProtoElement element = elementQueue.remove();
      if (element instanceof Interface) {
        handler.extendQueue(((Interface) element).getMethods());
      } else if (element instanceof Method) {
        handler.extendQueueBySingleElement(((Method) element).getInputMessage());
      } else if (element instanceof MessageType) {
        handler.extendQueue(((MessageType) element).getFields());
      } else if (element instanceof Field) {
        if (((Field) element).getOneof() != null) {
          handler.extendQueueBySingleElement(((Field) element).getOneof());
        }
      } else if (element instanceof Oneof) {
        handler.extendQueue(((Oneof) element).getFields());
      }
      fullNames.add(namer.getProtoFileImportFromProtoElement(element));
    }
    return createImportFilesFromNames(fullNames);
  }

  private List<ImportTypeView> createImportFilesFromNames(Set<String> fullNames) {
    List<ImportTypeView> imports = new ArrayList<>();
    for (String name : fullNames) {
      ImportTypeView.Builder builder = ImportTypeView.newBuilder();
      builder.fullName(name);
      builder.nickname("");
      imports.add(builder.build());
    }

    return imports;
  }

  private class BfsHandler {
    private Set<String> seen;
    private Queue<ProtoElement> elementQueue;

    BfsHandler(Queue<ProtoElement> elementQueue) {
      seen = new HashSet<>();
      this.elementQueue = elementQueue;
    }

    void extendQueue(List<? extends ProtoElement> elements) {
      for (ProtoElement element : elements) {
        extendQueueBySingleElement(element);
      }
    }

    void extendQueueBySingleElement(ProtoElement element) {
      if (!seen.contains(element.getFullName())) {
        elementQueue.add(element);
        seen.add(element.getFullName());
      }
    }
  }
}

/* Copyright 2017 Google Inc
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

import com.google.api.codegen.viewmodel.GrpcElementDocView;
import com.google.api.codegen.viewmodel.GrpcEnumDocView;
import com.google.api.codegen.viewmodel.GrpcEnumValueDocView;
import com.google.api.codegen.viewmodel.GrpcMessageDocView;
import com.google.api.codegen.viewmodel.ParamDocView;
import com.google.api.codegen.viewmodel.SimpleParamDocView;
import com.google.api.tools.framework.model.EnumType;
import com.google.api.tools.framework.model.EnumValue;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.ProtoContainerElement;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class GrpcElementDocTransformer {
  public List<GrpcElementDocView> generateElementDocs(
      ModelTypeTable typeTable, SurfaceNamer namer, ProtoContainerElement containerElement) {
    ImmutableList.Builder<GrpcElementDocView> children = ImmutableList.builder();
    children.addAll(generateMessageDocs(typeTable, namer, containerElement));
    children.addAll(generateEnumDocs(typeTable, namer, containerElement));
    return children.build();
  }

  private List<GrpcElementDocView> generateMessageDocs(
      ModelTypeTable typeTable, SurfaceNamer namer, ProtoContainerElement containerElement) {
    ImmutableList.Builder<GrpcElementDocView> messageDocs = ImmutableList.builder();
    for (MessageType message : containerElement.getMessages()) {
      // Doesn't have to document map entries because a dictionary is used.
      if (message.isMapEntry()) {
        continue;
      }

      GrpcMessageDocView.Builder doc = GrpcMessageDocView.newBuilder();
      doc.name(typeTable.getNicknameFor(TypeRef.of(message)));
      doc.lines(namer.getDocLines(message));
      doc.properties(generateMessagePropertyDocs(typeTable, namer, message.getFields()));
      doc.elementDocs(generateElementDocs(typeTable, namer, message));
      messageDocs.add(doc.build());
    }
    return messageDocs.build();
  }

  private List<ParamDocView> generateMessagePropertyDocs(
      ModelTypeTable typeTable, SurfaceNamer namer, Iterable<Field> fields) {
    ImmutableList.Builder<ParamDocView> propertyDocs = ImmutableList.builder();
    for (Field field : fields) {
      SimpleParamDocView.Builder doc = SimpleParamDocView.newBuilder();
      doc.paramName(field.getSimpleName());
      doc.typeName(namer.getParamTypeName(typeTable, field.getType()));
      doc.lines(namer.getDocLines(field));
      propertyDocs.add(doc.build());
    }
    return propertyDocs.build();
  }

  private List<GrpcElementDocView> generateEnumDocs(
      ModelTypeTable typeTable, SurfaceNamer namer, ProtoContainerElement containerElement) {
    ImmutableList.Builder<GrpcElementDocView> enumDocs = ImmutableList.builder();
    for (EnumType enumElement : containerElement.getEnums()) {
      GrpcEnumDocView.Builder doc = GrpcEnumDocView.newBuilder();
      doc.name(typeTable.getNicknameFor(TypeRef.of(enumElement)));
      doc.lines(namer.getDocLines(enumElement));
      doc.values(generateEnumValueDocs(namer, enumElement));
      enumDocs.add(doc.build());
    }
    return enumDocs.build();
  }

  private List<GrpcEnumValueDocView> generateEnumValueDocs(
      SurfaceNamer namer, EnumType enumElement) {
    ImmutableList.Builder<GrpcEnumValueDocView> valueDocs = ImmutableList.builder();
    for (EnumValue value : enumElement.getValues()) {
      GrpcEnumValueDocView.Builder doc = GrpcEnumValueDocView.newBuilder();
      doc.name(value.getSimpleName());
      doc.number(value.getNumber());
      doc.lines(namer.getDocLines(value));
      valueDocs.add(doc.build());
    }
    return valueDocs.build();
  }
}

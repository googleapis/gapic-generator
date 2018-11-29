/* Copyright 2017 Google LLC
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
package com.google.api.codegen.transformer;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.gapic.GapicParser;
import com.google.api.codegen.viewmodel.GrpcElementDocView;
import com.google.api.codegen.viewmodel.GrpcEnumDocView;
import com.google.api.codegen.viewmodel.GrpcEnumValueDocView;
import com.google.api.codegen.viewmodel.GrpcMessageDocView;
import com.google.api.codegen.viewmodel.ParamDocView;
import com.google.api.codegen.viewmodel.SimpleParamDocView;
import com.google.api.tools.framework.model.EnumType;
import com.google.api.tools.framework.model.EnumValue;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.ProtoContainerElement;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Comparator;
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
      doc.name(namer.getMessageTypeName(typeTable, message));
      doc.fullName(typeTable.getFullNameFor(TypeRef.of(message)));
      doc.fileUrl(GapicParser.getFileUrl(message.getFile()));
      doc.lines(namer.getDocLines(GapicParser.getDocString(message)));
      doc.properties(
          generateMessagePropertyDocs(
              typeTable, namer, FieldConfig.toFieldTypeIterableFromField(message.getFields())));
      doc.elementDocs(generateElementDocs(typeTable, namer, message));
      doc.packageName(message.getFile().getFullName());
      messageDocs.add(doc.build());
    }
    return messageDocs.build();
  }

  private List<ParamDocView> generateMessagePropertyDocs(
      ModelTypeTable typeTable, SurfaceNamer namer, Iterable<FieldModel> fields) {
    ImmutableList.Builder<ParamDocView> propertyDocs = ImmutableList.builder();
    for (FieldModel field : fields) {
      SimpleParamDocView.Builder doc = SimpleParamDocView.newBuilder();
      doc.paramName(namer.getFieldKey(field));
      doc.typeName(namer.getMessagePropertyTypeName(typeTable, field));
      doc.lines(namer.getDocLines(field));
      propertyDocs.add(doc.build());
    }
    return propertyDocs.build();
  }

  /** Return a list of enums, sorted alphabetically by name. */
  public List<GrpcEnumDocView> generateEnumDocs(
      ModelTypeTable typeTable, SurfaceNamer namer, ProtoContainerElement containerElement) {
    ImmutableSortedSet.Builder<GrpcEnumDocView> enumDocs =
        ImmutableSortedSet.orderedBy(Comparator.comparing(GrpcEnumDocView::name));
    for (EnumType enumElement : containerElement.getEnums()) {
      if (!enumElement.isReachable()) {
        continue;
      }
      GrpcEnumDocView.Builder doc = GrpcEnumDocView.newBuilder();
      doc.name(namer.getEnumTypeName(typeTable, enumElement));
      doc.lines(namer.getDocLines(GapicParser.getDocString(enumElement)));
      doc.values(generateEnumValueDocs(namer, enumElement));
      doc.packageName(enumElement.getFile().getFullName());
      enumDocs.add(doc.build());
    }
    return enumDocs.build().asList();
  }

  private List<GrpcEnumValueDocView> generateEnumValueDocs(
      SurfaceNamer namer, EnumType enumElement) {
    ImmutableList.Builder<GrpcEnumValueDocView> valueDocs = ImmutableList.builder();
    for (EnumValue value : enumElement.getValues()) {
      GrpcEnumValueDocView.Builder doc = GrpcEnumValueDocView.newBuilder();
      doc.name(value.getSimpleName());
      doc.number(value.getNumber());
      doc.lines(namer.getDocLines(GapicParser.getDocString(value)));
      valueDocs.add(doc.build());
    }
    return valueDocs.build();
  }
}

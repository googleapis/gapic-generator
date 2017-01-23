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
package com.google.api.codegen.transformer.nodejs;

import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.ParamDocTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.ParamDocView;
import com.google.api.codegen.viewmodel.SimpleParamDocView;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;

public class NodeJSParamDocTransformer implements ParamDocTransformer {

  @Override
  public List<ParamDocView> generateParamDocs(MethodTransformerContext context) {
    ImmutableList.Builder<ParamDocView> docs = ImmutableList.builder();
    if (!context.getMethod().getRequestStreaming()) {
      docs.add(generateRequestObjectParamDoc(context));
      docs.addAll(
          generateMethodParamDocs(context, context.getMethodConfig().getRequiredFields(), false));
      docs.addAll(
          generateMethodParamDocs(context, context.getMethodConfig().getOptionalFields(), true));
    }
    docs.add(generateOptionsParamDoc());
    return docs.build();
  }

  private List<ParamDocView> generateMethodParamDocs(
      MethodTransformerContext context, Iterable<Field> fields, boolean isOptional) {
    SurfaceNamer namer = context.getNamer();
    MethodConfig methodConfig = context.getMethodConfig();
    ImmutableList.Builder<ParamDocView> docs = ImmutableList.builder();
    for (Field field : fields) {
      if (isRequestTokenParam(methodConfig, field)) {
        continue;
      }

      SimpleParamDocView.Builder paramDoc = SimpleParamDocView.newBuilder();
      paramDoc.paramName("request." + namer.getVariableName(field));

      String typeName = namer.getParamTypeName(context.getTypeTable(), field.getType());
      paramDoc.typeName(typeName + (isOptional ? "=" : ""));

      ImmutableList.Builder<String> docLines = ImmutableList.builder();
      if (isPageSizeParam(methodConfig, field)) {
        docLines.add(
            "The maximum number of resources contained in the underlying API",
            "response. If page streaming is performed per-resource, this",
            "parameter does not affect the return value. If page streaming is",
            "performed per-page, this determines the maximum number of",
            "resources in a page.");
      } else {
        docLines.addAll(namer.getDocLines(field));
      }

      if (field.getType().isMessage() && !field.getType().isMap()) {
        docLines.add(
            "This object should have the same structure as "
                + linkForMessage(context.getTypeTable(), field.getType()));
      } else if (field.getType().isEnum()) {
        docLines.add(
            "The number should be among the values of "
                + linkForMessage(context.getTypeTable(), field.getType()));
      }

      paramDoc.lines(docLines.build());
      docs.add(paramDoc.build());
    }
    return docs.build();
  }

  private String linkForMessage(ModelTypeTable typeTable, TypeRef type) {
    if (inExternalFile(type)) {
      String fullName = typeTable.getFullNameFor(type);
      return String.format("[%s]{@link external:\"%s\"}", fullName, fullName);
    } else {
      String simpleName = typeTable.getNicknameFor(type);
      return String.format("[%s]{@link %s}", simpleName, simpleName);
    }
  }

  private boolean inExternalFile(TypeRef type) {
    ProtoElement element = null;
    if (type.isMessage()) {
      element = type.getMessageType();
    } else if (type.isEnum()) {
      element = type.getEnumType();
    } else {
      throw new IllegalStateException("Unexpected type: " + type);
    }

    return isExternalFilename(element.getFile().getSimpleName());
  }

  private boolean isExternalFilename(String filename) {
    for (String commonPath : COMMON_PROTO_PATHS) {
      if (filename.startsWith(commonPath)) {
        return true;
      }
    }
    return false;
  }

  private boolean isPageSizeParam(MethodConfig methodConfig, Field field) {
    return methodConfig.isPageStreaming()
        && methodConfig.getPageStreaming().hasPageSizeField()
        && field.equals(methodConfig.getPageStreaming().getPageSizeField());
  }

  private boolean isRequestTokenParam(MethodConfig methodConfig, Field field) {
    return methodConfig.isPageStreaming()
        && field.equals(methodConfig.getPageStreaming().getRequestTokenField());
  }

  private ParamDocView generateRequestObjectParamDoc(MethodTransformerContext context) {
    MethodConfig methodConfig = context.getMethodConfig();
    SimpleParamDocView.Builder paramDoc = SimpleParamDocView.newBuilder();
    paramDoc.paramName(context.getNamer().localVarName(Name.from("request")));
    paramDoc.lines(ImmutableList.of("The request object that will be sent."));

    String typeName = "Object";
    if (!methodConfig.getRequiredFieldConfigs().iterator().hasNext()
        && !methodConfig.getOptionalFieldConfigs().iterator().hasNext()) {
      typeName += "=";
    }

    paramDoc.typeName(typeName);
    return paramDoc.build();
  }

  private ParamDocView generateOptionsParamDoc() {
    SimpleParamDocView.Builder paramDoc = SimpleParamDocView.newBuilder();
    paramDoc.paramName("options");
    paramDoc.typeName("Object=");
    paramDoc.lines(
        ImmutableList.of(
            "Optional parameters. You can override the default settings for this call, e.g, timeout,",
            "retries, paginations, etc. See [gax.CallOptions]{@link "
                + "https://googleapis.github.io/gax-nodejs/global.html#CallOptions} for the details."));
    return paramDoc.build();
  }

  private static final ImmutableSet<String> COMMON_PROTO_PATHS =
      ImmutableSet.of(
          "google/api",
          "google/bytestream",
          "google/logging/type",
          "google/longrunning",
          "google/protobuf",
          "google/rpc",
          "google/type");
}

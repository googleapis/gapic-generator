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
package com.google.api.codegen.transformer.py;

import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.transformer.ApiMethodParamTransformer;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.py.PythonDocstringUtil;
import com.google.api.codegen.viewmodel.DynamicLangDefaultableParamView;
import com.google.api.codegen.viewmodel.ParamDocView;
import com.google.api.codegen.viewmodel.SimpleParamDocView;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class PythonApiMethodParamTransformer implements ApiMethodParamTransformer {
  @Override
  public List<DynamicLangDefaultableParamView> generateMethodParams(GapicMethodContext context) {
    ImmutableList.Builder<DynamicLangDefaultableParamView> methodParams = ImmutableList.builder();
    methodParams.add(
        DynamicLangDefaultableParamView.newBuilder().name("self").defaultValue("").build());
    if (context.getMethod().getRequestStreaming()) {
      methodParams.add(
          DynamicLangDefaultableParamView.newBuilder()
              .name(context.getNamer().getRequestVariableName(context.getMethod()))
              .defaultValue("")
              .build());
    } else {
      for (Field field : context.getMethodConfig().getRequiredFields()) {
        DynamicLangDefaultableParamView.Builder param =
            DynamicLangDefaultableParamView.newBuilder();
        param.name(context.getNamer().getVariableName(field));
        param.defaultValue("");
        methodParams.add(param.build());
      }
      for (Field field : context.getMethodConfig().getOptionalFields()) {
        if (isRequestTokenParam(context.getMethodConfig(), field)) {
          continue;
        }
        TypeRef type = field.getType();
        DynamicLangDefaultableParamView.Builder param =
            DynamicLangDefaultableParamView.newBuilder();
        param.name(context.getNamer().getVariableName(field));
        if (type.isRepeated() || type.isMessage() || type.isEnum() || field.getOneof() != null) {
          param.defaultValue("None");
        } else {
          param.defaultValue(context.getTypeTable().getSnippetZeroValueAndSaveNicknameFor(type));
        }
        methodParams.add(param.build());
      }
    }
    methodParams.add(
        DynamicLangDefaultableParamView.newBuilder().name("options").defaultValue("None").build());
    return methodParams.build();
  }

  @Override
  public List<ParamDocView> generateParamDocs(GapicMethodContext context) {
    ImmutableList.Builder<ParamDocView> docs = ImmutableList.builder();
    if (context.getMethod().getRequestStreaming()) {
      docs.add(generateRequestStreamingParamDoc(context));
    } else {
      docs.addAll(generateMethodParamDocs(context, context.getMethodConfig().getRequiredFields()));
      docs.addAll(generateMethodParamDocs(context, context.getMethodConfig().getOptionalFields()));
    }
    docs.add(generateOptionsParamDoc());
    return docs.build();
  }

  private ParamDocView generateRequestStreamingParamDoc(GapicMethodContext context) {
    SimpleParamDocView.Builder paramDoc = SimpleParamDocView.newBuilder();
    paramDoc.paramName(context.getNamer().localVarName(Name.from("requests")));
    TypeRef inputType = context.getMethod().getInputType();
    String requestTypeName = context.getTypeTable().getFullNameFor(inputType);
    paramDoc.lines(
        ImmutableList.of(
            "The input objects. If a dict is provided, it must be of the",
            String.format(
                "same form as the protobuf message :class:`%s`",
                PythonDocstringUtil.napoleonType(
                    requestTypeName, context.getNamer().getVersionedDirectoryNamespace()))));

    paramDoc.typeName("iterator[dict|" + requestTypeName + "]");
    return paramDoc.build();
  }

  private List<ParamDocView> generateMethodParamDocs(
      GapicMethodContext context, Iterable<Field> fields) {
    SurfaceNamer namer = context.getNamer();
    GapicMethodConfig methodConfig = context.getMethodConfig();
    ImmutableList.Builder<ParamDocView> docs = ImmutableList.builder();
    for (Field field : fields) {
      if (isRequestTokenParam(methodConfig, field)) {
        continue;
      }

      SimpleParamDocView.Builder paramDoc = SimpleParamDocView.newBuilder();
      paramDoc.paramName(namer.getVariableName(field));
      paramDoc.typeName(namer.getParamTypeName(context.getTypeTable(), field.getType()));
      ImmutableList.Builder<String> docLines = ImmutableList.builder();
      if (isPageSizeParam(methodConfig, field)) {
        docLines.add(
            "The maximum number of resources contained in the",
            "underlying API response. If page streaming is performed per-",
            "resource, this parameter does not affect the return value. If page",
            "streaming is performed per-page, this determines the maximum number",
            "of resources in a page.");
      } else {
        docLines.addAll(namer.getDocLines(field));
        boolean isMessageField = field.getType().isMessage() && !field.getType().isMap();
        boolean isMapContainingMessage =
            field.getType().isMap() && field.getType().getMapValueField().getType().isMessage();
        if (isMessageField || isMapContainingMessage) {
          String messageType;
          if (isMapContainingMessage) {
            messageType =
                context
                    .getTypeTable()
                    .getFullNameForElementType(field.getType().getMapValueField().getType());
          } else {
            messageType = context.getTypeTable().getFullNameForElementType(field.getType());
          }
          docLines.add(
              "If a dict is provided, it must be of the same form as the protobuf",
              String.format(
                  "message :class:`%s`",
                  PythonDocstringUtil.napoleonType(
                      messageType, namer.getVersionedDirectoryNamespace())));
        }
      }
      paramDoc.lines(docLines.build());
      docs.add(paramDoc.build());
    }
    return docs.build();
  }

  private boolean isPageSizeParam(GapicMethodConfig methodConfig, Field field) {
    return methodConfig.isPageStreaming()
        && methodConfig.getPageStreaming().hasPageSizeField()
        && field.equals(methodConfig.getPageStreaming().getPageSizeField());
  }

  private boolean isRequestTokenParam(GapicMethodConfig methodConfig, Field field) {
    return methodConfig.isPageStreaming()
        && field.equals(methodConfig.getPageStreaming().getRequestTokenField());
  }

  private ParamDocView generateOptionsParamDoc() {
    SimpleParamDocView.Builder paramDoc = SimpleParamDocView.newBuilder();
    paramDoc.paramName("options");
    paramDoc.typeName("~google.gax.CallOptions");
    paramDoc.lines(
        ImmutableList.of(
            "Overrides the default", "settings for this call, e.g, timeout, retries etc."));
    return paramDoc.build();
  }
}

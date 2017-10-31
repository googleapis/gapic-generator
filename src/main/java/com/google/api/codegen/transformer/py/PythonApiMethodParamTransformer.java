/* Copyright 2017 Google LLC
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

import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.transformer.ApiMethodParamTransformer;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.py.PythonDocstringUtil;
import com.google.api.codegen.viewmodel.DynamicLangDefaultableParamView;
import com.google.api.codegen.viewmodel.ParamDocView;
import com.google.api.codegen.viewmodel.SimpleParamDocView;
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
              .name(context.getNamer().getRequestVariableName(context.getMethodModel()))
              .defaultValue("")
              .build());
    } else {
      for (FieldModel field : context.getMethodConfig().getRequiredFields()) {
        DynamicLangDefaultableParamView.Builder param =
            DynamicLangDefaultableParamView.newBuilder();
        param.name(context.getNamer().getVariableName(field));
        param.defaultValue("");
        methodParams.add(param.build());
      }
      for (FieldModel field : context.getMethodConfig().getOptionalFields()) {
        if (isRequestTokenParam(context.getMethodConfig(), field)) {
          continue;
        }
        DynamicLangDefaultableParamView.Builder param =
            DynamicLangDefaultableParamView.newBuilder();
        param.name(context.getNamer().getVariableName(field));
        if (field.isRepeated() || field.isMessage() || field.isEnum() || field.getOneof() != null) {
          param.defaultValue("None");
        } else {
          param.defaultValue(context.getTypeTable().getSnippetZeroValueAndSaveNicknameFor(field));
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
    if (context.getMethodModel().getRequestStreaming()) {
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
    String requestTypeName =
        context.getMethodModel().getInputTypeName(context.getTypeTable()).getFullName();
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
      GapicMethodContext context, Iterable<FieldModel> fields) {
    SurfaceNamer namer = context.getNamer();
    MethodConfig methodConfig = context.getMethodConfig();
    ImmutableList.Builder<ParamDocView> docs = ImmutableList.builder();
    for (FieldModel field : fields) {
      if (isRequestTokenParam(methodConfig, field)) {
        continue;
      }

      SimpleParamDocView.Builder paramDoc = SimpleParamDocView.newBuilder();
      paramDoc.paramName(namer.getVariableName(field));
      paramDoc.typeName(namer.getParamTypeName(context.getTypeTable(), field));
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
        boolean isMessageField = field.isMessage() && !field.isMap();
        boolean isMapContainingMessage = field.isMap() && field.getMapValueField().isMessage();
        if (isMessageField || isMapContainingMessage) {
          String messageType;
          if (isMapContainingMessage) {
            messageType =
                context.getTypeTable().getFullNameForElementType(field.getMapValueField());
          } else {
            messageType = context.getTypeTable().getFullNameForElementType(field);
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

  private boolean isPageSizeParam(MethodConfig methodConfig, FieldModel field) {
    return methodConfig.isPageStreaming()
        && methodConfig.getPageStreaming().hasPageSizeField()
        && field.equals(methodConfig.getPageStreaming().getPageSizeField());
  }

  private boolean isRequestTokenParam(MethodConfig methodConfig, FieldModel field) {
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

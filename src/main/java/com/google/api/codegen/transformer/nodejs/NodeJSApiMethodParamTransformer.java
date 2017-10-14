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
package com.google.api.codegen.transformer.nodejs;

import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.transformer.ApiMethodParamTransformer;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.DynamicLangDefaultableParamView;
import com.google.api.codegen.viewmodel.ParamDocView;
import com.google.api.codegen.viewmodel.SimpleParamDocView;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class NodeJSApiMethodParamTransformer implements ApiMethodParamTransformer {

  @Override
  public List<DynamicLangDefaultableParamView> generateMethodParams(GapicMethodContext context) {
    ImmutableList.Builder<DynamicLangDefaultableParamView> methodParams = ImmutableList.builder();
    methodParams.addAll(generateDefaultableParams(context));

    DynamicLangDefaultableParamView.Builder optionsParam =
        DynamicLangDefaultableParamView.newBuilder();
    optionsParam.name("options");
    optionsParam.defaultValue("null");
    methodParams.add(optionsParam.build());

    return methodParams.build();
  }

  private List<DynamicLangDefaultableParamView> generateDefaultableParams(
      GapicMethodContext context) {
    if (context.getMethodModel().getRequestStreaming()) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<DynamicLangDefaultableParamView> methodParams = ImmutableList.builder();
    for (FieldModel field : context.getMethodConfig().getRequiredFields()) {
      DynamicLangDefaultableParamView param =
          DynamicLangDefaultableParamView.newBuilder()
              .name(context.getNamer().getVariableName(field))
              .defaultValue("")
              .build();
      methodParams.add(param);
    }
    return methodParams.build();
  }

  @Override
  public List<ParamDocView> generateParamDocs(GapicMethodContext context) {
    ImmutableList.Builder<ParamDocView> docs = ImmutableList.builder();
    if (!context.getMethodModel().getRequestStreaming()) {
      docs.add(generateRequestObjectParamDoc(context));
      docs.addAll(
          generateMethodParamDocs(context, context.getMethodConfig().getRequiredFields(), false));
      docs.addAll(
          generateMethodParamDocs(context, context.getMethodConfig().getOptionalFields(), true));
    }
    docs.add(generateOptionsParamDoc());
    return docs.build();
  }

  private ParamDocView generateRequestObjectParamDoc(GapicMethodContext context) {
    MethodConfig methodConfig = context.getMethodConfig();
    SimpleParamDocView.Builder paramDoc = SimpleParamDocView.newBuilder();
    paramDoc.paramName(context.getNamer().localVarName(Name.from("request")));
    paramDoc.lines(ImmutableList.of("The request object that will be sent."));

    String typeName = "Object";
    Iterable<FieldModel> optionalParams = removePageTokenFromFields(methodConfig);
    if (!methodConfig.getRequiredFieldConfigs().iterator().hasNext()
        && !optionalParams.iterator().hasNext()) {
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

  private List<FieldModel> removePageTokenFromFields(MethodConfig methodConfig) {
    ImmutableList.Builder<FieldModel> newFields = ImmutableList.builder();
    for (FieldModel field : methodConfig.getOptionalFields()) {
      if (methodConfig.isPageStreaming()
          && field.equals(methodConfig.getPageStreaming().getRequestTokenField())) {
        continue;
      }
      newFields.add(field);
    }
    return newFields.build();
  }

  private List<ParamDocView> generateMethodParamDocs(
      GapicMethodContext context, Iterable<FieldModel> fields, boolean isOptional) {
    SurfaceNamer namer = context.getNamer();
    MethodConfig methodConfig = context.getMethodConfig();
    ImmutableList.Builder<ParamDocView> docs = ImmutableList.builder();
    for (FieldModel field : fields) {
      if (isRequestTokenParam(methodConfig, field)) {
        continue;
      }

      SimpleParamDocView.Builder paramDoc = SimpleParamDocView.newBuilder();
      paramDoc.paramName("request." + namer.getVariableName(field));

      String typeName = namer.getParamTypeName(context.getTypeTable(), field);
      paramDoc.typeName(typeName + (isOptional ? "=" : ""));
      List<String> fieldDocLines = namer.getDocLines(field);
      ImmutableList.Builder<String> docLines = ImmutableList.builder();
      if (isPageSizeParam(methodConfig, field)) {
        docLines.add(
            "The maximum number of resources contained in the underlying API",
            "response. If page streaming is performed per-resource, this",
            "parameter does not affect the return value. If page streaming is",
            "performed per-page, this determines the maximum number of",
            "resources in a page.");
      } else {
        docLines.addAll(fieldDocLines);
      }

      paramDoc.lines(docLines.build());
      docs.add(paramDoc.build());
    }
    return docs.build();
  }

  private boolean isRequestTokenParam(MethodConfig methodConfig, FieldModel field) {
    return methodConfig.isPageStreaming()
        && field.equals(methodConfig.getPageStreaming().getRequestTokenField());
  }

  private boolean isPageSizeParam(MethodConfig methodConfig, FieldModel field) {
    return methodConfig.isPageStreaming()
        && methodConfig.getPageStreaming().hasPageSizeField()
        && field.equals(methodConfig.getPageStreaming().getPageSizeField());
  }
}

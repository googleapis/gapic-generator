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
package com.google.api.codegen.transformer.php;

import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.transformer.ApiMethodParamTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ZeroValuePurpose;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.DynamicLangDefaultableParamView;
import com.google.api.codegen.viewmodel.MapParamDocView;
import com.google.api.codegen.viewmodel.ParamDocView;
import com.google.api.codegen.viewmodel.SimpleParamDocView;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import java.util.List;

public class PhpApiMethodParamTransformer implements ApiMethodParamTransformer {
  @Override
  public List<DynamicLangDefaultableParamView> generateMethodParams(
      MethodTransformerContext context) {
    ImmutableList.Builder<DynamicLangDefaultableParamView> methodParams = ImmutableList.builder();
    methodParams.addAll(generateDefaultableParams(context));

    TypeRef arrayType = TypeRef.fromPrimitiveName("string").makeRepeated();

    DynamicLangDefaultableParamView.Builder optionalArgs =
        DynamicLangDefaultableParamView.newBuilder();
    optionalArgs.name(context.getNamer().localVarName(Name.from("optional", "args")));
    optionalArgs.defaultValue(
        context
            .getTypeTable()
            .getZeroValueAndSaveNicknameFor(arrayType, ZeroValuePurpose.Initialization));
    methodParams.add(optionalArgs.build());

    return methodParams.build();
  }

  @Override
  public List<ParamDocView> generateParamDocs(MethodTransformerContext context) {
    ImmutableList.Builder<ParamDocView> paramDocs = ImmutableList.builder();
    paramDocs.addAll(getMethodParamDocs(context, context.getMethodConfig().getRequiredFields()));
    paramDocs.add(getOptionalArrayParamDoc(context, context.getMethodConfig().getOptionalFields()));
    return paramDocs.build();
  }

  private List<DynamicLangDefaultableParamView> generateDefaultableParams(
      MethodTransformerContext context) {
    if (context.getMethod().getRequestStreaming()) {
      return ImmutableList.<DynamicLangDefaultableParamView>of();
    }
    ImmutableList.Builder<DynamicLangDefaultableParamView> methodParams = ImmutableList.builder();
    for (Field field : context.getMethodConfig().getRequiredFields()) {
      DynamicLangDefaultableParamView param =
          DynamicLangDefaultableParamView.newBuilder()
              .name(context.getNamer().getVariableName(field))
              .defaultValue("")
              .build();
      methodParams.add(param);
    }
    return methodParams.build();
  }

  private List<ParamDocView> getMethodParamDocs(
      MethodTransformerContext context, Iterable<Field> fields) {
    if (context.getMethod().getRequestStreaming()) {
      return ImmutableList.<ParamDocView>of();
    }
    MethodConfig methodConfig = context.getMethodConfig();
    ImmutableList.Builder<ParamDocView> paramDocs = ImmutableList.builder();
    for (Field field : fields) {
      SimpleParamDocView.Builder paramDoc = SimpleParamDocView.newBuilder();
      paramDoc.paramName(context.getNamer().getVariableName(field));
      paramDoc.typeName(context.getTypeTable().getAndSaveNicknameFor(field.getType()));

      ImmutableList.Builder<String> docLines = ImmutableList.builder();
      if (methodConfig.isPageStreaming()
          && methodConfig.getPageStreaming().hasPageSizeField()
          && field.equals(methodConfig.getPageStreaming().getPageSizeField())) {
        docLines.add(
            "The maximum number of resources contained in the underlying API",
            "response. The API may return fewer values in a page, even if",
            "there are additional values to be retrieved.");
      } else if (methodConfig.isPageStreaming()
          && field.equals(methodConfig.getPageStreaming().getRequestTokenField())) {
        docLines.add(
            "A page token is used to specify a page of values to be returned.",
            "If no page token is specified (the default), the first page",
            "of values will be returned. Any page token used here must have",
            "been generated by a previous call to the API.");
      } else {
        docLines.addAll(context.getNamer().getDocLines(field));
      }

      paramDoc.lines(docLines.build());

      paramDocs.add(paramDoc.build());
    }
    return paramDocs.build();
  }

  private ParamDocView getOptionalArrayParamDoc(
      MethodTransformerContext context, Iterable<Field> fields) {
    MapParamDocView.Builder paramDoc = MapParamDocView.newBuilder();

    Name optionalArgsName = Name.from("optional", "args");

    paramDoc.paramName(context.getNamer().localVarName(optionalArgsName));
    paramDoc.typeName(context.getNamer().getOptionalArrayTypeName());

    paramDoc.firstLine("Optional.");
    paramDoc.remainingLines(ImmutableList.<String>of());

    paramDoc.arrayKeyDocs(
        ImmutableList.<ParamDocView>builder()
            .addAll(getMethodParamDocs(context, fields))
            .addAll(getCallSettingsParamDocList(context))
            .build());

    return paramDoc.build();
  }

  private List<ParamDocView> getCallSettingsParamDocList(MethodTransformerContext context) {
    ImmutableList.Builder<ParamDocView> arrayKeyDocs = ImmutableList.builder();

    Name retrySettingsName = Name.from("retry", "settings");
    Name timeoutMillisName = Name.from("timeout", "millis");

    if (context.getNamer().methodHasRetrySettings(context.getMethodConfig())) {
      SimpleParamDocView.Builder retrySettingsDoc = SimpleParamDocView.newBuilder();
      retrySettingsDoc.paramName(context.getNamer().localVarName(retrySettingsName));
      retrySettingsDoc.typeName(context.getNamer().getRetrySettingsTypeName());
      // TODO figure out a reliable way to line-wrap comments across all languages
      // instead of encoding it in the transformer
      String retrySettingsDocText =
          String.format(
              "Retry settings to use for this call. If present, then\n%s is ignored.",
              context.getNamer().varReference(timeoutMillisName));
      List<String> retrySettingsDocLines = context.getNamer().getDocLines(retrySettingsDocText);
      retrySettingsDoc.lines(retrySettingsDocLines);
      arrayKeyDocs.add(retrySettingsDoc.build());
    }

    if (context.getNamer().methodHasTimeoutSettings(context.getMethodConfig())) {
      SimpleParamDocView.Builder timeoutDoc = SimpleParamDocView.newBuilder();
      timeoutDoc.typeName(
          context.getTypeTable().getAndSaveNicknameFor(TypeRef.of(Type.TYPE_INT32)));
      timeoutDoc.paramName(context.getNamer().localVarName(timeoutMillisName));
      // TODO figure out a reliable way to line-wrap comments across all languages
      // instead of encoding it in the transformer
      String timeoutMillisDocText = "Timeout to use for this call.";
      if (context.getNamer().methodHasRetrySettings(context.getMethodConfig())) {
        timeoutMillisDocText +=
            String.format(
                " Only used if %s\nis not set.",
                context.getNamer().varReference(retrySettingsName));
      }
      List<String> timeoutMillisDocLines = context.getNamer().getDocLines(timeoutMillisDocText);
      timeoutDoc.lines(timeoutMillisDocLines);
      arrayKeyDocs.add(timeoutDoc.build());
    }

    return arrayKeyDocs.build();
  }
}

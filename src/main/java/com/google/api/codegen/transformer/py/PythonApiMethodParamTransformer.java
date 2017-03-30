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

import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.transformer.ApiMethodParamTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.DynamicLangDefaultableParamView;
import com.google.api.codegen.viewmodel.ParamDocView;
import com.google.api.codegen.viewmodel.SimpleParamDocView;
import com.google.api.tools.framework.model.Field;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class PythonApiMethodParamTransformer implements ApiMethodParamTransformer {
  @Override
  public List<DynamicLangDefaultableParamView> generateMethodParams(
      MethodTransformerContext context) {
    // TODO(eoogbe): implement this method when migrating to MVVM
    return ImmutableList.<DynamicLangDefaultableParamView>of();
  }

  @Override
  public List<ParamDocView> generateParamDocs(MethodTransformerContext context) {
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

  private ParamDocView generateRequestStreamingParamDoc(MethodTransformerContext context) {
    SimpleParamDocView.Builder paramDoc = SimpleParamDocView.newBuilder();
    paramDoc.paramName(context.getNamer().localVarName(Name.from("requests")));
    paramDoc.lines(ImmutableList.of("The input objects."));

    String requestTypeName =
        context.getTypeTable().getFullNameFor(context.getMethod().getInputType());
    paramDoc.typeName("iterator[:class:`" + requestTypeName + "`]");
    return paramDoc.build();
  }

  private List<ParamDocView> generateMethodParamDocs(
      MethodTransformerContext context, Iterable<Field> fields) {
    SurfaceNamer namer = context.getNamer();
    MethodConfig methodConfig = context.getMethodConfig();
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
      }
      paramDoc.lines(docLines.build());
      docs.add(paramDoc.build());
    }
    return docs.build();
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

  private ParamDocView generateOptionsParamDoc() {
    SimpleParamDocView.Builder paramDoc = SimpleParamDocView.newBuilder();
    paramDoc.paramName("options");
    paramDoc.typeName(":class:`google.gax.CallOptions`");
    paramDoc.lines(
        ImmutableList.of(
            "Overrides the default", "settings for this call, e.g, timeout, retries etc."));
    return paramDoc.build();
  }
}

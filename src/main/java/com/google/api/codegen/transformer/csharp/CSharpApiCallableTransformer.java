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
package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.transformer.ApiCallableTransformer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.viewmodel.ApiCallSettingsView;
import com.google.api.codegen.viewmodel.ApiCallableView;
import com.google.api.codegen.viewmodel.RetryCodesDefinitionView;
import com.google.api.codegen.viewmodel.RetryParamsDefinitionView;
import com.google.api.tools.framework.model.Method;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CSharpApiCallableTransformer extends ApiCallableTransformer {

  @Override
  public List<ApiCallableView> generateStaticLangApiCallables(SurfaceTransformerContext context) {
    List<ApiCallableView> callableMembers = new ArrayList<>();

    for (Method method : context.getNonStreamingMethods()) {
      if (context.getMethodConfig(method).getRerouteToGrpcInterface() != null) {
        // Temporary hack to exclude mixins for the moment. To be removed.
        continue;
      }
      callableMembers.addAll(generateStaticLangApiCallables(context.asMethodContext(method)));
    }

    return callableMembers;
  }

  public List<ApiCallSettingsView> generateCallSettings(
      SurfaceTransformerContext context,
      List<RetryCodesDefinitionView> retryCodes,
      List<RetryParamsDefinitionView> retryParams) {
    final Map<String, RetryCodesDefinitionView> retryCodesByKey =
        Maps.uniqueIndex(
            retryCodes,
            new Function<RetryCodesDefinitionView, String>() {
              @Override
              public String apply(RetryCodesDefinitionView v) {
                return v.key();
              }
            });
    final Map<String, RetryParamsDefinitionView> retryParamsByKey =
        Maps.uniqueIndex(
            retryParams,
            new Function<RetryParamsDefinitionView, String>() {
              @Override
              public String apply(RetryParamsDefinitionView v) {
                return v.key();
              }
            });

    List<ApiCallSettingsView> settingsMembers = new ArrayList<>();
    for (Method method : context.getNonStreamingMethods()) {
      if (context.getMethodConfig(method).getRerouteToGrpcInterface() != null) {
        // Temporary hack to exclude mixins for the moment. To be removed.
        continue;
      }
      List<ApiCallSettingsView> calls =
          FluentIterable.from(generateApiCallableSettings(context.asMethodContext(method)))
              .transform(
                  new Function<ApiCallSettingsView, ApiCallSettingsView>() {
                    @Override
                    public ApiCallSettingsView apply(ApiCallSettingsView call) {
                      return call.toBuilder()
                          .retryCodesView(retryCodesByKey.get(call.retryCodesName()))
                          .retryParamsView(retryParamsByKey.get(call.retryParamsName()))
                          .build();
                    }
                  })
              .toList();
      settingsMembers.addAll(calls);
    }
    return settingsMembers;
  }
}

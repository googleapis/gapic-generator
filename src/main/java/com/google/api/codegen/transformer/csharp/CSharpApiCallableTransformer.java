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

  public List<ApiCallSettingsView> generateCallSettings(SurfaceTransformerContext context,
      List<RetryCodesDefinitionView> retryCodes, List<RetryParamsDefinitionView> retryParams) {
    final Map<String, RetryCodesDefinitionView> retryCodesByKey = Maps.uniqueIndex(retryCodes, new Function<RetryCodesDefinitionView, String>() {
      @Override public String apply(RetryCodesDefinitionView v) {
        return v.key();
      }
    });
    final Map<String, RetryParamsDefinitionView> retryParamsByKey = Maps.uniqueIndex(retryParams, new Function<RetryParamsDefinitionView, String>() {
      @Override public String apply(RetryParamsDefinitionView v) {
        return v.key();
      }
    });
    
    List<ApiCallSettingsView> settingsMembers = new ArrayList<>();
    for (Method method : context.getNonStreamingMethods()) {
      List<ApiCallSettingsView> calls = FluentIterable.from(generateApiCallableSettings(context.asMethodContext(method)))
          .transform(new Function<ApiCallSettingsView, ApiCallSettingsView>() {
            @Override public ApiCallSettingsView apply(ApiCallSettingsView call) {
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

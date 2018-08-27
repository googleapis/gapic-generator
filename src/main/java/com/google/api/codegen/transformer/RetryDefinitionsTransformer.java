/* Copyright 2016 Google LLC
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

import static com.google.api.codegen.configgen.mergers.RetryMerger.DEFAULT_RETRY_CODES;

import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.RetryCodesDefinitionProto;
import com.google.api.codegen.RetryParamsDefinitionProto;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.RetryCodesDefinitionView;
import com.google.api.codegen.viewmodel.RetryParamsDefinitionView;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;

/** RetryDefinitionsTransformer generates retry definitions from a service model. */
public class RetryDefinitionsTransformer {

  public List<RetryCodesDefinitionView> generateRetryCodesDefinitions(InterfaceContext context) {
    List<RetryCodesDefinitionView> definitions = new ArrayList<>();

    final SurfaceNamer namer = context.getNamer();
    for (Entry<String, List<String>> retryCodesDef :
        context.getInterfaceConfig().getRetryCodesDefinition().entrySet()) {
      List<String> codeNames = new ArrayList<>();
      for (String code : retryCodesDef.getValue()) {
        codeNames.add(namer.getStatusCodeName(code));
      }
      Collections.sort(codeNames);
      definitions.add(
          RetryCodesDefinitionView.newBuilder()
              .key(retryCodesDef.getKey())
              .name(namer.getRetryDefinitionName(retryCodesDef.getKey()))
              .retryFilterMethodName(namer.retryFilterMethodName(retryCodesDef.getKey()))
              .codes(retryCodesDef.getValue())
              .codeNames(codeNames)
              .build());
    }

    return definitions;
  }

  public static ImmutableMap<String, List<String>> createRetryCodesDefinition(
      DiagCollector diagCollector, @Nullable InterfaceConfigProto interfaceConfigProto) {
    ImmutableMap.Builder<String, List<String>> builder = ImmutableMap.builder();
    if (interfaceConfigProto != null) {
      for (RetryCodesDefinitionProto retryDef : interfaceConfigProto.getRetryCodesDefList()) {
        // Enforce ordering on set for baseline test consistency.
        Set<String> codes = new TreeSet<>();
        for (String codeText : retryDef.getRetryCodesList()) {
          try {
            codes.add(String.valueOf(codeText));
          } catch (IllegalArgumentException e) {
            diagCollector.addDiag(
                Diag.error(
                    SimpleLocation.TOPLEVEL,
                    "status code not found: '%s' (in interface %s)",
                    codeText,
                    interfaceConfigProto.getName()));
          }
        }
        builder.put(
            retryDef.getName(), (new ImmutableList.Builder<String>()).addAll(codes).build());
      }
    } else {
      // Use default values for retry settings.
      builder.putAll(DEFAULT_RETRY_CODES);
    }
    if (diagCollector.getErrorCount() > 0) {
      return null;
    }
    return builder.build();
  }

  public static ImmutableMap<String, RetryParamsDefinitionProto> createRetrySettingsDefinition(
      InterfaceConfigProto interfaceConfigProto) {
    ImmutableMap.Builder<String, RetryParamsDefinitionProto> builder = ImmutableMap.builder();
    for (RetryParamsDefinitionProto retryDef : interfaceConfigProto.getRetryParamsDefList()) {
      builder.put(retryDef.getName(), retryDef);
    }
    return builder.build();
  }

  public List<RetryParamsDefinitionView> generateRetryParamsDefinitions(InterfaceContext context) {
    List<RetryParamsDefinitionView> definitions = new ArrayList<>();

    SurfaceNamer namer = context.getNamer();
    for (Entry<String, RetryParamsDefinitionProto> retryCodesDef :
        context.getInterfaceConfig().getRetrySettingsDefinition().entrySet()) {
      RetryParamsDefinitionView.Builder view = RetryParamsDefinitionView.newBuilder();
      view.key(retryCodesDef.getKey());
      view.name(namer.publicMethodName(Name.from(retryCodesDef.getKey())));
      view.retryBackoffMethodName(namer.retryBackoffMethodName(retryCodesDef.getKey()));
      view.timeoutBackoffMethodName(namer.timeoutBackoffMethodName(retryCodesDef.getKey()));
      view.params(retryCodesDef.getValue());
      definitions.add(view.build());
    }

    return definitions;
  }
}

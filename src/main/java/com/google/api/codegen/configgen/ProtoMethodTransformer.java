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
package com.google.api.codegen.configgen;

import static com.google.api.codegen.configgen.transformer.RetryTransformer.DEFAULT_MAX_RETRY_DELAY;

import com.google.api.BackendRule;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProtoMethodModel;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.Iterables;
import java.util.List;
import java.util.stream.Collectors;

/** MethodTransformer implementation for proto Methods. */
public class ProtoMethodTransformer implements MethodTransformer {
  private static final PagingParameters PAGING_PARAMETERS = new ProtoPagingParameters();
  private static final int MILLIS_PER_SECOND = 1000;
  private static final int REQUEST_OBJECT_METHOD_THRESHOLD = 1;

  @Override
  public boolean isIgnoredParameter(String parameter) {
    return PAGING_PARAMETERS.getIgnoredParameters().contains(parameter);
  }

  @Override
  public String getTimeoutMillis(MethodModel method) {
    return String.valueOf(getTimeoutMillis((ProtoMethodModel) method));
  }

  public static long getTimeoutMillis(ProtoMethodModel method) {
    Model model = method.getProtoMethod().getModel();
    for (BackendRule backendRule : model.getServiceConfig().getBackend().getRulesList()) {
      if (backendRule.getSelector().equals(method.getFullName())) {
        return (long) Math.ceil(backendRule.getDeadline() * MILLIS_PER_SECOND);
      }
    }
    return DEFAULT_MAX_RETRY_DELAY;
  }

  @Override
  public boolean isRequestObjectMethod(MethodModel method) {
    // use all fields for the following check; if there are ignored fields for flattening
    // purposes, the caller still needs a way to set them (by using the request object method).
    List<String> parameterList = getParameterList(method);
    int fieldCount = Iterables.size(method.getInputFields());
    return (fieldCount > REQUEST_OBJECT_METHOD_THRESHOLD || fieldCount != parameterList.size())
        && !method.getRequestStreaming();
  }

  @Override
  public List<String> getParameterList(MethodModel method) {
    return method
        .getInputFields()
        .stream()
        .filter(f -> f.getOneof() == null && !isIgnoredParameter(f.getSimpleName()))
        .map(FieldModel::getSimpleName)
        .collect(Collectors.toList());
  }
}

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

import com.google.api.BackendRule;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProtoMethodModel;
import com.google.api.tools.framework.model.Model;

import static com.google.api.codegen.configgen.transformer.RetryTransformer.DEFAULT_MAX_RETRY_DELAY;

/** MethodTransformer implementation for proto Methods. */
public class ProtoMethodTransformer implements MethodTransformer {
  private static final PagingParameters PAGING_PARAMETERS = new ProtoPagingParameters();
  private static final int MILLIS_PER_SECOND = 1000;

  @Override
  public boolean isIgnoredParameter(String parameter) {
    return PAGING_PARAMETERS.getIgnoredParameters().contains(parameter);
  }

  @Override
  public String getTimeoutMillis(MethodModel method) {
    Model model = ((ProtoMethodModel) method).getProtoMethod().getModel();
    return getTimeoutMillis(model, method.getFullName());
  }

  public static String getTimeoutMillis(Model model, String methodFullName) {
    for (BackendRule backendRule : model.getServiceConfig().getBackend().getRulesList()) {
      if (backendRule.getSelector().equals(methodFullName)) {
        return String.valueOf((int) Math.ceil(backendRule.getDeadline() * MILLIS_PER_SECOND));
      }
    }
    return String.valueOf(DEFAULT_MAX_RETRY_DELAY);
  }
}

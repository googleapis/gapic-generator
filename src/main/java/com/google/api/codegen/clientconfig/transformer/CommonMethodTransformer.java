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
package com.google.api.codegen.clientconfig.transformer;

import com.google.api.codegen.clientconfig.viewmodel.MethodView;
import com.google.api.codegen.config.MethodConfig;

public class CommonMethodTransformer implements MethodTransformer {
  private final BatchingTransformer batchingTransformer = new BatchingTransformer();

  @Override
  public MethodView generateMethod(MethodConfig methodConfig) {
    MethodView.Builder method = MethodView.newBuilder();
    method.name(methodConfig.getMethodModel().getSimpleName());
    String retryCodesName = methodConfig.getRetryCodesConfigName();
    String retryParamsName = methodConfig.getRetrySettingsConfigName();
    method.timeoutMillis(String.valueOf(methodConfig.getTimeout().toMillis()));
    method.isRetryingSupported(!retryCodesName.isEmpty() && !retryParamsName.isEmpty());
    method.retryCodesName(retryCodesName);
    method.retryParamsName(retryParamsName);
    method.batching(batchingTransformer.generateBatching(methodConfig));
    return method.build();
  }
}

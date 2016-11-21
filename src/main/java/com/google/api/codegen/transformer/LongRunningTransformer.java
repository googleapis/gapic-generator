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
package com.google.api.codegen.transformer;

import com.google.api.codegen.config.LongRunningConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.viewmodel.LongRunningOperationDetailView;

public class LongRunningTransformer {
  public LongRunningOperationDetailView generateDetailView(MethodTransformerContext context) {
    MethodConfig methodConfig = context.getMethodConfig();
    LongRunningConfig lroConfig = methodConfig.getLongRunningConfig();
    String responseType =
        context
            .getTypeTable()
            .getAndSaveNicknameFor(
                context.getMethodConfig().getLongRunningConfig().getReturnType());

    return LongRunningOperationDetailView.newBuilder()
        .operationReturnType(
            context
                .getNamer()
                .getAndSaveOperationResponseTypeName(
                    context.getMethod(), context.getTypeTable(), methodConfig))
        .operationResponseType(context.getNamer().plainType(responseType))
        .build();
  }
}

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

import com.google.api.codegen.config.LongRunningConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.viewmodel.LongRunningOperationDetailView;

public class LongRunningTransformer {
  LongRunningOperationDetailView generateDetailView(MethodContext context) {
    MethodConfig methodConfig = context.getMethodConfig();
    LongRunningConfig lroConfig = methodConfig.getLongRunningConfig();
    SurfaceNamer namer = context.getNamer();

    String clientReturnTypeName =
        namer.getAndSaveOperationResponseTypeName(
            context.getMethodModel(), context.getTypeTable(), methodConfig);
    String operationPayloadTypeName =
        namer.getLongRunningOperationTypeName(context.getTypeTable(), lroConfig.getReturnType());
    String metadataTypeName =
        namer.getLongRunningOperationTypeName(context.getTypeTable(), lroConfig.getMetadataType());

    return LongRunningOperationDetailView.newBuilder()
        .methodName(namer.getApiMethodName(context.getMethodModel(), VisibilityConfig.PUBLIC))
        .constructorName(namer.getTypeConstructor(clientReturnTypeName))
        .clientReturnTypeName(clientReturnTypeName)
        .operationPayloadTypeName(operationPayloadTypeName)
        .isEmptyOperation(lroConfig.getReturnType().isEmptyType())
        .metadataTypeName(metadataTypeName)
        .implementsDelete(lroConfig.implementsDelete())
        .implementsCancel(lroConfig.implementsCancel())
        .initialPollDelay(lroConfig.getInitialPollDelay().toMillis())
        .pollDelayMultiplier(lroConfig.getPollDelayMultiplier())
        .maxPollDelay(lroConfig.getMaxPollDelay().toMillis())
        .totalPollTimeout(lroConfig.getTotalPollTimeout().toMillis())
        .build();
  }
}

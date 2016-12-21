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

import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.config.LongRunningConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.viewmodel.LongRunningOperationDetailView;

public class LongRunningTransformer {
  public LongRunningOperationDetailView generateDetailView(MethodTransformerContext context) {
    MethodConfig methodConfig = context.getMethodConfig();
    LongRunningConfig lroConfig = methodConfig.getLongRunningConfig();
    SurfaceNamer namer = context.getNamer();

    String clientReturnTypeName =
        namer.getAndSaveOperationResponseTypeName(
            context.getMethod(), context.getTypeTable(), methodConfig);
    String operationPayloadTypeName =
        context.getTypeTable().getAndSaveNicknameFor(lroConfig.getReturnType());
    String metadataTypeName =
        context.getTypeTable().getAndSaveNicknameFor(lroConfig.getMetadataType());

    return LongRunningOperationDetailView.newBuilder()
        .methodName(namer.getApiMethodName(context.getMethod(), VisibilityConfig.PUBLIC))
        .constructorName(namer.getTypeConstructor(clientReturnTypeName))
        .clientReturnTypeName(clientReturnTypeName)
        .operationPayloadTypeName(namer.valueType(operationPayloadTypeName))
        .isEmptyOperation(ServiceMessages.s_isEmptyType(lroConfig.getReturnType()))
        .metadataTypeName(namer.valueType(metadataTypeName))
        .implementsDelete(lroConfig.implementsDelete())
        .implementsCancel(lroConfig.implementsCancel())
        .build();
  }
}

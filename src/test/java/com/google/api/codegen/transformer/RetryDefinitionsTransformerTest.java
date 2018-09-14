/* Copyright 2018 Google LLC
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

import static com.google.api.codegen.configgen.transformer.RetryTransformer.RETRY_CODES_IDEMPOTENT_NAME;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.RETRY_CODES_NON_IDEMPOTENT_NAME;
import static com.google.rpc.Code.INVALID_ARGUMENT;
import static com.google.rpc.Code.PERMISSION_DENIED;

import com.google.api.Retry;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.RetryCodesDefinitionProto;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.BoundedDiagCollector;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.grpc.Status;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class RetryDefinitionsTransformerTest {

  private static final ProtoParser protoParser = Mockito.mock(ProtoParser.class);
  private static final Method noRetryCodesMethod = Mockito.mock(Method.class);
  private static final Method idempotentMethod = Mockito.mock(Method.class);
  private static final Method nonIdempotentMethod = Mockito.mock(Method.class);
  private static final Method permissionDeniedMethod = Mockito.mock(Method.class);
  private static final Interface apiInterface = Mockito.mock(Interface.class);

  private static final String noRetryCodesMethodName = "NoRetryMethod";
  private static final String idempotentMethodName = "NonIdempotentMethod";
  private static final String nonIdempotentMethodName = "IdempotentMethod";
  private static final String permissionDeniedMethodName = "PermissionDeniedMethod";

  private static final String permissionDeniedRetryName = "permission_denied";

  private static InterfaceConfigProto interfaceConfigProto;

  @BeforeClass
  public static void startUp() {
    Mockito.when(noRetryCodesMethod.getSimpleName()).thenReturn("NoRetryMethod");
    Mockito.when(nonIdempotentMethod.getSimpleName()).thenReturn("NonIdempotentMethod");
    Mockito.when(idempotentMethod.getSimpleName()).thenReturn("IdempotentMethod");
    Mockito.when(permissionDeniedMethod.getSimpleName()).thenReturn("PermissionDeniedMethod");

    Mockito.when(permissionDeniedMethod.getSimpleName()).thenReturn("permissionDeniedMethod");
    Mockito.when(apiInterface.getMethods())
        .thenReturn(
            ImmutableList.of(
                noRetryCodesMethod, idempotentMethod, nonIdempotentMethod, permissionDeniedMethod));

    InterfaceConfigProto interfaceConfigCustomRetry =
        InterfaceConfigProto.newBuilder()
            .addRetryCodesDef(
                RetryCodesDefinitionProto.newBuilder()
                    .setName(RETRY_CODES_IDEMPOTENT_NAME)
                    // This is not the default list for idempotent retry codes.
                    .addRetryCodes(Status.Code.RESOURCE_EXHAUSTED.name()))
            .addRetryCodesDef(
                RetryCodesDefinitionProto.newBuilder()
                    .setName(RETRY_CODES_NON_IDEMPOTENT_NAME)
                    // This is not the default list for non-idempotent retry codes.
                    .addRetryCodes(Status.Code.FAILED_PRECONDITION.name()))
            .addMethods(MethodConfigProto.newBuilder().setName(noRetryCodesMethodName))
            .addMethods(
                MethodConfigProto.newBuilder()
                    .setName(nonIdempotentMethodName)
                    .setRetryCodesName(RETRY_CODES_NON_IDEMPOTENT_NAME))
            .addMethods(
                MethodConfigProto.newBuilder()
                    .setName(idempotentMethodName)
                    .setRetryCodesName(RETRY_CODES_IDEMPOTENT_NAME))
            .addMethods(
                MethodConfigProto.newBuilder()
                    .setName(permissionDeniedMethodName)
                    .setRetryCodesName(permissionDeniedRetryName))
            .build();

    Mockito.when(protoParser.isHttpGetMethod(noRetryCodesMethod)).thenReturn(true);

    Mockito.when(protoParser.getRetry(noRetryCodesMethod)).thenReturn(Retry.getDefaultInstance());
    Mockito.when(protoParser.getRetry(idempotentMethod))
        .thenReturn(Retry.newBuilder().addCodes(INVALID_ARGUMENT).build());
    Mockito.when(protoParser.getRetry(nonIdempotentMethod)).thenReturn(Retry.getDefaultInstance());
    Mockito.when(protoParser.getRetry(permissionDeniedMethod))
        .thenReturn(Retry.newBuilder().addCodes(PERMISSION_DENIED).build());
  }

  @Test
  public void testNoConfigProto() {

    DiagCollector diagCollector = new BoundedDiagCollector();
    ImmutableMap.Builder<String, String> methodNameToRetryCodeNames = ImmutableMap.builder();
    Map<String, List<String>> retryCodesDef =
        RetryDefinitionsTransformer.createRetryCodesDefinition(
            diagCollector, null, apiInterface, methodNameToRetryCodeNames);

    Map<String, String> retryCodesMap = methodNameToRetryCodeNames.build();
  }

  //  @Test
  //  public void testWithConfigAndInterface() {
  //
  //    DiagCollector diagCollector = new BoundedDiagCollector();
  //    Map<String, List<String>> retryCodesDef =
  // RetryDefinitionsTransformer.createRetryCodesDefinition(diagCollector,
  //        null, apiInterface, )
  //  }
  //
  //  @Test
  //  public void testWithInterfaceAndConfigDifferentFromDefaults() {
  //
  //    DiagCollector diagCollector = new BoundedDiagCollector();
  //    Map<String, List<String>> retryCodesDef =
  // RetryDefinitionsTransformer.createRetryCodesDefinition(diagCollector,
  //        null, apiInterface, )
  //  }

  @Test
  public void testConfigNoProto() {}
}

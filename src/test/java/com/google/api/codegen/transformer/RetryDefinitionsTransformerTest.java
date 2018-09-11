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

import static com.google.api.codegen.configgen.mergers.RetryMerger.DEFAULT_RETRY_CODES;
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
import com.google.common.truth.Truth;
import io.grpc.Status;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class RetryDefinitionsTransformerTest {

  private static final ProtoParser protoParser = Mockito.mock(ProtoParser.class);
  private static final Method httpGetMethod = Mockito.mock(Method.class);
  private static final Method idempotentMethod = Mockito.mock(Method.class);
  private static final Method nonIdempotentMethod = Mockito.mock(Method.class);
  private static final Method permissionDeniedMethod = Mockito.mock(Method.class);
  private static final Interface apiInterface = Mockito.mock(Interface.class);

  private static final String GET_HTTP_METHOD_NAME = "HttpGetMethod";
  private static final String IDEMPOTENT_METHOD_NAME = "IdempotentMethod";
  private static final String NON_IDEMPOTENT_METHOD_NAME = "NonIdempotentMethod";
  private static final String PERMISSION_DENIED_METHOD_NAME = "PermissionDeniedMethod";

  private static InterfaceConfigProto interfaceConfigProto;

  @BeforeClass
  public static void startUp() {
    Mockito.when(httpGetMethod.getSimpleName()).thenReturn(GET_HTTP_METHOD_NAME);
    Mockito.when(nonIdempotentMethod.getSimpleName()).thenReturn(NON_IDEMPOTENT_METHOD_NAME);
    Mockito.when(idempotentMethod.getSimpleName()).thenReturn(IDEMPOTENT_METHOD_NAME);
    Mockito.when(permissionDeniedMethod.getSimpleName()).thenReturn(PERMISSION_DENIED_METHOD_NAME);

    Mockito.when(apiInterface.getMethods())
        .thenReturn(
            // Only include idempotentMethod in interfaceConfigProto, but not in the interface.getMethods() list.
            ImmutableList.of(
                httpGetMethod, nonIdempotentMethod, permissionDeniedMethod));

    interfaceConfigProto =
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
            .addMethods(
                MethodConfigProto.newBuilder()
                    .setName(GET_HTTP_METHOD_NAME)
                    .setRetryCodesName(RETRY_CODES_IDEMPOTENT_NAME))
            .addMethods(
                MethodConfigProto.newBuilder()
                    .setName(NON_IDEMPOTENT_METHOD_NAME)
                    .setRetryCodesName(RETRY_CODES_NON_IDEMPOTENT_NAME))
            .addMethods(
                MethodConfigProto.newBuilder()
                    .setName(IDEMPOTENT_METHOD_NAME)
                    .setRetryCodesName(RETRY_CODES_IDEMPOTENT_NAME))
            .addMethods(
                MethodConfigProto.newBuilder()
                    // Leave retry codes empty in this method config.
                    .setName(PERMISSION_DENIED_METHOD_NAME))
            .build();

    Mockito.when(protoParser.isHttpGetMethod(httpGetMethod)).thenReturn(true);

    Mockito.when(protoParser.getRetry(httpGetMethod)).thenReturn(Retry.getDefaultInstance());
    Mockito.when(protoParser.getRetry(nonIdempotentMethod)).thenReturn(Retry.getDefaultInstance());
    Mockito.when(protoParser.getRetry(permissionDeniedMethod))
        .thenReturn(Retry.newBuilder().addCodes(PERMISSION_DENIED).build());
  }

  @Test
  public void testWithConfigAndInterface() {

    DiagCollector diagCollector = new BoundedDiagCollector();
    ImmutableMap.Builder<String, String> methodNameToRetryCodeNames = ImmutableMap.builder();

    Map<String, List<String>> retryCodesDef =
        RetryDefinitionsTransformer.createRetryCodesDefinition(
            diagCollector,
            interfaceConfigProto,
            apiInterface,
            methodNameToRetryCodeNames,
            protoParser);

    Map<String, String> retryCodesMap = methodNameToRetryCodeNames.build();

    Truth.assertThat(retryCodesMap.size()).isEqualTo(3);
    String getHttpRetryName = retryCodesMap.get(GET_HTTP_METHOD_NAME);
    String nonIdempotentRetryName = retryCodesMap.get(NON_IDEMPOTENT_METHOD_NAME);
    String permissionDeniedName = retryCodesMap.get(PERMISSION_DENIED_METHOD_NAME);

    // httpGetMethod was an HTTP Get method, so it has two codes by default; disregard the extra
    // retry code specified in the InterfaceConfigProto.
    Truth.assertThat(retryCodesDef.get(getHttpRetryName)).isEqualTo(DEFAULT_RETRY_CODES.get(RETRY_CODES_IDEMPOTENT_NAME));

    // Even though config proto gives [FAILED_PRECONDITION] for nonIdempotentMethod, proto annotations have nothing
    // specified in retry codes.
    Truth.assertThat(retryCodesDef.get(nonIdempotentRetryName).size()).isEqualTo(0);

    // For permissionDeniedMethod, Config proto gives [] and proto method gives [PERMISSION_DENIED].
    Truth.assertThat(retryCodesDef.get(permissionDeniedName).size()).isEqualTo(1);
    Truth.assertThat(retryCodesDef.get(permissionDeniedName).get(0))
        .isEqualTo(PERMISSION_DENIED.name());
  }
}

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
import static com.google.api.codegen.transformer.RetryDefinitionsTransformer.NO_RETRY_CODE_DEF_NAME;
import static com.google.api.codegen.transformer.RetryDefinitionsTransformer.RETRY_CODES_FOR_HTTP_GET;
import static com.google.rpc.Code.CANCELLED;
import static com.google.rpc.Code.FAILED_PRECONDITION;
import static com.google.rpc.Code.PERMISSION_DENIED;
import static com.google.rpc.Code.RESOURCE_EXHAUSTED;

import com.google.api.Retry;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.RetryCodesDefinitionProto;
import com.google.api.codegen.config.RetryCodesConfig;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.BoundedDiagCollector;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.truth.Truth;
import io.grpc.Status;
import java.util.HashMap;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class RetryDefinitionsTransformerTest {

  private static final ProtoParser protoParser = Mockito.mock(ProtoParser.class);
  private static final Method httpGetMethod = Mockito.mock(Method.class);
  private static final Method cancelledMethod = Mockito.mock(Method.class);
  private static final Method nonIdempotentMethod = Mockito.mock(Method.class);
  private static final Method permissionDeniedMethod = Mockito.mock(Method.class);
  private static final Interface apiInterface = Mockito.mock(Interface.class);

  private static final String GET_HTTP_METHOD_NAME = "HttpGetMethod";
  private static final String IDEMPOTENT_METHOD_NAME = "IdempotentMethod";
  private static final String NON_IDEMPOTENT_METHOD_NAME = "NonIdempotentMethod";
  private static final String PERMISSION_DENIED_METHOD_NAME = "PermissionDeniedMethod";
  private static final String CANCELED_METHOD_NAME = "CanceledMethod";

  private static InterfaceConfigProto interfaceConfigProto;

  @BeforeClass
  public static void startUp() {
    Mockito.when(httpGetMethod.getSimpleName()).thenReturn(GET_HTTP_METHOD_NAME);
    Mockito.when(nonIdempotentMethod.getSimpleName()).thenReturn(NON_IDEMPOTENT_METHOD_NAME);
    Mockito.when(permissionDeniedMethod.getSimpleName()).thenReturn(PERMISSION_DENIED_METHOD_NAME);
    Mockito.when(cancelledMethod.getSimpleName()).thenReturn(CANCELED_METHOD_NAME);

    // Protofile Interface only contains methods with names
    // [GET_HTTP_METHOD_NAME, NON_IDEMPOTENT_METHOD_NAME, PERMISSION_DENIED_METHOD_NAME,
    // CANCELED_METHOD_NAME].
    // ConfigProto only contains methods with names
    // [GET_HTTP_METHOD_NAME, NON_IDEMPOTENT_METHOD_NAME, PERMISSION_DENIED_METHOD_NAME,
    // RETRY_CODES_IDEMPOTENT_NAME].
    Mockito.when(apiInterface.getMethods())
        .thenReturn(
            ImmutableList.of(
                httpGetMethod, nonIdempotentMethod, permissionDeniedMethod, cancelledMethod));

    interfaceConfigProto =
        InterfaceConfigProto.newBuilder()
            .addRetryCodesDef(
                RetryCodesDefinitionProto.newBuilder()
                    .setName(RETRY_CODES_IDEMPOTENT_NAME)
                    // This is not the default list for idempotent retry codes.
                    .addRetryCodes(Status.Code.RESOURCE_EXHAUSTED.name()))
            .addRetryCodesDef(
                RetryCodesDefinitionProto.newBuilder()
                    // Empty retry codes list.
                    .setName(RETRY_CODES_NON_IDEMPOTENT_NAME))
            .addRetryCodesDef(
                RetryCodesDefinitionProto.newBuilder()
                    // Empty retry codes list.
                    // Force protomethods to escape this retry name.
                    .setName(RetryDefinitionsTransformer.NO_RETRY_CODE_DEF_NAME))
            .addMethods(MethodConfigProto.newBuilder().setName(GET_HTTP_METHOD_NAME))
            // Don't set a retry code in config proto for GET_HTTP_METHOD
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
                    .setName(PERMISSION_DENIED_METHOD_NAME)
                    .setRetryCodesName(NO_RETRY_CODE_DEF_NAME))
            .build();

    Mockito.when(protoParser.isHttpGetMethod(httpGetMethod)).thenReturn(true);

    Mockito.when(protoParser.getRetry(httpGetMethod)).thenReturn(Retry.getDefaultInstance());
    Mockito.when(protoParser.getRetry(nonIdempotentMethod)).thenReturn(Retry.getDefaultInstance());
    Mockito.when(protoParser.getRetry(permissionDeniedMethod))
        .thenReturn(Retry.newBuilder().addCodes(PERMISSION_DENIED).build());
    Mockito.when(protoParser.getRetry(cancelledMethod))
        .thenReturn(Retry.newBuilder().addCodes(CANCELLED).build());
  }

  @Test
  public void testWithConfigAndInterface() {

    DiagCollector diagCollector = new BoundedDiagCollector();
    Map<String, String> retryCodesMap = new HashMap<>();
    RetryCodesConfig retryCodesConfig = RetryCodesConfig.create(
        diagCollector, interfaceConfigProto, apiInterface, protoParser);

    Map<String, ImmutableSet<String>> retryCodesDef =
        RetryCodesConfig.create(
            diagCollector, interfaceConfigProto, apiInterface, protoParser);

    Truth.assertThat(retryCodesMap.size()).isEqualTo(5);
    String getHttpRetryName = retryCodesMap.get(GET_HTTP_METHOD_NAME);
    String nonIdempotentRetryName = retryCodesMap.get(NON_IDEMPOTENT_METHOD_NAME);
    String permissionDeniedRetryName = retryCodesMap.get(NO_RETRY_CODE_DEF_NAME);
    String idempotentRetryName = retryCodesMap.get(IDEMPOTENT_METHOD_NAME);
    String cancelledRetryName = retryCodesMap.get(CANCELED_METHOD_NAME);

    // GET_HTTP_METHOD_NAME had to be escaped because it was defined in the config proto retry code
    // map already.
    Truth.assertThat(getHttpRetryName)
        .isEqualTo(RetryDefinitionsTransformer.HTTP_RETRY_CODE_DEF_NAME);
    Truth.assertThat(nonIdempotentRetryName).isEqualTo(RETRY_CODES_NON_IDEMPOTENT_NAME);
    Truth.assertThat(permissionDeniedRetryName).isEqualTo("http_get");
    Truth.assertThat(idempotentRetryName).isEqualTo("http_get");
    Truth.assertThat(cancelledRetryName).isEqualTo("http_get");

    // httpGetMethod was an HTTP Get method, so it has two codes by default; config proto didn't
    // have a retry config.
    Truth.assertThat(retryCodesDef.get(getHttpRetryName)).isEqualTo(RETRY_CODES_FOR_HTTP_GET);

    // Config proto gives [FAILED_PRECONDITION] for nonIdempotentMethod; method from protofile has
    // [] for retry codes.
    Truth.assertThat(retryCodesDef.get(nonIdempotentRetryName).size()).isEqualTo(1);
    Truth.assertThat(retryCodesDef.get(nonIdempotentRetryName).iterator().next())
        .isEqualTo(FAILED_PRECONDITION.name());

    // For permissionDeniedMethod, Config proto gives [] and proto method gives [PERMISSION_DENIED].
    Truth.assertThat(retryCodesDef.get(permissionDeniedRetryName).size()).isEqualTo(1);
    Truth.assertThat(retryCodesDef.get(permissionDeniedRetryName).iterator().next())
        .isEqualTo(PERMISSION_DENIED.name());

    // cancelledMethod is not contained in Config proto, and the proto method gives [CANCELLED].
    Truth.assertThat(retryCodesDef.get(cancelledRetryName).iterator().next())
        .isEqualTo("CanceledMethod_retry");

    Truth.assertThat(retryCodesDef.get(idempotentRetryName).iterator().next())
        .isEqualTo(RESOURCE_EXHAUSTED.name());
  }
}

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
import static com.google.common.truth.Truth.assertThat;
import static com.google.rpc.Code.RESOURCE_EXHAUSTED;

import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.RetryCodesDefinitionProto;
import com.google.api.codegen.config.GapicProductConfig.GapicConfigPresence;
import com.google.api.codegen.config.RetryCodesConfig;
import com.google.api.codegen.configgen.transformer.RetryTransformer;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.BoundedDiagCollector;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.common.collect.ImmutableList;
import io.grpc.Status;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class RetryDefinitionsTransformerTest {

  private static final ProtoParser protoParser = Mockito.mock(ProtoParser.class);
  private static final Method httpGetMethod = Mockito.mock(Method.class);
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
    Mockito.when(permissionDeniedMethod.getSimpleName()).thenReturn(PERMISSION_DENIED_METHOD_NAME);

    Mockito.when(protoParser.isHttpGetMethod(httpGetMethod)).thenReturn(true);

    // Protofile Interface only contains methods with names
    // [GET_HTTP_METHOD_NAME, NON_IDEMPOTENT_METHOD_NAME, PERMISSION_DENIED_METHOD_NAME.
    Mockito.when(apiInterface.getMethods())
        .thenReturn(ImmutableList.of(httpGetMethod, nonIdempotentMethod, permissionDeniedMethod));

    // ConfigProto only contains methods with names
    // [GET_HTTP_METHOD_NAME, NON_IDEMPOTENT_METHOD_NAME, PERMISSION_DENIED_METHOD_NAME,
    // RETRY_CODES_IDEMPOTENT_NAME].
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
                    .setName(RetryCodesConfig.NO_RETRY_CODE_DEF_NAME))
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
                    .setRetryCodesName(RetryCodesConfig.NO_RETRY_CODE_DEF_NAME))
            .build();
  }

  @Test
  public void testWithConfigAndInterface() {

    DiagCollector diagCollector = new BoundedDiagCollector();

    RetryCodesConfig retryCodesConfig =
        RetryCodesConfig.create(
            diagCollector,
            interfaceConfigProto,
            apiInterface,
            protoParser,
            GapicConfigPresence.PROVIDED);

    Map<String, ImmutableList<String>> retryCodesDef = retryCodesConfig.getRetryCodesDefinition();
    Map<String, String> retryCodesMap = retryCodesConfig.getMethodRetryNames();

    assertThat(retryCodesMap.size()).isEqualTo(4);
    String getHttpRetryName = retryCodesMap.get(GET_HTTP_METHOD_NAME);
    String nonIdempotentRetryName = retryCodesMap.get(NON_IDEMPOTENT_METHOD_NAME);
    String permissionDeniedRetryName = retryCodesMap.get(PERMISSION_DENIED_METHOD_NAME);
    String idempotentRetryName = retryCodesMap.get(IDEMPOTENT_METHOD_NAME);

    // GET_HTTP_METHOD_NAME had to be escaped because it was defined in the config proto retry code
    // map already.
    assertThat(getHttpRetryName).isEqualTo(RetryCodesConfig.HTTP_RETRY_CODE_DEF_NAME);
    assertThat(nonIdempotentRetryName).isEqualTo(RETRY_CODES_NON_IDEMPOTENT_NAME);
    assertThat(permissionDeniedRetryName).isEqualTo(RetryCodesConfig.NO_RETRY_CODE_DEF_NAME);
    assertThat(idempotentRetryName).isEqualTo(RetryTransformer.RETRY_CODES_IDEMPOTENT_NAME);

    // httpGetMethod was an HTTP Get method, so it has two codes by default; config proto didn't
    // have a retry config.
    assertThat(retryCodesDef.get(getHttpRetryName))
        .isEqualTo(RetryCodesConfig.RETRY_CODES_FOR_HTTP_GET);

    // Config proto gives [] for nonIdempotentMethod; method from protofile has
    // [] for retry codes.
    assertThat(retryCodesDef.get(nonIdempotentRetryName).size()).isEqualTo(0);

    // For permissionDeniedMethod, Config proto gives [] and proto method gives [PERMISSION_DENIED].
    assertThat(retryCodesDef.get(permissionDeniedRetryName).size()).isEqualTo(0);

    assertThat(retryCodesDef.get(idempotentRetryName).iterator().next())
        .isEqualTo(RESOURCE_EXHAUSTED.name());
  }

  @Test
  public void testWithInterfaceOnly() {
    // During GAPIC config migration, we should only create retry codes for methods named in the
    // GAPIC config.
    InterfaceConfigProto bareBonesConfigProto =
        InterfaceConfigProto.newBuilder()
            .addMethods(MethodConfigProto.newBuilder().setName(GET_HTTP_METHOD_NAME))
            .addMethods(MethodConfigProto.newBuilder().setName(NON_IDEMPOTENT_METHOD_NAME))
            .addMethods(MethodConfigProto.newBuilder().setName(PERMISSION_DENIED_METHOD_NAME))
            .build();

    DiagCollector diagCollector = new BoundedDiagCollector();

    RetryCodesConfig retryCodesConfig =
        RetryCodesConfig.create(
            diagCollector,
            bareBonesConfigProto,
            apiInterface,
            protoParser,
            GapicConfigPresence.NOT_PROVIDED);

    Map<String, ImmutableList<String>> retryCodesDef = retryCodesConfig.getRetryCodesDefinition();
    Map<String, String> retryCodesMap = retryCodesConfig.getMethodRetryNames();

    assertThat(retryCodesMap).hasSize(3);
    String getHttpRetryName = retryCodesMap.get(GET_HTTP_METHOD_NAME);
    String nonIdempotentRetryName = retryCodesMap.get(NON_IDEMPOTENT_METHOD_NAME);
    String permissionDeniedRetryName = retryCodesMap.get(PERMISSION_DENIED_METHOD_NAME);

    // GET_HTTP_METHOD_NAME had to be escaped because it was defined in the config proto retry code
    // map already.
    assertThat(getHttpRetryName).isEqualTo(RetryCodesConfig.HTTP_RETRY_CODE_DEF_NAME);
    assertThat(nonIdempotentRetryName).isEqualTo(RetryCodesConfig.NO_RETRY_CODE_DEF_NAME);
    assertThat(permissionDeniedRetryName).isEqualTo("no_retry");

    // httpGetMethod was an HTTP Get method, so it has two codes by default; config proto didn't
    // have a retry config.
    assertThat(retryCodesDef.get(getHttpRetryName))
        .isEqualTo(RetryCodesConfig.RETRY_CODES_FOR_HTTP_GET);

    // Method from protofile has
    // [] for retry codes.
    assertThat(retryCodesDef.get(nonIdempotentRetryName).size()).isEqualTo(0);

    // For permissionDeniedMethod, proto method gives [PERMISSION_DENIED].
    assertThat(retryCodesDef.get(permissionDeniedRetryName).size()).isEqualTo(0);
    // assertThat(retryCodesDef.get(permissionDeniedRetryName).iterator().next())
    //     .isEqualTo(PERMISSION_DENIED.name());
  }
}

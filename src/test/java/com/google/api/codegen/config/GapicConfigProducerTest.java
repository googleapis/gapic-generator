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
package com.google.api.codegen.config;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.codegen.CodegenTestUtil;
import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.MixedPathTestDataLocator;
import com.google.api.codegen.RetryCodesDefinitionProto;
import com.google.api.codegen.RetryParamsDefinitionProto;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.grpc.ServiceConfig;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.api.tools.framework.model.testing.TestDataLocator;
import com.google.common.collect.ImmutableList;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GapicConfigProducerTest {

  @ClassRule public static TemporaryFolder tempDir = new TemporaryFolder();

  @Test
  public void missingConfigSchemaVersion() {
    TestDataLocator locator = MixedPathTestDataLocator.create(this.getClass());
    locator.addTestDataSource(CodegenTestUtil.class, "testsrc/common");
    Model model =
        CodegenTestUtil.readModel(
            locator, tempDir, new String[] {"myproto.proto"}, new String[] {"myproto.yaml"});

    ConfigProto configProto =
        CodegenTestUtil.readConfig(
            model.getDiagReporter().getDiagCollector(),
            locator,
            new String[] {"missing_config_schema_version.yaml"});
    GapicProductConfig.create(model, configProto, null, null, null, TargetLanguage.JAVA, null);
    Diag expectedError =
        Diag.error(
            SimpleLocation.TOPLEVEL, "config_schema_version field is required in GAPIC yaml.");
    assertThat(model.getDiagReporter().getDiagCollector().hasErrors()).isTrue();
    assertThat(model.getDiagReporter().getDiagCollector().getDiags()).contains(expectedError);
  }

  @Test
  public void missingInterface() {
    TestDataLocator locator = MixedPathTestDataLocator.create(this.getClass());
    locator.addTestDataSource(CodegenTestUtil.class, "testsrc/common");
    Model model =
        CodegenTestUtil.readModel(
            locator, tempDir, new String[] {"myproto.proto"}, new String[] {"myproto.yaml"});

    ConfigProto configProto =
        CodegenTestUtil.readConfig(
            model.getDiagReporter().getDiagCollector(),
            locator,
            new String[] {"missing_interface_v1.yaml"});
    GapicProductConfig.create(model, configProto, null, null, null, TargetLanguage.JAVA, null);
    Diag expectedError =
        Diag.error(
            SimpleLocation.TOPLEVEL,
            "interface not found: google.example.myproto.v1.MyUnknownProto. Interfaces: [google.example.myproto.v1.MyProto]");
    assertThat(model.getDiagReporter().getDiagCollector().hasErrors()).isTrue();
    assertThat(model.getDiagReporter().getDiagCollector().getDiags()).contains(expectedError);
  }

  @Test
  public void testFindMethod() {
    MethodConfigProto.Builder methodBuilder = MethodConfigProto.newBuilder();
    InterfaceConfigProto.Builder service = InterfaceConfigProto.newBuilder();

    methodBuilder.setName("foo");
    service.addMethods(methodBuilder);
    methodBuilder.setName("bar");
    service.addMethods(methodBuilder);
    methodBuilder.setName("baz");
    service.addMethods(methodBuilder);

    assertThat(GapicProductConfig.findMethod(service, "baz")).isEqualTo(2);
    assertThat(GapicProductConfig.findMethod(service, "dne")).isEqualTo(-1);
  }

  @Test
  public void testFindAndSetRetry() {
    String RETRY_CODES = "retry_policy_1_codes";
    String RETRY_PARAMS = "retry_policy_1_params";
    String NO_RETRY_CODES = "no_retry_codes";
    String NO_RETRY_PARAMS = "no_retry_params";
    long timeout = 60000;
    MethodConfigProto.Builder methodBuilder = MethodConfigProto.newBuilder();
    InterfaceConfigProto.Builder service = InterfaceConfigProto.newBuilder();

    methodBuilder.setName("foo");
    service.addMethods(methodBuilder);
    methodBuilder.setName("bar");
    methodBuilder.setRetryCodesName(NO_RETRY_CODES);
    methodBuilder.setRetryParamsName(NO_RETRY_PARAMS);
    service.addMethods(methodBuilder);
    methodBuilder.setName("baz");
    service.addMethods(methodBuilder);

    // test basic find and set
    GapicProductConfig.findAndSetRetry(service, false, "foo", RETRY_CODES, RETRY_PARAMS, timeout);
    assertThat(service.getMethods(0).getRetryCodesName()).isEqualTo(RETRY_CODES);
    assertThat(service.getMethods(0).getRetryParamsName()).isEqualTo(RETRY_PARAMS);
    assertThat(service.getMethods(0).getTimeoutMillis()).isEqualTo(timeout);

    // test do not overwrite, e.g. service-defined retry not overwriting a method-defined retry
    GapicProductConfig.findAndSetRetry(service, false, "bar", RETRY_CODES, RETRY_PARAMS, timeout);
    assertThat(service.getMethods(1).getRetryCodesName()).isEqualTo(NO_RETRY_CODES);
    assertThat(service.getMethods(1).getRetryParamsName()).isEqualTo(NO_RETRY_PARAMS);
    assertThat(service.getMethods(1).getTimeoutMillis()).isNotEqualTo(timeout);

    // test overwrite, e.g. method-defined retry overwriting a service-defined retry or existing
    // GAPIC-defined retry
    GapicProductConfig.findAndSetRetry(service, true, "baz", RETRY_CODES, RETRY_PARAMS, timeout);
    assertThat(service.getMethods(2).getRetryCodesName()).isEqualTo(RETRY_CODES);
    assertThat(service.getMethods(2).getRetryParamsName()).isEqualTo(RETRY_PARAMS);
    assertThat(service.getMethods(2).getTimeoutMillis()).isEqualTo(timeout);

    // test add method config not defined in original GAPIC interface (but in the proto)
    GapicProductConfig.findAndSetRetry(service, false, "buz", RETRY_CODES, RETRY_PARAMS, timeout);
    assertThat(service.getMethods(3).getRetryCodesName()).isEqualTo(RETRY_CODES);
    assertThat(service.getMethods(3).getRetryParamsName()).isEqualTo(RETRY_PARAMS);
    assertThat(service.getMethods(3).getName()).isEqualTo("buz");
    assertThat(service.getMethods(3).getTimeoutMillis()).isEqualTo(timeout);
  }

  @Test
  public void testAddRetryConfigIfAbsent() {
    RetryParamsDefinitionProto.Builder retryParamsBuilder = RetryParamsDefinitionProto.newBuilder();
    RetryCodesDefinitionProto.Builder retryCodesBuilder = RetryCodesDefinitionProto.newBuilder();
    InterfaceConfigProto.Builder service = InterfaceConfigProto.newBuilder();

    retryCodesBuilder.setName("retry_policy_1_codes");
    retryParamsBuilder.setName("retry_policy_1_params");

    // test basic add new retry config
    GapicProductConfig.addRetryConfigIfAbsent(service, retryCodesBuilder, retryParamsBuilder);
    assertThat(service.getRetryCodesDef(0).getName()).isEqualTo(retryCodesBuilder.getName());
    assertThat(service.getRetryParamsDef(0).getName()).isEqualTo(retryParamsBuilder.getName());

    // test attempt to add duplicate retry config
    GapicProductConfig.addRetryConfigIfAbsent(service, retryCodesBuilder, retryParamsBuilder);
    assertThat(service.getRetryParamsDefList().size()).isEqualTo(1);
  }

  @Test
  public void testInjectRetryPolicyConfig() {
    TestDataLocator locator = MixedPathTestDataLocator.create(this.getClass());
    locator.addTestDataSource(CodegenTestUtil.class, "testsrc/common");

    Model model =
        CodegenTestUtil.readModel(
            locator,
            tempDir,
            new String[] {"library.proto", "another_service.proto"},
            new String[] {"library.yaml"});

    ServiceConfig serviceConfig =
        CodegenTestUtil.readGRPCServiceConfig(
            model.getDiagReporter().getDiagCollector(),
            locator,
            "library_grpc_service_config.json");

    ConfigProto configProto =
        CodegenTestUtil.readConfig(
            model.getDiagReporter().getDiagCollector(),
            locator,
            new String[] {"library_v2_gapic.yaml"});

    GapicProductConfig product =
        GapicProductConfig.create(
            model,
            configProto,
            null,
            "google.example.library.v1",
            null,
            TargetLanguage.GO,
            serviceConfig);

    assertThat(product).isNotNull();

    InterfaceConfig libraryInterface =
        product.getInterfaceConfig("google.example.library.v1.LibraryService");
    Map<String, RetryParamsDefinitionProto> params = libraryInterface.getRetrySettingsDefinition();
    assertThat(params.get("retry_policy_1_params")).isNotNull();
    assertThat(params.get("no_retry_params")).isNotNull();
    assertThat(params.get("no_retry_params").getTotalTimeoutMillis()).isEqualTo(60000);
    Map<String, ImmutableList<String>> codes =
        libraryInterface.getRetryCodesConfig().getRetryCodesDefinition();
    assertThat(codes.get("retry_policy_1_codes")).isNotNull();
    assertThat(codes.get("no_retry_codes")).isNotNull();
  }
}

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
import com.google.api.codegen.MixedPathTestDataLocator;
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
    locator.addTestDataSource(CodegenTestUtil.class, "testsrc/protoannotations");
    Model model =
        CodegenTestUtil.readModel(
            locator, tempDir, new String[] {"myproto.proto"}, new String[] {"myproto.yaml"});

    ConfigProto configProto =
        CodegenTestUtil.readConfig(
            model.getDiagReporter().getDiagCollector(),
            locator,
            new String[] {"missing_config_schema_version.yaml"});
    GapicProductConfig.create(
        model, configProto, null, null, null, TargetLanguage.JAVA, null, TransportProtocol.GRPC);
    Diag expectedError =
        Diag.error(
            SimpleLocation.TOPLEVEL, "config_schema_version field is required in GAPIC yaml.");
    assertThat(model.getDiagReporter().getDiagCollector().hasErrors()).isTrue();
    assertThat(model.getDiagReporter().getDiagCollector().getDiags()).contains(expectedError);
  }

  @Test
  public void missingInterface() {
    TestDataLocator locator = MixedPathTestDataLocator.create(this.getClass());
    locator.addTestDataSource(CodegenTestUtil.class, "testsrc/protoannotations");
    Model model =
        CodegenTestUtil.readModel(
            locator, tempDir, new String[] {"myproto.proto"}, new String[] {"myproto.yaml"});

    ConfigProto configProto =
        CodegenTestUtil.readConfig(
            model.getDiagReporter().getDiagCollector(),
            locator,
            new String[] {"missing_interface_v1.yaml"});
    GapicProductConfig.create(
        model, configProto, null, null, null, TargetLanguage.JAVA, null, TransportProtocol.GRPC);
    Diag expectedError =
        Diag.error(
            SimpleLocation.TOPLEVEL,
            "interface not found: google.example.myproto.v1.MyUnknownProto. Interfaces:"
                + " [google.example.myproto.v1.MyProto]");
    assertThat(model.getDiagReporter().getDiagCollector().hasErrors()).isTrue();
    assertThat(model.getDiagReporter().getDiagCollector().getDiags()).contains(expectedError);
  }

  @Test
  public void testCreateProductWithGRPCServiceConfig() {
    TestDataLocator locator = MixedPathTestDataLocator.create(this.getClass());
    locator.addTestDataSource(CodegenTestUtil.class, "testsrc/protoannotations");

    Model model =
        CodegenTestUtil.readModel(
            locator,
            tempDir,
            new String[] {"library.proto", "common_resources.proto", "another_service.proto"},
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
            serviceConfig,
            TransportProtocol.GRPC);

    assertThat(model.getDiagReporter().getDiagCollector().hasErrors()).isFalse();
    assertThat(product).isNotNull();

    InterfaceConfig libraryInterface =
        product.getInterfaceConfig("google.example.library.v1.LibraryService");
    Map<String, RetryParamsDefinitionProto> params = libraryInterface.getRetrySettingsDefinition();
    assertThat(params.get("retry_policy_1_params")).isNotNull();
    assertThat(params.get("no_retry_1_params")).isNotNull();
    assertThat(params.get("no_retry_1_params").getTotalTimeoutMillis()).isEqualTo(60000);
    assertThat(params.get("no_retry_params")).isNotNull();
    assertThat(params.get("no_retry_params").getTotalTimeoutMillis()).isEqualTo(0);
    Map<String, ImmutableList<String>> codes =
        libraryInterface.getRetryCodesConfig().getRetryCodesDefinition();
    assertThat(codes.get("retry_policy_1_codes")).isNotNull();
    assertThat(codes.get("no_retry_1_codes")).isNotNull();
    assertThat(codes.get("no_retry_codes")).isNotNull();
  }
}

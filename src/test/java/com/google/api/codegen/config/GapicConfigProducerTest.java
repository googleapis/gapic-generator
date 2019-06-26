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
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.api.tools.framework.model.testing.TestDataLocator;
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
  public void findMethod() {
    MethodConfigProto.Builder mcb = MethodConfigProto.newBuilder();
    InterfaceConfigProto.Builder service = InterfaceConfigProto.newBuilder();

    mcb.setName("foo");
    service.addMethods(mcb);
    mcb.setName("bar");
    service.addMethods(mcb);
    mcb.setName("baz");
    service.addMethods(mcb);

    assertThat(GapicProductConfig.findMethod(service, "baz")).isEqualTo(2);
    assertThat(GapicProductConfig.findMethod(service, "dne")).isEqualTo(-1);
  }

  @Test
  public void findAndSetRetry() {
    String rc = "retry_policy_1_codes";
    String rp = "retry_policy_1_params";
    String norc = "no_retry_codes";
    String norp = "no_retry_params";
    MethodConfigProto.Builder mcb = MethodConfigProto.newBuilder();
    InterfaceConfigProto.Builder service = InterfaceConfigProto.newBuilder();

    mcb.setName("foo");
    service.addMethods(mcb);
    mcb.setName("bar");
    mcb.setRetryCodesName(norc);
    mcb.setRetryParamsName(norp);
    service.addMethods(mcb);
    mcb.setName("baz");
    service.addMethods(mcb);

    // test basic find and set
    GapicProductConfig.findAndSetRetry(service, false, "foo", rc, rp);
    assertThat(service.getMethods(0).getRetryCodesName()).isEqualTo(rc);
    assertThat(service.getMethods(0).getRetryParamsName()).isEqualTo(rp);

    // test do not overwrite, e.g. service-defined retry not overwriting a method-defined retry
    GapicProductConfig.findAndSetRetry(service, false, "bar", rc, rp);
    assertThat(service.getMethods(1).getRetryCodesName()).isEqualTo(norc);
    assertThat(service.getMethods(1).getRetryParamsName()).isEqualTo(norp);

    // test overwrite, e.g. method-defined retry overwriting a service-defined retry or existing
    // GAPIC-defined retry
    GapicProductConfig.findAndSetRetry(service, true, "baz", rc, rp);
    assertThat(service.getMethods(2).getRetryCodesName()).isEqualTo(rc);
    assertThat(service.getMethods(2).getRetryParamsName()).isEqualTo(rp);

    // test add method config not defined in original GAPIC interface (but in the proto)
    GapicProductConfig.findAndSetRetry(service, false, "buz", rc, rp);
    assertThat(service.getMethods(3).getRetryCodesName()).isEqualTo(rc);
    assertThat(service.getMethods(3).getRetryParamsName()).isEqualTo(rp);
    assertThat(service.getMethods(3).getName()).isEqualTo("buz");
  }

  @Test
  public void addRetryConfigIfAbsent() {
    RetryParamsDefinitionProto.Builder rpb = RetryParamsDefinitionProto.newBuilder();
    RetryCodesDefinitionProto.Builder rcb = RetryCodesDefinitionProto.newBuilder();
    InterfaceConfigProto.Builder service = InterfaceConfigProto.newBuilder();

    rcb.setName("retry_policy_1_params");
    rpb.setName("retry_policy_1_params");

    // test basic add new retry config
    GapicProductConfig.addRetryConfigIfAbsent(service, rcb, rpb);
    assertThat(service.getRetryCodesDef(0).getName()).isEqualTo(rcb.getName());
    assertThat(service.getRetryParamsDef(0).getName()).isEqualTo(rpb.getName());

    // test attempt to add duplicate retry config
    GapicProductConfig.addRetryConfigIfAbsent(service, rcb, rpb);
    assertThat(service.getRetryParamsDefList().size()).isEqualTo(1);
  }
}

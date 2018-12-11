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

  private static Model model;
  private static GapicProductConfig productConfig;

  @Test
  public void missingConfigSchemaVersion() {
    TestDataLocator locator = TestDataLocator.create(GapicConfigProducerTest.class);
    locator.addTestDataSource(CodegenTestUtil.class, "testsrc/common");
    locator.addTestDataSource(CodegenTestUtil.class, "testsrc/libraryproto");
    model =
        CodegenTestUtil.readModel(
            locator, tempDir, new String[] {"myproto.proto"}, new String[] {"myproto.yaml"});

    ConfigProto configProto =
        CodegenTestUtil.readConfig(
            model.getDiagReporter().getDiagCollector(),
            locator,
            new String[] {"missing_config_schema_version.yaml"});
    productConfig = GapicProductConfig.create(model, configProto, null, null, TargetLanguage.JAVA);
    Diag expectedError =
        Diag.error(
            SimpleLocation.TOPLEVEL, "config_schema_version field is required in GAPIC yaml.");
    assertThat(model.getDiagReporter().getDiagCollector().hasErrors()).isTrue();
    assertThat(model.getDiagReporter().getDiagCollector().getDiags()).contains(expectedError);
  }
}

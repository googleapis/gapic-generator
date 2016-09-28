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
package com.google.api.codegen.transformer.go;

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.CodegenTestUtil;
import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.util.Name;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.testing.TestDataLocator;
import com.google.common.truth.Truth;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.junit.Test;

import java.util.List;

public class GoSurfaceNamerTest {
  @ClassRule public static TemporaryFolder tempDir = new TemporaryFolder();

  @Test
  public void testClientNamePrefix() {
    TestDataLocator locator = TestDataLocator.create(GoGapicSurfaceTransformerTest.class);
    Model model =
        CodegenTestUtil.readModel(
            locator, tempDir, new String[] {"myproto.proto"}, new String[] {"myproto.yaml"});

    ConfigProto configProto =
        CodegenTestUtil.readConfig(
            model.getDiagCollector(), locator, new String[] {"myproto_gapic.yaml"});

    ApiConfig apiConfig = ApiConfig.createApiConfig(model, configProto);

    if (model.getDiagCollector().hasErrors()) {
      throw new IllegalStateException(model.getDiagCollector().getDiags().toString());
    }
    GoSurfaceNamer namer = new GoSurfaceNamer(apiConfig.getPackageName());
    List<Interface> services = model.getSymbolTable().getInterfaces().asList();
    Truth.assertThat(namer.clientNamePrefix(services.get(0))).isEqualTo(Name.from());
    Truth.assertThat(namer.clientNamePrefix(services.get(1))).isEqualTo(Name.from("guru"));
  }

  // Don't drop service name if the name is different from package,
  // even if there's only one.
  @Test
  public void testClientNamePrefixSingle() {
    TestDataLocator locator = TestDataLocator.create(GoGapicSurfaceTransformerTest.class);
    Model model =
        CodegenTestUtil.readModel(
            locator,
            tempDir,
            new String[] {"singleservice.proto"},
            new String[] {"singleservice.yaml"});

    ConfigProto configProto =
        CodegenTestUtil.readConfig(
            model.getDiagCollector(), locator, new String[] {"singleservice_gapic.yaml"});

    ApiConfig apiConfig = ApiConfig.createApiConfig(model, configProto);

    if (model.getDiagCollector().hasErrors()) {
      throw new IllegalStateException(model.getDiagCollector().getDiags().toString());
    }
    GoSurfaceNamer namer = new GoSurfaceNamer(apiConfig.getPackageName());
    List<Interface> services = model.getSymbolTable().getInterfaces().asList();
    Truth.assertThat(namer.clientNamePrefix(services.get(0)))
        .isEqualTo(Name.from("oddly", "named"));
  }
}

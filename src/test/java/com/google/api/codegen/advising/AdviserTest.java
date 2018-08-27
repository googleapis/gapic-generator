/* Copyright 2017 Google LLC
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
package com.google.api.codegen.advising;

import com.google.api.codegen.CodegenTestUtil;
import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.LanguageSettingsProto;
import com.google.api.tools.framework.aspects.control.ControlConfigAspect;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.model.testing.ConfigBaselineTestCase;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;

public class AdviserTest extends ConfigBaselineTestCase {
  private ConfigProto configProto;
  private Adviser adviser;

  @Override
  public Object run() throws Exception {
    model
        .getDiagReporter()
        .getDiagSuppressor()
        .addSuppressionDirective(
            model, "control-*", Arrays.asList(ControlConfigAspect.create(model)));
    model.getDiagReporter().getDiagSuppressor().addPattern(model, "http:.*");
    model.establishStage(Merged.KEY);
    adviser.advise(model, configProto);
    return "";
  }

  @Before
  public void setup() {
    getTestDataLocator().addTestDataSource(CodegenTestUtil.class, "testsrc/common");
    getTestDataLocator()
        .addTestDataSource(CodegenTestUtil.class, "testsrc/libraryproto/configonly");
  }

  @Test
  public void missing_language_settings() throws Exception {
    configProto =
        ConfigProto.newBuilder()
            .putLanguageSettings(
                "java", buildLanguageSettings("com.google.cloud.example.library.spi.v1"))
            .putLanguageSettings(
                "python", buildLanguageSettings("google.cloud.gapic.example.library.v1"))
            .putLanguageSettings(
                "go", buildLanguageSettings("cloud.google.com/go/example/library/apiv1"))
            .putLanguageSettings("csharp", buildLanguageSettings("Google.Example.Library.V1"))
            .putLanguageSettings(
                "ruby", buildLanguageSettings("Google::Cloud::Example::Library::V1"))
            .putLanguageSettings(
                "php", buildLanguageSettings("Google\\Cloud\\Example\\Library\\V1"))
            .build();
    adviser =
        new Adviser(
            ImmutableList.<AdviserRule>of(new LanguageSettingsRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void missing_package_name() throws Exception {
    configProto =
        ConfigProto.newBuilder()
            .putLanguageSettings(
                "java", buildLanguageSettings("com.google.cloud.example.library.spi.v1"))
            .putLanguageSettings(
                "python", buildLanguageSettings("google.cloud.gapic.example.library.v1"))
            .putLanguageSettings(
                "go", buildLanguageSettings("cloud.google.com/go/example/library/apiv1"))
            .putLanguageSettings("csharp", buildLanguageSettings("Google.Example.Library.V1"))
            .putLanguageSettings(
                "ruby", buildLanguageSettings("Google::Cloud::Example::Library::V1"))
            .putLanguageSettings(
                "php", buildLanguageSettings("Google\\Cloud\\Example\\Library\\V1"))
            .putLanguageSettings("nodejs", LanguageSettingsProto.getDefaultInstance())
            .build();
    adviser =
        new Adviser(
            ImmutableList.<AdviserRule>of(new LanguageSettingsRule()), ImmutableList.<String>of());
    test("library");
  }

  private LanguageSettingsProto buildLanguageSettings(String packageName) {
    return LanguageSettingsProto.newBuilder().setPackageName(packageName).build();
  }
}

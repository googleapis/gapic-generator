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
package com.google.api.codegen.transformer.go;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.codegen.CodegenTestUtil;
import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProtoMethodModel;
import com.google.api.codegen.gapic.PackageNameCodePathMapper;
import com.google.api.codegen.transformer.DefaultFeatureConfig;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.testing.TestDataLocator;
import java.util.Collections;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GoGapicSurfaceTransformerTest {

  @ClassRule public static TemporaryFolder tempDir = new TemporaryFolder();

  private static Model model;
  private static Interface apiInterface;
  private static GapicProductConfig productConfig;

  @BeforeClass
  public static void setupClass() {
    TestDataLocator locator = TestDataLocator.create(GoGapicSurfaceTransformerTest.class);
    model =
        CodegenTestUtil.readModel(
            locator,
            tempDir,
            new String[] {"myproto.proto", "singleservice.proto"},
            new String[] {"myproto.yaml"});
    for (Interface apiInterface : model.getSymbolTable().getInterfaces()) {
      if (apiInterface.getSimpleName().equals("Gopher")) {
        GoGapicSurfaceTransformerTest.apiInterface = apiInterface;
        break;
      }
    }

    ConfigProto configProto =
        CodegenTestUtil.readConfig(
            model.getDiagReporter().getDiagCollector(),
            locator,
            new String[] {"myproto_gapic.yaml"});

    productConfig = GapicProductConfig.create(model, configProto, TargetLanguage.GO);

    if (model.getDiagReporter().getDiagCollector().hasErrors()) {
      throw new IllegalStateException(
          model.getDiagReporter().getDiagCollector().getDiags().toString());
    }
  }

  private final GoGapicSurfaceTransformer transformer =
      new GoGapicSurfaceTransformer(new PackageNameCodePathMapper());
  private GapicInterfaceContext context;

  @Before
  public void setup() {
    GoSurfaceNamer namer = new GoSurfaceNamer(productConfig.getPackageName());
    context =
        GapicInterfaceContext.create(
            apiInterface,
            productConfig,
            GoGapicSurfaceTransformer.createTypeTable(),
            namer,
            new DefaultFeatureConfig());
  }

  @Test
  public void testGetImportsPlain() {
    MethodModel method = new ProtoMethodModel(getMethod(context.getInterface(), "SimpleMethod"));
    transformer.addXApiImports(context, Collections.singletonList(method));
    transformer.generateRetryConfigDefinitions(context, Collections.singletonList(method));
    assertThat(context.getImportTypeTable().getImports()).doesNotContainKey("time");
    assertThat(context.getImportTypeTable().getImports())
        .doesNotContainKey("cloud.google.com/go/longrunning");
  }

  @Test
  public void testGetImportsRetry() {
    MethodModel method = new ProtoMethodModel(getMethod(context.getInterface(), "RetryMethod"));
    transformer.addXApiImports(context, Collections.singletonList(method));
    transformer.generateRetryConfigDefinitions(context, Collections.singletonList(method));
    assertThat(context.getImportTypeTable().getImports()).containsKey("time");
    assertThat(context.getImportTypeTable().getImports())
        .doesNotContainKey("cloud.google.com/go/longrunning");
  }

  @Test
  public void testGetImportsPageStream() {
    MethodModel method =
        new ProtoMethodModel(getMethod(context.getInterface(), "PageStreamMethod"));
    transformer.addXApiImports(context, Collections.singletonList(method));
    transformer.generateRetryConfigDefinitions(context, Collections.singletonList(method));
    assertThat(context.getImportTypeTable().getImports()).containsKey("math");
    assertThat(context.getImportTypeTable().getImports())
        .doesNotContainKey("cloud.google.com/go/longrunning");
  }

  @Test
  public void testGetImportsLro() {
    MethodModel method = new ProtoMethodModel(getMethod(context.getInterface(), "LroMethod"));
    transformer.addXApiImports(context, Collections.singletonList(method));
    transformer.generateRetryConfigDefinitions(context, Collections.singletonList(method));
    assertThat(context.getImportTypeTable().getImports()).doesNotContainKey("math");
    assertThat(context.getImportTypeTable().getImports())
        .containsKey("cloud.google.com/go/longrunning");
  }

  @Test
  public void testGetImportsNotLro() {
    MethodModel method = new ProtoMethodModel(getMethod(context.getInterface(), "NotLroMethod"));
    transformer.addXApiImports(context, Collections.singletonList(method));
    assertThat(context.getImportTypeTable().getImports())
        .doesNotContainKey("cloud.google.com/go/longrunning");
  }

  @Test
  public void testGetExampleImportsServerStream() {
    MethodModel method =
        new ProtoMethodModel(getMethod(context.getInterface(), "ServerStreamMethod"));
    transformer.addXExampleImports(context, Collections.singletonList(method));
    assertThat(context.getImportTypeTable().getImports()).containsKey("io");
  }

  @Test
  public void testGetExampleImportsBidiStream() {
    MethodModel method =
        new ProtoMethodModel(getMethod(context.getInterface(), "BidiStreamMethod"));
    transformer.addXExampleImports(context, Collections.singletonList(method));
    assertThat(context.getImportTypeTable().getImports()).containsKey("io");
  }

  @Test
  public void testGetExampleImportsClientStream() {
    MethodModel method =
        new ProtoMethodModel(getMethod(context.getInterface(), "ClientStreamMethod"));
    transformer.addXExampleImports(context, Collections.singletonList(method));
    assertThat(context.getImportTypeTable().getImports()).doesNotContainKey("io");
  }

  @Test
  public void testExampleImports() {
    transformer.addXExampleImports(context, context.getSupportedMethods());
    assertThat(context.getImportTypeTable().getImports())
        .containsEntry("context", TypeAlias.create("context", ""));
    assertThat(context.getImportTypeTable().getImports())
        .containsEntry(
            "cloud.google.com/go/gopher/apiv1",
            TypeAlias.create("cloud.google.com/go/gopher/apiv1", "gopher"));
    assertThat(context.getImportTypeTable().getImports())
        .containsEntry(
            "google.golang.org/genproto/googleapis/example/myproto/v1",
            TypeAlias.create(
                "google.golang.org/genproto/googleapis/example/myproto/v1", "myprotopb"));

    // Only shows up in response, not needed for example.
    assertThat(context.getImportTypeTable().getImports())
        .doesNotContainKey("google.golang.org/genproto/googleapis/example/odd/v1");
  }

  private Method getMethod(Interface apiInterface, String methodName) {
    Method method = apiInterface.lookupMethod(methodName);
    if (method == null) {
      throw new IllegalArgumentException(
          String.format(
              "Method %s not found, available: %s", methodName, apiInterface.getMethods()));
    }

    return method;
  }
}

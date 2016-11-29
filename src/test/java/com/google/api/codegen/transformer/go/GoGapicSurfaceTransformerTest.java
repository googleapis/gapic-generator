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

import com.google.api.codegen.CodegenTestUtil;
import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.gapic.PackageNameCodePathMapper;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.testing.TestDataLocator;
import com.google.common.truth.Truth;
import java.util.Collections;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GoGapicSurfaceTransformerTest {

  @ClassRule public static TemporaryFolder tempDir = new TemporaryFolder();

  private static Model model;
  private static Interface service;
  private static ApiConfig apiConfig;

  @BeforeClass
  public static void setupClass() {
    TestDataLocator locator = TestDataLocator.create(GoGapicSurfaceTransformerTest.class);
    model =
        CodegenTestUtil.readModel(
            locator,
            tempDir,
            new String[] {"myproto.proto", "singleservice.proto"},
            new String[] {"myproto.yaml"});
    for (Interface serv : model.getSymbolTable().getInterfaces()) {
      if (serv.getSimpleName().equals("Gopher")) {
        service = serv;
        break;
      }
    }

    ConfigProto configProto =
        CodegenTestUtil.readConfig(
            model.getDiagCollector(), locator, new String[] {"myproto_gapic.yaml"});

    apiConfig = ApiConfig.createApiConfig(model, configProto);

    if (model.getDiagCollector().hasErrors()) {
      throw new IllegalStateException(model.getDiagCollector().getDiags().toString());
    }
  }

  private final GoGapicSurfaceTransformer transformer =
      new GoGapicSurfaceTransformer(new PackageNameCodePathMapper());
  private SurfaceTransformerContext context;

  @Before
  public void setup() {
    GoSurfaceNamer namer = new GoSurfaceNamer(apiConfig.getPackageName());
    context =
        SurfaceTransformerContext.create(
            service,
            apiConfig,
            GoGapicSurfaceTransformer.createTypeTable(),
            namer,
            new GoFeatureConfig());
  }

  @Test
  public void testGetImportsPlain() {
    Method method = getMethod(context.getInterface(), "SimpleMethod");
    transformer.addXApiImports(context, Collections.singletonList(method));
    transformer.generateRetryConfigDefinitions(context, Collections.singletonList(method));
    Truth.assertThat(context.getTypeTable().getImports()).doesNotContainKey("time");
    Truth.assertThat(context.getTypeTable().getImports()).doesNotContainKey("longrunning");
  }

  @Test
  public void testGetImportsRetry() {
    Method method = getMethod(context.getInterface(), "RetryMethod");
    transformer.addXApiImports(context, Collections.singletonList(method));
    transformer.generateRetryConfigDefinitions(context, Collections.singletonList(method));
    Truth.assertThat(context.getTypeTable().getImports()).containsKey("time");
    Truth.assertThat(context.getTypeTable().getImports()).doesNotContainKey("longrunning");
  }

  @Test
  public void testGetImportsPageStream() {
    Method method = getMethod(context.getInterface(), "PageStreamMethod");
    transformer.addXApiImports(context, Collections.singletonList(method));
    transformer.generateRetryConfigDefinitions(context, Collections.singletonList(method));
    Truth.assertThat(context.getTypeTable().getImports()).containsKey("math");
    Truth.assertThat(context.getTypeTable().getImports()).doesNotContainKey("longrunning");
  }

  @Test
  public void testGetImportsLro() {
    Method method = getMethod(context.getInterface(), "LroMethod");
    transformer.addXApiImports(context, Collections.singletonList(method));
    transformer.generateRetryConfigDefinitions(context, Collections.singletonList(method));
    Truth.assertThat(context.getTypeTable().getImports()).doesNotContainKey("math");
    Truth.assertThat(context.getTypeTable().getImports())
        .containsKey("cloud.google.com/go/longrunning");
  }

  @Test
  public void testGetExampleImportsServerStream() {
    Method method = getMethod(context.getInterface(), "ServerStreamMethod");
    transformer.addXExampleImports(context, Collections.singletonList(method));
    Truth.assertThat(context.getTypeTable().getImports()).containsKey("io");
  }

  @Test
  public void testGetExampleImportsBidiStream() {
    Method method = getMethod(context.getInterface(), "BidiStreamMethod");
    transformer.addXExampleImports(context, Collections.singletonList(method));
    Truth.assertThat(context.getTypeTable().getImports()).containsKey("io");
  }

  @Test
  public void testGetExampleImportsClientStream() {
    Method method = getMethod(context.getInterface(), "ClientStreamMethod");
    transformer.addXExampleImports(context, Collections.singletonList(method));
    Truth.assertThat(context.getTypeTable().getImports()).doesNotContainKey("io");
  }

  @Test
  public void testExampleImports() {
    transformer.addXExampleImports(context, context.getSupportedMethods());
    Truth.assertThat(context.getTypeTable().getImports())
        .containsEntry(
            "golang.org/x/net/context", TypeAlias.create("golang.org/x/net/context", ""));
    Truth.assertThat(context.getTypeTable().getImports())
        .containsEntry(
            "cloud.google.com/go/gopher/apiv1",
            TypeAlias.create("cloud.google.com/go/gopher/apiv1", ""));
    Truth.assertThat(context.getTypeTable().getImports())
        .containsEntry(
            "google.golang.org/genproto/googleapis/example/myproto/v1",
            TypeAlias.create(
                "google.golang.org/genproto/googleapis/example/myproto/v1", "myprotopb"));

    // Only shows up in response, not needed for example.
    Truth.assertThat(context.getTypeTable().getImports())
        .doesNotContainKey("google.golang.org/genproto/googleapis/example/odd/v1");
  }

  private Method getMethod(Interface service, String methodName) {
    for (Method method : service.getMethods()) {
      String name = method.getFullName();
      int dot = name.lastIndexOf('.');
      if (dot >= 0) {
        name = name.substring(dot + 1);
      }
      if (name.equals(methodName)) {
        return method;
      }
    }
    throw new IllegalArgumentException(
        String.format("Method %s not found, available: %s", methodName, service.getMethods()));
  }
}

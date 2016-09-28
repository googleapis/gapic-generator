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
import com.google.api.codegen.gapic.PackageNameCodePathMapper;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.testing.TestDataLocator;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.junit.Test;

import java.util.Collections;

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
            locator, tempDir, new String[] {"myproto.proto"}, new String[] {"myproto.yaml"});
    service = model.getSymbolTable().getInterfaces().asList().get(0);

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
            new FeatureConfig());
  }

  private static final ImmutableList<String> PAGE_STREAM_IMPORTS =
      ImmutableList.<String>of("math;;;");

  @Test
  public void testGetImportsPlain() {
    Method method = getMethod(context.getInterface(), "SimpleMethod");
    transformer.generateApiMethods(context, Collections.singletonList(method), PAGE_STREAM_IMPORTS);
    transformer.generateRetryConfigDefinitions(context, Collections.singletonList(method));
    Truth.assertThat(context.getTypeTable().getImports()).doesNotContainKey("time");
  }

  @Test
  public void testGetImportsRetry() {
    Method method = getMethod(context.getInterface(), "RetryMethod");
    transformer.generateApiMethods(context, Collections.singletonList(method), PAGE_STREAM_IMPORTS);
    transformer.generateRetryConfigDefinitions(context, Collections.singletonList(method));
    Truth.assertThat(context.getTypeTable().getImports()).containsKey("time");
  }

  @Test
  public void testGetImportsPageStream() {
    Method method = getMethod(context.getInterface(), "PageStreamMethod");
    transformer.generateApiMethods(context, Collections.singletonList(method), PAGE_STREAM_IMPORTS);
    transformer.generateRetryConfigDefinitions(context, Collections.singletonList(method));
    Truth.assertThat(context.getTypeTable().getImports()).containsKey("math");
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

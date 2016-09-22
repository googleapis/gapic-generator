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
import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.gapic.PackageNameCodePathMapper;
import com.google.api.codegen.InterfaceConfig;
import com.google.api.codegen.MultiYamlReader;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.model.testing.TestConfig;
import com.google.api.tools.framework.model.testing.TestDataLocator;
import com.google.api.tools.framework.setup.StandardSetup;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.truth.Truth;
import com.google.protobuf.Message;

import io.grpc.Status;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GoGapicSurfaceTransformerTest {

  @ClassRule public static TemporaryFolder tempDir = new TemporaryFolder();

  private static Model model;
  private static Interface service;
  private static ApiConfig apiConfig;

  @BeforeClass
  public static void setupClass() {
    List<String> protoFiles = Collections.singletonList("myproto.proto");
    List<String> yamlFiles = Collections.singletonList("myproto.yaml");
    TestDataLocator testDataLocator = TestDataLocator.create(GoGapicSurfaceTransformerTest.class);
    TestConfig testConfig =
        new TestConfig(testDataLocator, tempDir.getRoot().getPath(), protoFiles);
    model = testConfig.createModel(yamlFiles);
    StandardSetup.registerStandardProcessors(model);
    StandardSetup.registerStandardConfigAspects(model);
    model.establishStage(Merged.KEY);
    service = model.getSymbolTable().getInterfaces().asList().get(0);

    List<String> gapicConfigFiles = Collections.singletonList("myproto_gapic.yaml");
    List<String> gapicConfigContents = new ArrayList<>();
    for (String fileName : gapicConfigFiles) {
      gapicConfigContents.add(testDataLocator.readTestData(testDataLocator.findTestData(fileName)));
    }

    ImmutableMap<String, Message> supportedConfigTypes =
        ImmutableMap.<String, Message>of(
            ConfigProto.getDescriptor().getFullName(), ConfigProto.getDefaultInstance());
    ConfigProto configProto =
        (ConfigProto)
            MultiYamlReader.read(
                model.getDiagCollector(),
                gapicConfigFiles,
                gapicConfigContents,
                supportedConfigTypes);
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
    GoSurfaceNamer namer = new GoSurfaceNamer(model, apiConfig.getPackageName());
    context =
        SurfaceTransformerContext.create(
            service, apiConfig, GoGapicSurfaceTransformer.createTypeTable(), namer);
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

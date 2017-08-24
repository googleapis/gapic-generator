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
package com.google.api.codegen.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.Service;
import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.discovery.config.TypeNameGenerator;
import com.google.api.codegen.discovery.config.csharp.CSharpTypeNameGenerator;
import com.google.api.codegen.discovery.config.go.GoTypeNameGenerator;
import com.google.api.codegen.discovery.config.java.JavaTypeNameGenerator;
import com.google.api.codegen.discovery.config.js.JSTypeNameGenerator;
import com.google.api.codegen.discovery.config.nodejs.NodeJSTypeNameGenerator;
import com.google.api.codegen.discovery.config.php.PhpTypeNameGenerator;
import com.google.api.codegen.discovery.config.py.PythonTypeNameGenerator;
import com.google.api.codegen.discovery.config.ruby.RubyTypeNameGenerator;
import com.google.api.codegen.discovery.transformer.SampleMethodToViewTransformer;
import com.google.api.codegen.discovery.transformer.csharp.CSharpSampleMethodToViewTransformer;
import com.google.api.codegen.discovery.transformer.go.GoSampleMethodToViewTransformer;
import com.google.api.codegen.discovery.transformer.java.JavaSampleMethodToViewTransformer;
import com.google.api.codegen.discovery.transformer.js.JSSampleMethodToViewTransformer;
import com.google.api.codegen.discovery.transformer.nodejs.NodeJSSampleMethodToViewTransformer;
import com.google.api.codegen.discovery.transformer.php.PhpSampleMethodToViewTransformer;
import com.google.api.codegen.discovery.transformer.py.PythonSampleMethodToViewTransformer;
import com.google.api.codegen.discovery.transformer.ruby.RubySampleMethodToViewTransformer;
import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.lang3.NotImplementedException;

/*
 * Creates DiscoveryProvider instances based on an ID.
 */
public class MainDiscoveryProviderFactory implements DiscoveryProviderFactory {

  public static final String CSHARP = "csharp";
  public static final String GO = "go";
  public static final String JAVA = "java";
  public static final String JS = "js";
  public static final String NODEJS = "nodejs";
  public static final String PHP = "php";
  public static final String PYTHON = "python";
  public static final String RUBY = "ruby";

  private static final Map<String, Class<? extends SampleMethodToViewTransformer>>
      SAMPLE_METHOD_TO_VIEW_TRANSFORMER_MAP =
          ImmutableMap.<String, Class<? extends SampleMethodToViewTransformer>>builder()
              .put(CSHARP, CSharpSampleMethodToViewTransformer.class)
              .put(GO, GoSampleMethodToViewTransformer.class)
              .put(JAVA, JavaSampleMethodToViewTransformer.class)
              .put(JS, JSSampleMethodToViewTransformer.class)
              .put(NODEJS, NodeJSSampleMethodToViewTransformer.class)
              .put(PHP, PhpSampleMethodToViewTransformer.class)
              .put(PYTHON, PythonSampleMethodToViewTransformer.class)
              .put(RUBY, RubySampleMethodToViewTransformer.class)
              .build();
  private static final Map<String, Class<? extends TypeNameGenerator>> TYPE_NAME_GENERATOR_MAP =
      ImmutableMap.<String, Class<? extends TypeNameGenerator>>builder()
          .put(CSHARP, CSharpTypeNameGenerator.class)
          .put(GO, GoTypeNameGenerator.class)
          .put(JAVA, JavaTypeNameGenerator.class)
          .put(JS, JSTypeNameGenerator.class)
          .put(NODEJS, NodeJSTypeNameGenerator.class)
          .put(PHP, PhpTypeNameGenerator.class)
          .put(PYTHON, PythonTypeNameGenerator.class)
          .build();

  public static DiscoveryProvider defaultCreate(
      Service service,
      ApiaryConfig apiaryConfig,
      List<JsonNode> sampleConfigOverrides,
      String rubyNamesFile,
      String id) {
    // Use nodes corresponding to language pattern fields matching current language.
    List<JsonNode> overrides = new ArrayList<JsonNode>();
    // Sort patterns to ensure deterministic ordering of overrides
    for (JsonNode override : sampleConfigOverrides) {
      List<String> languagePatterns = Lists.newArrayList(override.fieldNames());
      Collections.sort(languagePatterns);
      for (String languagePattern : languagePatterns) {
        if (Pattern.matches(languagePattern, id)) {
          overrides.add(override.get(languagePattern));
        }
      }
    }

    SampleMethodToViewTransformer sampleMethodToViewTransformer = null;
    TypeNameGenerator typeNameGenerator = null;
    try {
      sampleMethodToViewTransformer = SAMPLE_METHOD_TO_VIEW_TRANSFORMER_MAP.get(id).newInstance();
      if (id.equals(RUBY)) {
        typeNameGenerator = new RubyTypeNameGenerator(rubyNamesFile);
      } else {
        typeNameGenerator = TYPE_NAME_GENERATOR_MAP.get(id).newInstance();
      }
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    if (sampleMethodToViewTransformer == null || typeNameGenerator == null) {
      throw new NotImplementedException("MainDiscoveryProviderFactory: invalid id \"" + id + "\"");
    }

    return ViewModelProvider.newBuilder()
        .setMethods(service.getApis(0).getMethodsList())
        .setApiaryConfig(apiaryConfig)
        .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
        .setMethodToViewTransformer(sampleMethodToViewTransformer)
        .setOverrides(overrides)
        .setTypeNameGenerator(typeNameGenerator)
        .setOutputRoot(
            "autogenerated/"
                + apiaryConfig.getApiName()
                + "/"
                + apiaryConfig.getApiVersion()
                + "/"
                + service.getDocumentation().getOverview())
        .build();
  }

  @Override
  public DiscoveryProvider create(
      Service service,
      ApiaryConfig apiaryConfig,
      List<JsonNode> sampleConfigOverrides,
      String rubyNamesFile,
      String id) {
    return defaultCreate(service, apiaryConfig, sampleConfigOverrides, rubyNamesFile, id);
  }
}

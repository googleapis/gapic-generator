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
package com.google.api.codegen.gapic;

import static com.google.api.codegen.common.TargetLanguage.CSHARP;
import static com.google.api.codegen.common.TargetLanguage.GO;
import static com.google.api.codegen.common.TargetLanguage.JAVA;
import static com.google.api.codegen.common.TargetLanguage.NODEJS;
import static com.google.api.codegen.common.TargetLanguage.PHP;
import static com.google.api.codegen.common.TargetLanguage.PYTHON;
import static com.google.api.codegen.common.TargetLanguage.RUBY;

import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.clientconfig.ClientConfigGapicContext;
import com.google.api.codegen.clientconfig.ClientConfigSnippetSetRunner;
import com.google.api.codegen.clientconfig.php.PhpClientConfigGapicContext;
import com.google.api.codegen.common.CodeGenerator;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.nodejs.NodeJSCodePathMapper;
import com.google.api.codegen.php.PhpGapicCodePathMapper;
import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.codegen.transformer.csharp.CSharpGapicClientTransformer;
import com.google.api.codegen.transformer.csharp.CSharpGapicSmokeTestTransformer;
import com.google.api.codegen.transformer.csharp.CSharpGapicSnippetsTransformer;
import com.google.api.codegen.transformer.csharp.CSharpGapicUnitTestTransformer;
import com.google.api.codegen.transformer.go.GoGapicSurfaceTestTransformer;
import com.google.api.codegen.transformer.go.GoGapicSurfaceTransformer;
import com.google.api.codegen.transformer.java.JavaGapicPackageTransformer;
import com.google.api.codegen.transformer.java.JavaGapicSamplesTransformer;
import com.google.api.codegen.transformer.java.JavaGapicSurfaceTransformer;
import com.google.api.codegen.transformer.java.JavaSurfaceTestTransformer;
import com.google.api.codegen.transformer.nodejs.NodeJSGapicSamplesTransformer;
import com.google.api.codegen.transformer.nodejs.NodeJSGapicSurfaceDocTransformer;
import com.google.api.codegen.transformer.nodejs.NodeJSGapicSurfaceTestTransformer;
import com.google.api.codegen.transformer.nodejs.NodeJSGapicSurfaceTransformer;
import com.google.api.codegen.transformer.nodejs.NodeJSPackageMetadataTransformer;
import com.google.api.codegen.transformer.php.PhpGapicSurfaceTestTransformer;
import com.google.api.codegen.transformer.php.PhpGapicSurfaceTransformer;
import com.google.api.codegen.transformer.php.PhpPackageMetadataTransformer;
import com.google.api.codegen.transformer.py.PythonGapicSamplesTransformer;
import com.google.api.codegen.transformer.py.PythonGapicSurfaceTestTransformer;
import com.google.api.codegen.transformer.py.PythonGapicSurfaceTransformer;
import com.google.api.codegen.transformer.py.PythonPackageMetadataTransformer;
import com.google.api.codegen.transformer.ruby.RubyGapicSurfaceDocTransformer;
import com.google.api.codegen.transformer.ruby.RubyGapicSurfaceTestTransformer;
import com.google.api.codegen.transformer.ruby.RubyGapicSurfaceTransformer;
import com.google.api.codegen.transformer.ruby.RubyPackageMetadataTransformer;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.csharp.CSharpNameFormatter;
import com.google.api.codegen.util.csharp.CSharpRenderingUtil;
import com.google.api.codegen.util.java.JavaRenderingUtil;
import com.google.api.codegen.util.py.PythonRenderingUtil;
import com.google.api.codegen.util.ruby.RubyNameFormatter;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** GapicProviderFactory creates CodeGenerator instances based on an id. */
public class GapicProviderFactory {
  public static final String ARTIFACT_SURFACE = "surface";
  public static final String ARTIFACT_TEST = "test";
  public static final String ARTIFACT_SAMPLES = "samples";

  public static boolean enableSurfaceGenerator(List<String> enabledArtifacts) {
    return enabledArtifacts.isEmpty() || enabledArtifacts.contains(ARTIFACT_SURFACE);
  }

  public static boolean enableTestGenerator(List<String> enabledArtifacts) {
    return enabledArtifacts.isEmpty() || enabledArtifacts.contains(ARTIFACT_TEST);
  }

  public static boolean enableSampleGenerator(List<String> enabledArtifacts) {
    return enabledArtifacts.contains(ARTIFACT_SAMPLES);
  }

  /** Create the GapicProviders based on the given id */
  public static List<CodeGenerator<?>> create(
      TargetLanguage language,
      Model model,
      GapicProductConfig productConfig,
      PackageMetadataConfig packageConfig,
      List<String> enabledArtifacts) {

    ArrayList<CodeGenerator<?>> providers = new ArrayList<>();
    // Please keep the following IDs in alphabetical order
    if (language.equals(CSHARP)) {
      String packageName = productConfig.getPackageName();
      if (enableSurfaceGenerator(enabledArtifacts)) {
        GapicCodePathMapper pathMapper =
            CommonGapicCodePathMapper.newBuilder()
                .setPrefix(packageName + File.separator + packageName)
                .setPackageFilePathNameFormatter(new CSharpNameFormatter())
                .build();
        CodeGenerator mainProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CSharpRenderingUtil()))
                .setModelToViewTransformer(
                    new CSharpGapicClientTransformer(pathMapper, packageConfig))
                .build();
        providers.add(mainProvider);
        GapicCodePathMapper snippetPathMapper =
            CommonGapicCodePathMapper.newBuilder()
                .setPrefix(packageName + File.separator + packageName + ".Snippets")
                .setPackageFilePathNameFormatter(new CSharpNameFormatter())
                .build();
        CodeGenerator snippetProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CSharpRenderingUtil()))
                .setModelToViewTransformer(new CSharpGapicSnippetsTransformer(snippetPathMapper))
                .build();
        providers.add(snippetProvider);
      }
      if (enableTestGenerator(enabledArtifacts)) {
        GapicCodePathMapper smokeTestPathMapper =
            CommonGapicCodePathMapper.newBuilder()
                .setPrefix(packageName + File.separator + packageName + ".SmokeTests")
                .setPackageFilePathNameFormatter(new CSharpNameFormatter())
                .build();
        CodeGenerator smokeTestProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CSharpRenderingUtil()))
                .setModelToViewTransformer(new CSharpGapicSmokeTestTransformer(smokeTestPathMapper))
                .build();
        providers.add(smokeTestProvider);
        GapicCodePathMapper unitTestPathMapper =
            CommonGapicCodePathMapper.newBuilder()
                .setPrefix(packageName + File.separator + packageName + ".Tests")
                .setPackageFilePathNameFormatter(new CSharpNameFormatter())
                .build();
        CodeGenerator unitTestProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CSharpRenderingUtil()))
                .setModelToViewTransformer(new CSharpGapicUnitTestTransformer(unitTestPathMapper))
                .build();
        providers.add(unitTestProvider);
      }

    } else if (language.equals(GO)) {
      if (enableSurfaceGenerator(enabledArtifacts)) {
        CodeGenerator provider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(
                    new GoGapicSurfaceTransformer(new PackageNameCodePathMapper()))
                .build();
        providers.add(provider);
      }
      if (enableTestGenerator(enabledArtifacts)) {
        CodeGenerator testProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(new GoGapicSurfaceTestTransformer())
                .build();
        providers.add(testProvider);
      }

    } else if (language.equals(JAVA)) {
      if (enableSurfaceGenerator(enabledArtifacts)) {
        GapicCodePathMapper javaPathMapper =
            CommonGapicCodePathMapper.newBuilder()
                .setPrefix("src/main/java")
                .setShouldAppendPackage(true)
                .build();
        CodeGenerator mainProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new JavaRenderingUtil()))
                .setModelToViewTransformer(
                    new JavaGapicSurfaceTransformer(javaPathMapper, packageConfig))
                .build();

        providers.add(mainProvider);

        CodeGenerator metadataProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new JavaRenderingUtil()))
                .setModelToViewTransformer(new JavaGapicPackageTransformer(packageConfig))
                .build();
        providers.add(metadataProvider);

        CodeGenerator staticResourcesProvider =
            new StaticResourcesProvider(
                ImmutableMap.<String, String>builder()
                    .put("java/static/build.gradle", "../build.gradle")
                    .put("java/static/settings.gradle", "../settings.gradle")
                    .put("java/static/gradlew", "../gradlew")
                    .put("java/static/gradlew.bat", "../gradlew.bat")
                    .put(
                        "java/static/gradle/wrapper/gradle-wrapper.jar",
                        "../gradle/wrapper/gradle-wrapper.jar")
                    .put(
                        "java/static/gradle/wrapper/gradle-wrapper.properties",
                        "../gradle/wrapper/gradle-wrapper.properties")
                    .build(),
                ImmutableSet.of("../gradlew"));
        providers.add(staticResourcesProvider);

        CodeGenerator sampleProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new JavaRenderingUtil()))
                .setModelToViewTransformer(new JavaGapicSamplesTransformer(javaPathMapper))
                .build();
        providers.add(sampleProvider);
      }
      if (enableTestGenerator(enabledArtifacts)) {
        GapicCodePathMapper javaTestPathMapper =
            CommonGapicCodePathMapper.newBuilder()
                .setPrefix("src/test/java")
                .setShouldAppendPackage(true)
                .build();
        CodeGenerator testProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(
                    new JavaSurfaceTestTransformer(
                        javaTestPathMapper,
                        new JavaGapicSurfaceTransformer(javaTestPathMapper, packageConfig),
                        "java/grpc_test.snip"))
                .build();
        providers.add(testProvider);
      }
      return providers;

    } else if (language.equals(NODEJS)) {
      if (enableSurfaceGenerator(enabledArtifacts)) {
        GapicCodePathMapper nodeJSPathMapper = new NodeJSCodePathMapper();
        CodeGenerator mainProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(
                    new NodeJSGapicSurfaceTransformer(nodeJSPathMapper, packageConfig))
                .build();

        CodeGenerator metadataProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(new NodeJSPackageMetadataTransformer(packageConfig))
                .build();
        CodeGenerator clientConfigProvider =
            CommonGapicProvider.<Interface>newBuilder()
                .setModel(model)
                .setContext(new ClientConfigGapicContext(model, productConfig))
                .setSnippetSetRunner(
                    new ClientConfigSnippetSetRunner<>(SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                .setSnippetFileNames(Arrays.asList("clientconfig/json.snip"))
                .setCodePathMapper(nodeJSPathMapper)
                .build();

        providers.add(mainProvider);
        providers.add(metadataProvider);
        providers.add(clientConfigProvider);

        if (enableSampleGenerator(enabledArtifacts)) {
          CodeGenerator sampleProvider =
              ViewModelGapicProvider.newBuilder()
                  .setModel(model)
                  .setProductConfig(productConfig)
                  .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                  .setModelToViewTransformer(
                      new NodeJSGapicSamplesTransformer(nodeJSPathMapper, packageConfig))
                  .build();
          providers.add(sampleProvider);
        }

        CodeGenerator messageProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(new NodeJSGapicSurfaceDocTransformer())
                .build();
        providers.add(messageProvider);
      }

      if (enableTestGenerator(enabledArtifacts)) {
        CodeGenerator testProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(new NodeJSGapicSurfaceTestTransformer())
                .build();
        providers.add(testProvider);
      }

    } else if (language.equals(PHP)) {
      if (enableSurfaceGenerator(enabledArtifacts)) {
        GapicCodePathMapper phpPathMapper =
            PhpGapicCodePathMapper.newBuilder().setPrefix("src").build();
        CodeGenerator provider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(
                    new PhpGapicSurfaceTransformer(productConfig, phpPathMapper, model))
                .build();

        GapicCodePathMapper phpClientConfigPathMapper =
            PhpGapicCodePathMapper.newBuilder().setPrefix("src").setSuffix("resources").build();
        CodeGenerator clientConfigProvider =
            CommonGapicProvider.<Interface>newBuilder()
                .setModel(model)
                .setContext(new PhpClientConfigGapicContext(model, productConfig))
                .setSnippetSetRunner(
                    new ClientConfigSnippetSetRunner<>(SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                .setSnippetFileNames(Arrays.asList("clientconfig/json.snip"))
                .setCodePathMapper(phpClientConfigPathMapper)
                .build();

        CodeGenerator metadataProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(new PhpPackageMetadataTransformer(packageConfig))
                .build();

        providers.add(provider);
        providers.add(clientConfigProvider);
        providers.add(metadataProvider);
      }
      if (enableTestGenerator(enabledArtifacts)) {
        CodeGenerator testProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(new PhpGapicSurfaceTestTransformer(packageConfig))
                .build();
        providers.add(testProvider);
      }

    } else if (language.equals(PYTHON)) {
      if (enableSurfaceGenerator(enabledArtifacts)) {
        GapicCodePathMapper pythonPathMapper =
            CommonGapicCodePathMapper.newBuilder().setShouldAppendPackage(true).build();
        CodeGenerator mainProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new PythonRenderingUtil()))
                .setModelToViewTransformer(
                    new PythonGapicSurfaceTransformer(pythonPathMapper, packageConfig))
                .build();
        CodeGenerator sampleProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new PythonRenderingUtil()))
                .setModelToViewTransformer(
                    new PythonGapicSamplesTransformer(pythonPathMapper, packageConfig))
                .build();
        CodeGenerator clientConfigProvider =
            CommonGapicProvider.<Interface>newBuilder()
                .setModel(model)
                .setContext(new ClientConfigGapicContext(model, productConfig))
                .setSnippetSetRunner(
                    new ClientConfigSnippetSetRunner<>(SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                .setSnippetFileNames(Arrays.asList("clientconfig/python_clientconfig.snip"))
                .setCodePathMapper(pythonPathMapper)
                .build();
        providers.add(mainProvider);
        providers.add(sampleProvider);
        providers.add(clientConfigProvider);

        CodeGenerator metadataProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new PythonRenderingUtil()))
                .setModelToViewTransformer(new PythonPackageMetadataTransformer(packageConfig))
                .build();
        providers.add(metadataProvider);
      }
      if (enableTestGenerator(enabledArtifacts)) {
        GapicCodePathMapper pythonTestPathMapper =
            CommonGapicCodePathMapper.newBuilder()
                .setPrefix("test")
                .setShouldAppendPackage(true)
                .build();
        CodeGenerator testProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(
                    new PythonGapicSurfaceTestTransformer(pythonTestPathMapper, packageConfig))
                .build();
        providers.add(testProvider);
      }

    } else if (language.equals(RUBY)) {
      if (enableSurfaceGenerator(enabledArtifacts)) {
        GapicCodePathMapper rubyPathMapper =
            CommonGapicCodePathMapper.newBuilder()
                .setPrefix("lib")
                .setShouldAppendPackage(true)
                .setPackageFilePathNameFormatter(new RubyNameFormatter())
                .build();
        CodeGenerator mainProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(
                    new RubyGapicSurfaceTransformer(rubyPathMapper, packageConfig))
                .build();
        CodeGenerator clientConfigProvider =
            CommonGapicProvider.<Interface>newBuilder()
                .setModel(model)
                .setContext(new ClientConfigGapicContext(model, productConfig))
                .setSnippetSetRunner(
                    new ClientConfigSnippetSetRunner<>(SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                .setSnippetFileNames(Arrays.asList("clientconfig/json.snip"))
                .setCodePathMapper(rubyPathMapper)
                .build();
        CodeGenerator metadataProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(new RubyPackageMetadataTransformer(packageConfig))
                .build();

        providers.add(mainProvider);
        providers.add(clientConfigProvider);
        providers.add(metadataProvider);

        CodeGenerator messageProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(
                    new RubyGapicSurfaceDocTransformer(rubyPathMapper, packageConfig))
                .build();
        providers.add(messageProvider);
      }
      if (enableTestGenerator(enabledArtifacts)) {
        CommonGapicCodePathMapper.Builder rubyTestPathMapperBuilder =
            CommonGapicCodePathMapper.newBuilder()
                .setShouldAppendPackage(true)
                .setPackageFilePathNameFormatter(new RubyNameFormatter());
        CodeGenerator testProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(
                    new RubyGapicSurfaceTestTransformer(
                        rubyTestPathMapperBuilder.setPrefix("test").build(),
                        rubyTestPathMapperBuilder.setPrefix("acceptance").build(),
                        packageConfig))
                .build();
        providers.add(testProvider);
      }
    } else {
      throw new UnsupportedOperationException(
          "GapicProviderFactory: unsupported language \"" + language + "\"");
    }

    if (providers.isEmpty()) {
      throw new IllegalArgumentException("No artifacts are enabled.");
    }
    return providers;
  }
}

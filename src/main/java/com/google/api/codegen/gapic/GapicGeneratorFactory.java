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
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.java.JavaGapicCodePathMapper;
import com.google.api.codegen.nodejs.NodeJSCodePathMapper;
import com.google.api.codegen.php.PhpGapicCodePathMapper;
import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.csharp.CSharpBasicPackageTransformer;
import com.google.api.codegen.transformer.csharp.CSharpGapicClientPackageTransformer;
import com.google.api.codegen.transformer.csharp.CSharpGapicClientTransformer;
import com.google.api.codegen.transformer.csharp.CSharpGapicSmokeTestTransformer;
import com.google.api.codegen.transformer.csharp.CSharpGapicSnippetsTransformer;
import com.google.api.codegen.transformer.csharp.CSharpGapicUnitTestTransformer;
import com.google.api.codegen.transformer.go.GoGapicSurfaceTestTransformer;
import com.google.api.codegen.transformer.go.GoGapicSurfaceTransformer;
import com.google.api.codegen.transformer.java.JavaGapicPackageTransformer;
import com.google.api.codegen.transformer.java.JavaGapicSamplesPackageTransformer;
import com.google.api.codegen.transformer.java.JavaGapicSamplesTransformer;
import com.google.api.codegen.transformer.java.JavaGapicSurfaceTransformer;
import com.google.api.codegen.transformer.java.JavaSurfaceTestTransformer;
import com.google.api.codegen.transformer.nodejs.NodeJSGapicSamplesTransformer;
import com.google.api.codegen.transformer.nodejs.NodeJSGapicSurfaceDocTransformer;
import com.google.api.codegen.transformer.nodejs.NodeJSGapicSurfaceTestTransformer;
import com.google.api.codegen.transformer.nodejs.NodeJSGapicSurfaceTransformer;
import com.google.api.codegen.transformer.nodejs.NodeJSPackageMetadataTransformer;
import com.google.api.codegen.transformer.php.PhpGapicSamplesTransformer;
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
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/** GapicGeneratorFactory creates CodeGenerator instances based on an id. */
public class GapicGeneratorFactory {

  /**
   * Create the GapicGenerators based on the given id.
   *
   * <p>Samples are created only in the languages for which they are production-ready.
   */
  public static List<CodeGenerator<?>> create(
      TargetLanguage language,
      Model model,
      GapicProductConfig productConfig,
      PackageMetadataConfig packageConfig,
      ArtifactFlags artifactFlags) {
    return create(language, model, productConfig, packageConfig, artifactFlags, false);
  }

  /**
   * Create the GapicGenerators based on the given id.
   *
   * <p>If {@code devSamples} is set, standalone samples are generated for all languages, including
   * those that currently have development stubs. Otherwise, samples are only produced in languages
   * where they are production-ready.
   */
  public static List<CodeGenerator<?>> create(
      TargetLanguage language,
      Model model,
      GapicProductConfig productConfig,
      PackageMetadataConfig packageConfig,
      ArtifactFlags artifactFlags,
      boolean devSamples) {

    ArrayList<CodeGenerator<?>> generators = new ArrayList<>();
    // Please keep the following IDs in alphabetical order
    if (language.equals(CSHARP)) {
      String packageName = productConfig.getPackageName();

      Function<String, GapicCodePathMapper> newCodePathMapper =
          suffix ->
              CommonGapicCodePathMapper.newBuilder()
                  .setPrefix(packageName + File.separator + packageName + suffix)
                  .setPackageFilePathNameFormatter(new CSharpNameFormatter())
                  .build();

      Function<ModelToViewTransformer<ProtoApiModel>, CodeGenerator> newCsharpGenerator =
          transformer ->
              GapicGenerator.newBuilder()
                  .setModel(model)
                  .setProductConfig(productConfig)
                  .setSnippetSetRunner(new CommonSnippetSetRunner(new CSharpRenderingUtil()))
                  .setModelToViewTransformer(transformer)
                  .build();

      if (artifactFlags.surfaceGeneratorEnabled()) {
        GapicCodePathMapper clientPathMapper = newCodePathMapper.apply("");
        if (artifactFlags.codeFilesEnabled()) {
          generators.add(
              newCsharpGenerator.apply(new CSharpGapicClientTransformer(clientPathMapper)));
        }

        if (artifactFlags.packagingFilesEnabled()) {
          generators.add(
              newCsharpGenerator.apply(
                  new CSharpGapicClientPackageTransformer(clientPathMapper, packageConfig)));
        }

        GapicCodePathMapper snippetPathMapper = newCodePathMapper.apply(".Snippets");
        if (artifactFlags.codeFilesEnabled()) {
          generators.add(
              newCsharpGenerator.apply(new CSharpGapicSnippetsTransformer(snippetPathMapper)));
        }

        if (artifactFlags.packagingFilesEnabled()) {
          generators.add(
              newCsharpGenerator.apply(
                  CSharpBasicPackageTransformer.forSnippets(snippetPathMapper)));
        }
      }
      if (artifactFlags.testGeneratorEnabled()) {
        GapicCodePathMapper smokeTestPathMapper = newCodePathMapper.apply(".SmokeTests");
        if (artifactFlags.codeFilesEnabled()) {
          generators.add(
              newCsharpGenerator.apply(new CSharpGapicSmokeTestTransformer(smokeTestPathMapper)));
        }

        if (artifactFlags.packagingFilesEnabled()) {
          generators.add(
              newCsharpGenerator.apply(
                  CSharpBasicPackageTransformer.forSmokeTests(smokeTestPathMapper)));
        }

        GapicCodePathMapper unitTestPathMapper = newCodePathMapper.apply(".Tests");
        if (artifactFlags.codeFilesEnabled()) {
          generators.add(
              newCsharpGenerator.apply(new CSharpGapicUnitTestTransformer(unitTestPathMapper)));
        }

        if (artifactFlags.packagingFilesEnabled()) {
          generators.add(
              newCsharpGenerator.apply(
                  CSharpBasicPackageTransformer.forUnitTests(unitTestPathMapper)));
        }
      }

    } else if (language.equals(GO)) {
      if (artifactFlags.surfaceGeneratorEnabled()) {
        CodeGenerator generator =
            GapicGenerator.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(
                    new GoGapicSurfaceTransformer(new PackageNameCodePathMapper()))
                .build();
        generators.add(generator);
      }
      if (artifactFlags.testGeneratorEnabled()) {
        CodeGenerator testGenerator =
            GapicGenerator.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(new GoGapicSurfaceTestTransformer())
                .build();
        generators.add(testGenerator);
      }

    } else if (language.equals(JAVA)) {
      Function<ModelToViewTransformer<ProtoApiModel>, CodeGenerator> newJavaGenerator =
          transformer ->
              GapicGenerator.newBuilder()
                  .setModel(model)
                  .setProductConfig(productConfig)
                  .setSnippetSetRunner(new CommonSnippetSetRunner(new JavaRenderingUtil()))
                  .setModelToViewTransformer(transformer)
                  .build();

      if (artifactFlags.surfaceGeneratorEnabled()) {
        GapicCodePathMapper javaPathMapper =
            JavaGapicCodePathMapper.newBuilder().prefix("src/main/java").build();

        if (artifactFlags.codeFilesEnabled()) {
          generators.add(newJavaGenerator.apply(new JavaGapicSurfaceTransformer(javaPathMapper)));
          if (devSamples) {
            generators.add(newJavaGenerator.apply(new JavaGapicSamplesTransformer(javaPathMapper)));
            generators.add(
                newJavaGenerator.apply(new JavaGapicSamplesPackageTransformer(packageConfig)));
          }
        }

        if (artifactFlags.packagingFilesEnabled()) {
          generators.add(newJavaGenerator.apply(new JavaGapicPackageTransformer<>(packageConfig)));

          CodeGenerator staticResourcesGenerator =
              new StaticResourcesGenerator(
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
          generators.add(staticResourcesGenerator);
        }
      }

      if (artifactFlags.testGeneratorEnabled()) {
        if (artifactFlags.codeFilesEnabled()) {
          GapicCodePathMapper javaTestPathMapper =
              JavaGapicCodePathMapper.newBuilder().prefix("src/test/java").build();
          generators.add(
              newJavaGenerator.apply(
                  new JavaSurfaceTestTransformer<>(
                      javaTestPathMapper,
                      new JavaGapicSurfaceTransformer(javaTestPathMapper),
                      "java/grpc_test.snip")));
        }
      }
      return generators;

    } else if (language.equals(NODEJS)) {
      if (artifactFlags.surfaceGeneratorEnabled()) {
        GapicCodePathMapper nodeJSPathMapper = new NodeJSCodePathMapper();
        CodeGenerator mainGenerator =
            GapicGenerator.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(
                    new NodeJSGapicSurfaceTransformer(nodeJSPathMapper, packageConfig))
                .build();

        CodeGenerator metadataGenerator =
            GapicGenerator.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(new NodeJSPackageMetadataTransformer(packageConfig))
                .build();
        CodeGenerator clientConfigGenerator =
            LegacyGapicGenerator.newBuilder()
                .setModel(model)
                .setContext(new ClientConfigGapicContext(model, productConfig))
                .setSnippetSetRunner(
                    new ClientConfigSnippetSetRunner<>(SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                .setSnippetFileNames(Arrays.asList("clientconfig/json.snip"))
                .setCodePathMapper(nodeJSPathMapper)
                .build();

        generators.add(mainGenerator);
        generators.add(metadataGenerator);
        generators.add(clientConfigGenerator);

        if (devSamples) {
          CodeGenerator sampleGenerator =
              GapicGenerator.newBuilder()
                  .setModel(model)
                  .setProductConfig(productConfig)
                  .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                  .setModelToViewTransformer(
                      new NodeJSGapicSamplesTransformer(nodeJSPathMapper, packageConfig))
                  .build();
          generators.add(sampleGenerator);
        }

        CodeGenerator messageGenerator =
            GapicGenerator.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(new NodeJSGapicSurfaceDocTransformer())
                .build();
        generators.add(messageGenerator);
      }

      if (artifactFlags.testGeneratorEnabled()) {
        CodeGenerator testGenerator =
            GapicGenerator.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(new NodeJSGapicSurfaceTestTransformer())
                .build();
        generators.add(testGenerator);
      }

    } else if (language.equals(PHP)) {
      if (artifactFlags.surfaceGeneratorEnabled()) {
        GapicCodePathMapper phpPathMapper =
            PhpGapicCodePathMapper.newBuilder().setPrefix("src").build();
        CodeGenerator generator =
            GapicGenerator.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(
                    new PhpGapicSurfaceTransformer(productConfig, phpPathMapper, model))
                .build();

        GapicCodePathMapper phpClientConfigPathMapper =
            PhpGapicCodePathMapper.newBuilder().setPrefix("src").setSuffix("resources").build();
        CodeGenerator clientConfigGenerator =
            LegacyGapicGenerator.newBuilder()
                .setModel(model)
                .setContext(new PhpClientConfigGapicContext(model, productConfig))
                .setSnippetSetRunner(
                    new ClientConfigSnippetSetRunner<>(SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                .setSnippetFileNames(Arrays.asList("clientconfig/json.snip"))
                .setCodePathMapper(phpClientConfigPathMapper)
                .build();

        CodeGenerator metadataGenerator =
            GapicGenerator.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(new PhpPackageMetadataTransformer(packageConfig))
                .build();

        if (devSamples) {
          CodeGenerator sampleGenerator =
              GapicGenerator.newBuilder()
                  .setModel(model)
                  .setProductConfig(productConfig)
                  .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                  .setModelToViewTransformer(
                      new PhpGapicSamplesTransformer(phpPathMapper, packageConfig))
                  .build();
          generators.add(sampleGenerator);
        }

        generators.add(generator);
        generators.add(clientConfigGenerator);
        generators.add(metadataGenerator);
      }
      if (artifactFlags.testGeneratorEnabled()) {
        CodeGenerator testGenerator =
            GapicGenerator.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(new PhpGapicSurfaceTestTransformer(packageConfig))
                .build();
        generators.add(testGenerator);
      }
    } else if (language.equals(PYTHON)) {
      if (artifactFlags.surfaceGeneratorEnabled()) {
        GapicCodePathMapper pythonPathMapper =
            CommonGapicCodePathMapper.newBuilder().setShouldAppendPackage(true).build();
        CodeGenerator mainGenerator =
            GapicGenerator.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new PythonRenderingUtil()))
                .setModelToViewTransformer(
                    new PythonGapicSurfaceTransformer(pythonPathMapper, packageConfig))
                .build();
        CodeGenerator clientConfigGenerator =
            LegacyGapicGenerator.newBuilder()
                .setModel(model)
                .setContext(new ClientConfigGapicContext(model, productConfig))
                .setSnippetSetRunner(
                    new ClientConfigSnippetSetRunner<>(SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                .setSnippetFileNames(Arrays.asList("clientconfig/python_clientconfig.snip"))
                .setCodePathMapper(pythonPathMapper)
                .build();
        generators.add(mainGenerator);
        generators.add(clientConfigGenerator);

        CodeGenerator sampleGenerator =
            GapicGenerator.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new PythonRenderingUtil()))
                .setModelToViewTransformer(
                    new PythonGapicSamplesTransformer(pythonPathMapper, packageConfig))
                .build();
        generators.add(sampleGenerator);

        CodeGenerator metadataGenerator =
            GapicGenerator.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new PythonRenderingUtil()))
                .setModelToViewTransformer(new PythonPackageMetadataTransformer(packageConfig))
                .build();
        generators.add(metadataGenerator);
      }
      if (artifactFlags.testGeneratorEnabled()) {
        GapicCodePathMapper pythonTestPathMapper =
            CommonGapicCodePathMapper.newBuilder()
                .setPrefix("test")
                .setShouldAppendPackage(true)
                .build();
        CodeGenerator testGenerator =
            GapicGenerator.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(
                    new PythonGapicSurfaceTestTransformer(pythonTestPathMapper, packageConfig))
                .build();
        generators.add(testGenerator);
      }

    } else if (language.equals(RUBY)) {
      if (artifactFlags.surfaceGeneratorEnabled()) {
        GapicCodePathMapper rubyPathMapper =
            CommonGapicCodePathMapper.newBuilder()
                .setPrefix("lib")
                .setShouldAppendPackage(true)
                .setPackageFilePathNameFormatter(new RubyNameFormatter())
                .build();
        CodeGenerator mainGenerator =
            GapicGenerator.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(
                    new RubyGapicSurfaceTransformer(rubyPathMapper, packageConfig))
                .build();
        CodeGenerator clientConfigGenerator =
            LegacyGapicGenerator.newBuilder()
                .setModel(model)
                .setContext(new ClientConfigGapicContext(model, productConfig))
                .setSnippetSetRunner(
                    new ClientConfigSnippetSetRunner<>(SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                .setSnippetFileNames(Arrays.asList("clientconfig/json.snip"))
                .setCodePathMapper(rubyPathMapper)
                .build();
        CodeGenerator metadataGenerator =
            GapicGenerator.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(new RubyPackageMetadataTransformer(packageConfig))
                .build();

        generators.add(mainGenerator);
        generators.add(clientConfigGenerator);
        generators.add(metadataGenerator);

        CodeGenerator messageGenerator =
            GapicGenerator.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(
                    new RubyGapicSurfaceDocTransformer(rubyPathMapper, packageConfig))
                .build();
        generators.add(messageGenerator);
      }
      if (artifactFlags.testGeneratorEnabled()) {
        CommonGapicCodePathMapper.Builder rubyTestPathMapperBuilder =
            CommonGapicCodePathMapper.newBuilder()
                .setShouldAppendPackage(true)
                .setPackageFilePathNameFormatter(new RubyNameFormatter());
        CodeGenerator testGenerator =
            GapicGenerator.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(
                    new RubyGapicSurfaceTestTransformer(
                        rubyTestPathMapperBuilder.setPrefix("test").build(),
                        rubyTestPathMapperBuilder.setPrefix("acceptance").build(),
                        packageConfig))
                .build();
        generators.add(testGenerator);
      }
    } else {
      throw new UnsupportedOperationException(
          "GapicGeneratorFactory: unsupported language \"" + language + "\"");
    }

    if (generators.isEmpty()) {
      throw new IllegalArgumentException("No artifacts are enabled.");
    }
    return generators;
  }
}

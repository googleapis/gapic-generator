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
package com.google.api.codegen.gapic;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.clientconfig.ClientConfigGapicContext;
import com.google.api.codegen.clientconfig.ClientConfigSnippetSetRunner;
import com.google.api.codegen.clientconfig.php.PhpClientConfigGapicContext;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.grpcmetadatagen.java.JavaPackageCopier;
import com.google.api.codegen.nodejs.NodeJSCodePathMapper;
import com.google.api.codegen.php.PhpGapicCodePathMapper;
import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.codegen.transformer.csharp.CSharpGapicClientTransformer;
import com.google.api.codegen.transformer.csharp.CSharpGapicSnippetsTransformer;
import com.google.api.codegen.transformer.csharp.CSharpGapicTestTransformer;
import com.google.api.codegen.transformer.go.GoGapicSurfaceTestTransformer;
import com.google.api.codegen.transformer.go.GoGapicSurfaceTransformer;
import com.google.api.codegen.transformer.java.JavaGapicMetadataTransformer;
import com.google.api.codegen.transformer.java.JavaGapicSampleAppTransformer;
import com.google.api.codegen.transformer.java.JavaGapicSurfaceTestTransformer;
import com.google.api.codegen.transformer.java.JavaGapicSurfaceTransformer;
import com.google.api.codegen.transformer.nodejs.NodeJSGapicSurfaceDocTransformer;
import com.google.api.codegen.transformer.nodejs.NodeJSGapicSurfaceTestTransformer;
import com.google.api.codegen.transformer.nodejs.NodeJSGapicSurfaceTransformer;
import com.google.api.codegen.transformer.nodejs.NodeJSPackageMetadataTransformer;
import com.google.api.codegen.transformer.php.PhpGapicSurfaceTestTransformer;
import com.google.api.codegen.transformer.php.PhpGapicSurfaceTransformer;
import com.google.api.codegen.transformer.php.PhpPackageMetadataTransformer;
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
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.NotImplementedException;

/** MainGapicProviderFactory creates GapicProvider instances based on an id. */
public class MainGapicProviderFactory
    implements GapicProviderFactory<GapicProvider<? extends Object>> {

  public static final String CLIENT_CONFIG = "client_config";
  public static final String CSHARP = "csharp";
  public static final String GO = "go";
  public static final String JAVA = "java";
  public static final String NODEJS = "nodejs";
  public static final String NODEJS_DOC = "nodejs_doc";
  public static final String PHP = "php";
  public static final String PYTHON = "py";
  public static final String RUBY = "ruby";
  public static final String RUBY_DOC = "ruby_doc";

  private static final ImmutableList<String> JAVA_SAMPLE_APP_STATIC_FILES =
      ImmutableList.of(
          "gradlew",
          "gradle/wrapper/gradle-wrapper.jar",
          "gradle/wrapper/gradle-wrapper.properties",
          "gradlew.bat",
          "settings.gradle");

  /** Create the GapicProviders based on the given id */
  public static List<GapicProvider<? extends Object>> defaultCreate(
      Model model,
      GapicProductConfig productConfig,
      GapicGeneratorConfig generatorConfig,
      PackageMetadataConfig packageConfig,
      String outputPath) {

    ArrayList<GapicProvider<? extends Object>> providers = new ArrayList<>();
    String id = generatorConfig.id();
    // Please keep the following IDs in alphabetical order
    if (id.equals(CLIENT_CONFIG)) {
      GapicProvider<? extends Object> provider =
          CommonGapicProvider.<Interface>newBuilder()
              .setModel(model)
              .setView(new InterfaceView())
              .setContext(new ClientConfigGapicContext(model, productConfig))
              .setSnippetSetRunner(
                  new ClientConfigSnippetSetRunner<Interface>(
                      SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
              .setSnippetFileNames(Arrays.asList("clientconfig/json.snip"))
              .setCodePathMapper(CommonGapicCodePathMapper.defaultInstance())
              .build();
      providers.add(provider);
    } else if (id.equals(CSHARP)) {
      if (generatorConfig.enableSurfaceGenerator()) {
        GapicCodePathMapper pathMapper =
            CommonGapicCodePathMapper.newBuilder()
                .setPrefix("")
                .setPackageFilePathNameFormatter(new CSharpNameFormatter())
                .build();
        GapicProvider<? extends Object> mainProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CSharpRenderingUtil()))
                .setModelToViewTransformer(
                    new CSharpGapicClientTransformer(pathMapper, packageConfig))
                .build();
        providers.add(mainProvider);

        GapicProvider<? extends Object> snippetProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CSharpRenderingUtil()))
                .setModelToViewTransformer(new CSharpGapicSnippetsTransformer(pathMapper))
                .build();
        providers.add(snippetProvider);
      }
      if (generatorConfig.enableTestGenerator()) {
        GapicCodePathMapper pathMapper =
            CommonGapicCodePathMapper.newBuilder()
                .setPrefix("")
                .setPackageFilePathNameFormatter(new CSharpNameFormatter())
                .build();
        GapicProvider<? extends Object> smokeTestProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CSharpRenderingUtil()))
                .setModelToViewTransformer(
                    new CSharpGapicTestTransformer(pathMapper, packageConfig))
                .build();
        providers.add(smokeTestProvider);
      }

    } else if (id.equals(GO)) {
      if (generatorConfig.enableSurfaceGenerator()) {
        GapicProvider<? extends Object> provider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(
                    new GoGapicSurfaceTransformer(new PackageNameCodePathMapper()))
                .build();
        providers.add(provider);
      }
      if (generatorConfig.enableTestGenerator()) {
        GapicProvider<? extends Object> testProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(new GoGapicSurfaceTestTransformer())
                .build();
        providers.add(testProvider);
      }

    } else if (id.equals(JAVA)) {
      if (generatorConfig.enableSurfaceGenerator()) {
        GapicCodePathMapper javaPathMapper =
            CommonGapicCodePathMapper.newBuilder()
                .setPrefix("src/main/java")
                .setShouldAppendPackage(true)
                .build();
        GapicProvider<? extends Object> mainProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new JavaRenderingUtil()))
                .setModelToViewTransformer(
                    new JavaGapicSurfaceTransformer(javaPathMapper, packageConfig))
                .build();

        providers.add(mainProvider);

        GapicProvider<? extends Object> metadataProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new JavaRenderingUtil()))
                .setModelToViewTransformer(
                    new JavaGapicMetadataTransformer(
                        javaPathMapper, productConfig, packageConfig, generatorConfig))
                .build();

        providers.add(metadataProvider);
      }
      if (generatorConfig.enableTestGenerator()) {
        GapicCodePathMapper javaTestPathMapper =
            CommonGapicCodePathMapper.newBuilder()
                .setPrefix("src/test/java")
                .setShouldAppendPackage(true)
                .build();
        GapicProvider<? extends Object> testProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(new JavaGapicSurfaceTestTransformer(javaTestPathMapper))
                .build();
        providers.add(testProvider);
      }
      if (generatorConfig.enableSampleAppGenerator()) {
        GapicCodePathMapper javaSampleAppPathMapper =
            CommonGapicCodePathMapper.newBuilder()
                .setPrefix("src/main/java")
                .setShouldAppendPackage(true)
                .build();
        GapicProvider<? extends Object> sampleAppProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(
                    new JavaGapicSampleAppTransformer(javaSampleAppPathMapper))
                .build();
        providers.add(sampleAppProvider);

        // Copy static files for the Java sample application (e.g. gradle wrapper, build files)
        GapicProvider<? extends Object> staticFileProvider =
            new StaticGapicProvider<>(
                new JavaPackageCopier(JAVA_SAMPLE_APP_STATIC_FILES, outputPath));
        providers.add(staticFileProvider);
      }
      return providers;

    } else if (id.equals(NODEJS) || id.equals(NODEJS_DOC)) {
      if (generatorConfig.enableSurfaceGenerator()) {
        GapicCodePathMapper nodeJSPathMapper = new NodeJSCodePathMapper();
        GapicProvider<? extends Object> mainProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(
                    new NodeJSGapicSurfaceTransformer(nodeJSPathMapper, packageConfig))
                .build();
        GapicProvider<? extends Object> metadataProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(new NodeJSPackageMetadataTransformer(packageConfig))
                .build();
        GapicProvider<? extends Object> clientConfigProvider =
            CommonGapicProvider.<Interface>newBuilder()
                .setModel(model)
                .setView(new InterfaceView())
                .setContext(new ClientConfigGapicContext(model, productConfig))
                .setSnippetSetRunner(
                    new ClientConfigSnippetSetRunner<Interface>(
                        SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                .setSnippetFileNames(Arrays.asList("clientconfig/json.snip"))
                .setCodePathMapper(nodeJSPathMapper)
                .build();

        providers.add(mainProvider);
        providers.add(metadataProvider);
        providers.add(clientConfigProvider);

        if (id.equals(NODEJS_DOC)) {
          GapicProvider<? extends Object> messageProvider =
              ViewModelGapicProvider.newBuilder()
                  .setModel(model)
                  .setProductConfig(productConfig)
                  .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                  .setModelToViewTransformer(new NodeJSGapicSurfaceDocTransformer())
                  .build();
          providers.add(messageProvider);
        }
      }
      if (generatorConfig.enableTestGenerator()) {
        GapicProvider<? extends Object> testProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(new NodeJSGapicSurfaceTestTransformer())
                .build();
        providers.add(testProvider);
      }

    } else if (id.equals(PHP)) {
      if (generatorConfig.enableSurfaceGenerator()) {
        GapicCodePathMapper phpPathMapper =
            PhpGapicCodePathMapper.newBuilder().setPrefix("src").build();
        GapicProvider<? extends Object> provider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(
                    new PhpGapicSurfaceTransformer(productConfig, phpPathMapper))
                .build();

        GapicCodePathMapper phpClientConfigPathMapper =
            PhpGapicCodePathMapper.newBuilder().setPrefix("src").setSuffix("resources").build();
        GapicProvider<? extends Object> clientConfigProvider =
            CommonGapicProvider.<Interface>newBuilder()
                .setModel(model)
                .setView(new InterfaceView())
                .setContext(new PhpClientConfigGapicContext(model, productConfig))
                .setSnippetSetRunner(
                    new ClientConfigSnippetSetRunner<Interface>(
                        SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                .setSnippetFileNames(Arrays.asList("clientconfig/json.snip"))
                .setCodePathMapper(phpClientConfigPathMapper)
                .build();

        GapicProvider<? extends Object> metadataProvider =
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

      if (generatorConfig.enableTestGenerator()) {
        GapicProvider<? extends Object> testProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(new PhpGapicSurfaceTestTransformer())
                .build();
        providers.add(testProvider);
      }

    } else if (id.equals(PYTHON)) {
      if (generatorConfig.enableSurfaceGenerator()) {
        GapicCodePathMapper pythonPathMapper =
            CommonGapicCodePathMapper.newBuilder().setShouldAppendPackage(true).build();
        GapicProvider<? extends Object> mainProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new PythonRenderingUtil()))
                .setModelToViewTransformer(
                    new PythonGapicSurfaceTransformer(pythonPathMapper, packageConfig))
                .build();
        GapicProvider<? extends Object> clientConfigProvider =
            CommonGapicProvider.<Interface>newBuilder()
                .setModel(model)
                .setView(new InterfaceView())
                .setContext(new ClientConfigGapicContext(model, productConfig))
                .setSnippetSetRunner(
                    new ClientConfigSnippetSetRunner<Interface>(
                        SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                .setSnippetFileNames(Arrays.asList("clientconfig/python_clientconfig.snip"))
                .setCodePathMapper(pythonPathMapper)
                .build();
        providers.add(mainProvider);
        providers.add(clientConfigProvider);

        if (id.equals(PYTHON)) {
          GapicCodePathMapper pythonTestPathMapper =
              CommonGapicCodePathMapper.newBuilder()
                  .setPrefix("test")
                  .setShouldAppendPackage(true)
                  .build();
          GapicProvider<? extends Object> testProvider =
              ViewModelGapicProvider.newBuilder()
                  .setModel(model)
                  .setProductConfig(productConfig)
                  .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                  .setModelToViewTransformer(
                      new PythonGapicSurfaceTestTransformer(pythonTestPathMapper, packageConfig))
                  .build();
          providers.add(testProvider);
        }

        GapicProvider<? extends Object> metadataProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new PythonRenderingUtil()))
                .setModelToViewTransformer(new PythonPackageMetadataTransformer(packageConfig))
                .build();
        providers.add(metadataProvider);
      }

    } else if (id.equals(RUBY) || id.equals(RUBY_DOC)) {
      if (generatorConfig.enableSurfaceGenerator()) {
        GapicCodePathMapper rubyPathMapper =
            CommonGapicCodePathMapper.newBuilder()
                .setPrefix("lib")
                .setShouldAppendPackage(true)
                .setPackageFilePathNameFormatter(new RubyNameFormatter())
                .build();
        GapicProvider<? extends Object> mainProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(
                    new RubyGapicSurfaceTransformer(rubyPathMapper, packageConfig))
                .build();
        GapicProvider<? extends Object> clientConfigProvider =
            CommonGapicProvider.<Interface>newBuilder()
                .setModel(model)
                .setView(new InterfaceView())
                .setContext(new ClientConfigGapicContext(model, productConfig))
                .setSnippetSetRunner(
                    new ClientConfigSnippetSetRunner<Interface>(
                        SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                .setSnippetFileNames(Arrays.asList("clientconfig/json.snip"))
                .setCodePathMapper(rubyPathMapper)
                .build();
        GapicProvider<? extends Object> metadataProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(new RubyPackageMetadataTransformer(packageConfig))
                .build();

        providers.add(mainProvider);
        providers.add(clientConfigProvider);
        providers.add(metadataProvider);

        if (id.equals(RUBY_DOC)) {
          GapicProvider<? extends Object> messageProvider =
              ViewModelGapicProvider.newBuilder()
                  .setModel(model)
                  .setProductConfig(productConfig)
                  .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                  .setModelToViewTransformer(
                      new RubyGapicSurfaceDocTransformer(rubyPathMapper, packageConfig))
                  .build();
          providers.add(messageProvider);
        }
      }
      if (generatorConfig.enableTestGenerator()) {
        GapicCodePathMapper rubyTestPathMapper =
            CommonGapicCodePathMapper.newBuilder()
                .setPrefix("test")
                .setShouldAppendPackage(true)
                .setPackageFilePathNameFormatter(new RubyNameFormatter())
                .build();
        GapicProvider<? extends Object> testProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(
                    new RubyGapicSurfaceTestTransformer(rubyTestPathMapper, packageConfig))
                .build();
        providers.add(testProvider);
      }
    } else {
      throw new NotImplementedException("GapicProviderFactory: invalid id \"" + id + "\"");
    }

    if (providers.isEmpty()) {
      throw new IllegalArgumentException("No artifacts are enabled.");
    }
    return providers;
  }

  /** Create the GapicProviders based on the given id */
  @Override
  public List<GapicProvider<? extends Object>> create(
      Model model,
      GapicProductConfig productConfig,
      GapicGeneratorConfig generatorConfig,
      PackageMetadataConfig packageConfig,
      String outputPath) {
    return defaultCreate(model, productConfig, generatorConfig, packageConfig, outputPath);
  }
}

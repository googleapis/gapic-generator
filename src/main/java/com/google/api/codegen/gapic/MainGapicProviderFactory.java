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
import com.google.api.codegen.ProtoFileView;
import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.clientconfig.ClientConfigGapicContext;
import com.google.api.codegen.clientconfig.ClientConfigSnippetSetRunner;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.nodejs.NodeJSCodePathMapper;
import com.google.api.codegen.nodejs.NodeJSGapicContext;
import com.google.api.codegen.nodejs.NodeJSSnippetSetRunner;
import com.google.api.codegen.php.PhpGapicCodePathMapper;
import com.google.api.codegen.py.PythonGapicContext;
import com.google.api.codegen.py.PythonInterfaceInitializer;
import com.google.api.codegen.py.PythonProtoFileInitializer;
import com.google.api.codegen.py.PythonSnippetSetRunner;
import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.codegen.ruby.RubyGapicContext;
import com.google.api.codegen.ruby.RubySnippetSetRunner;
import com.google.api.codegen.transformer.csharp.CSharpGapicClientTransformer;
import com.google.api.codegen.transformer.csharp.CSharpGapicSnippetsTransformer;
import com.google.api.codegen.transformer.go.GoGapicSurfaceTestTransformer;
import com.google.api.codegen.transformer.go.GoGapicSurfaceTransformer;
import com.google.api.codegen.transformer.java.JavaGapicSurfaceTestTransformer;
import com.google.api.codegen.transformer.java.JavaGapicSurfaceTransformer;
import com.google.api.codegen.transformer.php.PhpGapicSurfaceTransformer;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.csharp.CSharpNameFormatter;
import com.google.api.codegen.util.csharp.CSharpRenderingUtil;
import com.google.api.codegen.util.java.JavaRenderingUtil;
import com.google.api.codegen.util.ruby.RubyNameFormatter;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
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
  public static final String PYTHON = "python";
  public static final String PYTHON_DOC = "python_doc";
  public static final String RUBY = "ruby";
  public static final String RUBY_DOC = "ruby_doc";

  /** Create the GapicProviders based on the given id */
  public static List<GapicProvider<? extends Object>> defaultCreate(
      Model model, ApiConfig apiConfig, GapicGeneratorConfig generatorConfig) {

    ArrayList<GapicProvider<? extends Object>> providers = new ArrayList<>();
    String id = generatorConfig.id();
    // Please keep the following IDs in alphabetical order
    if (id.equals(CLIENT_CONFIG)) {
      GapicProvider<? extends Object> provider =
          CommonGapicProvider.<Interface>newBuilder()
              .setModel(model)
              .setView(new InterfaceView())
              .setContext(new ClientConfigGapicContext(model, apiConfig))
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
                .setShouldAppendPackage(true)
                .setPackageFilePathNameFormatter(new CSharpNameFormatter())
                .build();
        GapicProvider<? extends Object> mainProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setApiConfig(apiConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CSharpRenderingUtil()))
                .setModelToViewTransformer(new CSharpGapicClientTransformer(pathMapper))
                .build();
        providers.add(mainProvider);

        GapicProvider<? extends Object> snippetProvider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setApiConfig(apiConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CSharpRenderingUtil()))
                .setModelToViewTransformer(new CSharpGapicSnippetsTransformer(pathMapper))
                .build();
        providers.add(snippetProvider);
      }

    } else if (id.equals(GO)) {
      if (generatorConfig.enableSurfaceGenerator()) {
        GapicProvider<? extends Object> provider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setApiConfig(apiConfig)
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
                .setApiConfig(apiConfig)
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
                .setApiConfig(apiConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new JavaRenderingUtil()))
                .setModelToViewTransformer(new JavaGapicSurfaceTransformer(javaPathMapper))
                .build();

        providers.add(mainProvider);
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
                .setApiConfig(apiConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(new JavaGapicSurfaceTestTransformer(javaTestPathMapper))
                .build();
        providers.add(testProvider);
      }
      return providers;

    } else if (id.equals(NODEJS) || id.equals(NODEJS_DOC)) {
      if (generatorConfig.enableSurfaceGenerator()) {
        GapicCodePathMapper nodeJSPathMapper = new NodeJSCodePathMapper();
        GapicProvider<? extends Object> mainProvider =
            CommonGapicProvider.<Interface>newBuilder()
                .setModel(model)
                .setView(new InterfaceView())
                .setContext(new NodeJSGapicContext(model, apiConfig))
                .setSnippetSetRunner(
                    new NodeJSSnippetSetRunner<Interface>(SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                .setSnippetFileNames(Arrays.asList("nodejs/main.snip"))
                .setCodePathMapper(nodeJSPathMapper)
                .build();
        GapicProvider<? extends Object> clientConfigProvider =
            CommonGapicProvider.<Interface>newBuilder()
                .setModel(model)
                .setView(new InterfaceView())
                .setContext(new ClientConfigGapicContext(model, apiConfig))
                .setSnippetSetRunner(
                    new ClientConfigSnippetSetRunner<Interface>(
                        SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                .setSnippetFileNames(Arrays.asList("clientconfig/json.snip"))
                .setCodePathMapper(nodeJSPathMapper)
                .build();

        providers.add(mainProvider);
        providers.add(clientConfigProvider);

        if (id.equals(NODEJS_DOC)) {
          GapicProvider<? extends Object> messageProvider =
              CommonGapicProvider.<ProtoFile>newBuilder()
                  .setModel(model)
                  .setView(new ProtoFileView())
                  .setContext(new NodeJSGapicContext(model, apiConfig))
                  .setSnippetSetRunner(
                      new NodeJSSnippetSetRunner<ProtoFile>(SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                  .setSnippetFileNames(Arrays.asList("nodejs/message.snip"))
                  .setCodePathMapper(nodeJSPathMapper)
                  .build();
          providers.add(messageProvider);
        }
      }

    } else if (id.equals(PHP)) {
      if (generatorConfig.enableSurfaceGenerator()) {
        GapicCodePathMapper phpPathMapper =
            PhpGapicCodePathMapper.newBuilder().setPrefix("src").build();
        GapicProvider<? extends Object> provider =
            ViewModelGapicProvider.newBuilder()
                .setModel(model)
                .setApiConfig(apiConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformer(new PhpGapicSurfaceTransformer(apiConfig, phpPathMapper))
                .build();

        GapicCodePathMapper phpClientConfigPathMapper =
            PhpGapicCodePathMapper.newBuilder().setPrefix("src").setSuffix("resources").build();
        GapicProvider<? extends Object> clientConfigProvider =
            CommonGapicProvider.<Interface>newBuilder()
                .setModel(model)
                .setView(new InterfaceView())
                .setContext(new ClientConfigGapicContext(model, apiConfig))
                .setSnippetSetRunner(
                    new ClientConfigSnippetSetRunner<Interface>(
                        SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                .setSnippetFileNames(Arrays.asList("clientconfig/json.snip"))
                .setCodePathMapper(phpClientConfigPathMapper)
                .build();
        providers.add(provider);
        providers.add(clientConfigProvider);
      }

    } else if (id.equals(PYTHON) || id.equals(PYTHON_DOC)) {
      if (generatorConfig.enableSurfaceGenerator()) {
        GapicCodePathMapper pythonPathMapper =
            CommonGapicCodePathMapper.newBuilder().setShouldAppendPackage(true).build();
        GapicProvider<? extends Object> mainProvider =
            CommonGapicProvider.<Interface>newBuilder()
                .setModel(model)
                .setView(new InterfaceView())
                .setContext(new PythonGapicContext(model, apiConfig))
                .setSnippetSetRunner(
                    new PythonSnippetSetRunner<>(
                        new PythonInterfaceInitializer(apiConfig),
                        SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                .setSnippetFileNames(Arrays.asList("py/main.snip"))
                .setCodePathMapper(pythonPathMapper)
                .build();
        // Note: enumProvider implementation doesn't care about the InputElementT view.
        GapicProvider<? extends Object> enumProvider =
            CommonGapicProvider.<Interface>newBuilder()
                .setModel(model)
                .setView(new InterfaceView())
                .setContext(new PythonGapicContext(model, apiConfig))
                .setSnippetSetRunner(
                    new PythonSnippetSetRunner<Interface>(
                        new PythonInterfaceInitializer(apiConfig),
                        SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                .setSnippetFileNames(Arrays.asList("py/enum.snip"))
                .setCodePathMapper(pythonPathMapper)
                .build();
        GapicProvider<? extends Object> clientConfigProvider =
            CommonGapicProvider.<Interface>newBuilder()
                .setModel(model)
                .setView(new InterfaceView())
                .setContext(new ClientConfigGapicContext(model, apiConfig))
                .setSnippetSetRunner(
                    new ClientConfigSnippetSetRunner<Interface>(
                        SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                .setSnippetFileNames(Arrays.asList("clientconfig/json.snip"))
                .setCodePathMapper(pythonPathMapper)
                .build();

        providers.add(mainProvider);
        providers.add(clientConfigProvider);
        providers.add(enumProvider);

        if (id.equals(PYTHON_DOC)) {
          GapicProvider<? extends Object> messageProvider =
              CommonGapicProvider.<ProtoFile>newBuilder()
                  .setModel(model)
                  .setView(new ProtoFileView())
                  .setContext(new PythonGapicContext(model, apiConfig))
                  .setSnippetSetRunner(
                      new PythonSnippetSetRunner<>(
                          new PythonProtoFileInitializer(), SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                  .setSnippetFileNames(Arrays.asList("py/message.snip"))
                  .setCodePathMapper(CommonGapicCodePathMapper.defaultInstance())
                  .build();

          providers.add(messageProvider);
        }
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
            CommonGapicProvider.<Interface>newBuilder()
                .setModel(model)
                .setView(new InterfaceView())
                .setContext(new RubyGapicContext(model, apiConfig))
                .setSnippetSetRunner(
                    new RubySnippetSetRunner<Interface>(SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                .setSnippetFileNames(Arrays.asList("ruby/main.snip"))
                .setCodePathMapper(rubyPathMapper)
                .build();
        GapicProvider<? extends Object> clientConfigProvider =
            CommonGapicProvider.<Interface>newBuilder()
                .setModel(model)
                .setView(new InterfaceView())
                .setContext(new ClientConfigGapicContext(model, apiConfig))
                .setSnippetSetRunner(
                    new ClientConfigSnippetSetRunner<Interface>(
                        SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                .setSnippetFileNames(Arrays.asList("clientconfig/json.snip"))
                .setCodePathMapper(rubyPathMapper)
                .build();

        providers.add(mainProvider);
        providers.add(clientConfigProvider);

        if (id.equals(RUBY_DOC)) {
          GapicProvider<? extends Object> messageProvider =
              CommonGapicProvider.<ProtoFile>newBuilder()
                  .setModel(model)
                  .setView(new ProtoFileView())
                  .setContext(new RubyGapicContext(model, apiConfig))
                  .setSnippetSetRunner(
                      new RubySnippetSetRunner<ProtoFile>(SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
                  .setSnippetFileNames(Arrays.asList("ruby/message.snip"))
                  .setCodePathMapper(rubyPathMapper)
                  .build();
          providers.add(messageProvider);
        }
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
      Model model, ApiConfig apiConfig, GapicGeneratorConfig generatorConfig) {
    return defaultCreate(model, apiConfig, generatorConfig);
  }
}

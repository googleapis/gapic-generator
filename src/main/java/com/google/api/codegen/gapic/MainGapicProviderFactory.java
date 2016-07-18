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

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.InterfaceListView;
import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.ProtoFileView;
import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.clientconfig.ClientConfigGapicContext;
import com.google.api.codegen.clientconfig.ClientConfigSnippetSetRunner;
import com.google.api.codegen.csharp.CSharpCodePathMapper;
import com.google.api.codegen.csharp.CSharpGapicContext;
import com.google.api.codegen.csharp.CSharpSnippetSetRunner;
import com.google.api.codegen.go.GoGapicContext;
import com.google.api.codegen.go.GoSnippetSetRunner;
import com.google.api.codegen.java.JavaGapicContext;
import com.google.api.codegen.java.JavaIterableSnippetSetRunner;
import com.google.api.codegen.java.JavaSnippetSetRunner;
import com.google.api.codegen.nodejs.NodeJSGapicContext;
import com.google.api.codegen.nodejs.NodeJSSnippetSetRunner;
import com.google.api.codegen.php.PhpGapicContext;
import com.google.api.codegen.php.PhpSnippetSetRunner;
import com.google.api.codegen.py.PythonGapicContext;
import com.google.api.codegen.py.PythonInterfaceInitializer;
import com.google.api.codegen.py.PythonProtoFileInitializer;
import com.google.api.codegen.py.PythonSnippetSetRunner;
import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.codegen.ruby.RubyGapicContext;
import com.google.api.codegen.ruby.RubySnippetSetRunner;
import com.google.api.codegen.transformer.java.JavaGapicSurfaceTransformer;
import com.google.api.codegen.transformer.php.PhpGapicSurfaceTransformer;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.java.JavaRenderingUtil;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

/**
 * MainGapicProviderFactory creates GapicProvider instances based on an id.
 */
public class MainGapicProviderFactory
    implements GapicProviderFactory<GapicProvider<? extends Object>> {

  public static final String CLIENT_CONFIG = "client_config";
  public static final String CSHARP = "csharp";
  public static final String GO = "go";
  public static final String JAVA = "java";
  public static final String NODEJS = "nodejs";
  public static final String PHP = "php";
  public static final String PYTHON = "python";
  public static final String RUBY = "ruby";

  public static final String JAVA_SURFACE = "java_surface";
  public static final String PHP_SURFACE = "php_surface";

  /**
   * Create the GapicProviders based on the given id
   */
  public static List<GapicProvider<? extends Object>> defaultCreate(
      Model model, ApiConfig apiConfig, String id) {

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
      return Arrays.<GapicProvider<? extends Object>>asList(provider);

    } else if (id.equals(CSHARP)) {
      GapicProvider<? extends Object> provider =
          CommonGapicProvider.<Interface>newBuilder()
              .setModel(model)
              .setView(new InterfaceView())
              .setContext(new CSharpGapicContext(model, apiConfig))
              .setSnippetSetRunner(
                  new CSharpSnippetSetRunner<Interface>(SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
              .setSnippetFileNames(Arrays.asList("csharp/wrapper.snip"))
              .setCodePathMapper(new CSharpCodePathMapper())
              .build();
      return Arrays.<GapicProvider<? extends Object>>asList(provider);

    } else if (id.equals(GO)) {
      GapicProvider<? extends Object> provider =
          CommonGapicProvider.<Interface>newBuilder()
              .setModel(model)
              .setView(new InterfaceView())
              .setContext(new GoGapicContext(model, apiConfig))
              .setSnippetSetRunner(
                  new GoSnippetSetRunner<Interface>(SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
              .setSnippetFileNames(
                  Arrays.asList("go/main.snip", "go/example.snip", "go/doc.snip", "go/common.snip"))
              .setCodePathMapper(CommonGapicCodePathMapper.defaultInstance())
              .build();
      return Arrays.<GapicProvider<? extends Object>>asList(provider);

    } else if (id.equals(JAVA)) {
      GapicCodePathMapper javaPathMapper =
          CommonGapicCodePathMapper.newBuilder()
              .setPrefix("src/main/java")
              .setShouldAppendPackage(true)
              .build();
      GapicProvider<? extends Object> mainProvider =
          CommonGapicProvider.<Interface>newBuilder()
              .setModel(model)
              .setView(new InterfaceView())
              .setContext(new JavaGapicContext(model, apiConfig))
              .setSnippetSetRunner(
                  new JavaSnippetSetRunner<Interface>(SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
              .setSnippetFileNames(Arrays.asList("java/main.snip", "java/settings.snip"))
              .setCodePathMapper(javaPathMapper)
              .build();
      GapicProvider<? extends Object> packageInfoProvider =
          CommonGapicProvider.<Iterable<Interface>>newBuilder()
              .setModel(model)
              .setView(new InterfaceListView())
              .setContext(new JavaGapicContext(model, apiConfig))
              .setSnippetSetRunner(
                  new JavaIterableSnippetSetRunner<Interface>(
                      SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
              .setSnippetFileNames(Arrays.asList("java/package-info.snip"))
              .setCodePathMapper(javaPathMapper)
              .build();

      return Arrays.<GapicProvider<? extends Object>>asList(mainProvider, packageInfoProvider);

    } else if (id.equals(JAVA_SURFACE)) {
      GapicCodePathMapper javaPathMapper =
          CommonGapicCodePathMapper.newBuilder()
              .setPrefix("src/main/java")
              .setShouldAppendPackage(true)
              .build();
      GapicProvider<? extends Object> mainProvider =
          SurfaceGapicProvider.newBuilder()
              .setModel(model)
              .setSnippetSetRunner(new CommonSnippetSetRunner(new JavaRenderingUtil()))
              .setModelToViewTransformer(new JavaGapicSurfaceTransformer(apiConfig, javaPathMapper))
              .build();

      return Arrays.<GapicProvider<? extends Object>>asList(mainProvider);

    } else if (id.equals(NODEJS)) {
      GapicCodePathMapper nodeJSPathMapper =
          CommonGapicCodePathMapper.newBuilder().setPrefix("lib").build();
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

      return Arrays.<GapicProvider<? extends Object>>asList(mainProvider, clientConfigProvider);

    } else if (id.equals(PHP)) {
      GapicCodePathMapper phpPathMapper =
          CommonGapicCodePathMapper.newBuilder().setPrefix("src").build();
      GapicProvider<? extends Object> provider =
          CommonGapicProvider.<Interface>newBuilder()
              .setModel(model)
              .setView(new InterfaceView())
              .setContext(new PhpGapicContext(model, apiConfig))
              .setSnippetSetRunner(
                  new PhpSnippetSetRunner<Interface>(SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
              .setSnippetFileNames(Arrays.asList("php/main.snip"))
              .setCodePathMapper(phpPathMapper)
              .build();

      GapicCodePathMapper phpClientConfigPathMapper =
          CommonGapicCodePathMapper.newBuilder().setPrefix("resources").build();
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
      return Arrays.<GapicProvider<? extends Object>>asList(provider, clientConfigProvider);

    } else if (id.equals(PHP_SURFACE)) {
      GapicCodePathMapper phpPathMapper =
          CommonGapicCodePathMapper.newBuilder().setPrefix("src").build();
      GapicProvider<? extends Object> mainProvider =
          SurfaceGapicProvider.newBuilder()
              .setModel(model)
              .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
              .setModelToViewTransformer(new PhpGapicSurfaceTransformer(apiConfig, phpPathMapper))
              .build();

      return Arrays.<GapicProvider<? extends Object>>asList(mainProvider);

    } else if (id.equals(PYTHON)) {
      GapicCodePathMapper pythonPathMapper =
          CommonGapicCodePathMapper.newBuilder().setShouldAppendPackage(true).build();
      GapicProvider<? extends Object> mainProvider =
          CommonGapicProvider.<Interface>newBuilder()
              .setModel(model)
              .setView(new InterfaceView())
              .setContext(new PythonGapicContext(model, apiConfig))
              .setSnippetSetRunner(
                  new PythonSnippetSetRunner<Interface>(
                      new PythonInterfaceInitializer(), SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
              .setSnippetFileNames(Arrays.asList("py/main.snip"))
              .setCodePathMapper(pythonPathMapper)
              .build();
      GapicProvider<? extends Object> messageProvider =
          CommonGapicProvider.<ProtoFile>newBuilder()
              .setModel(model)
              .setView(new ProtoFileView())
              .setContext(new PythonGapicContext(model, apiConfig))
              .setSnippetSetRunner(
                  new PythonSnippetSetRunner<ProtoFile>(
                      new PythonProtoFileInitializer(), SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
              .setSnippetFileNames(Arrays.asList("py/message.snip"))
              .setCodePathMapper(CommonGapicCodePathMapper.defaultInstance())
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

      return Arrays.<GapicProvider<? extends Object>>asList(
          mainProvider, messageProvider, clientConfigProvider);

    } else if (id.equals(RUBY)) {
      GapicCodePathMapper rubyPathMapper =
          CommonGapicCodePathMapper.newBuilder()
              .setPrefix("lib")
              .setShouldAppendPackage(true)
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

      return Arrays.<GapicProvider<? extends Object>>asList(
          mainProvider, messageProvider, clientConfigProvider);

    } else {
      throw new NotImplementedException("GapicProviderFactory: invalid id \"" + id + "\"");
    }
  }

  /**
   * Create the GapicProviders based on the given id
   */
  @Override
  public List<GapicProvider<? extends Object>> create(Model model, ApiConfig apiConfig, String id) {
    return defaultCreate(model, apiConfig, id);
  }
}

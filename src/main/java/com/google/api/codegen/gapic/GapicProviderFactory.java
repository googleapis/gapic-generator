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
import com.google.api.codegen.clientconfig.ClientConfigGapicContext;
import com.google.api.codegen.clientconfig.ClientConfigSnippetSetRunner;
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
import com.google.api.codegen.ruby.RubyGapicContext;
import com.google.api.codegen.ruby.RubySnippetSetRunner;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

/**
 * GapicProviderFactory creates GapicProvider instances based on an id.
 */
public class GapicProviderFactory {

  /**
   * Create the GapicProviders based on the given id
   */
  public static List<GapicProvider<? extends Object>> defaultCreate(Model model,
      ApiConfig apiConfig, String id) {

    // Please keep the following IDs in alphabetical order

    if (id.equals("client_config")) {
      GapicProvider<? extends Object> provider = CommonGapicProvider.<Interface>newBuilder()
          .setModel(model)
          .setView(new InterfaceView())
          .setContext(new ClientConfigGapicContext(model, apiConfig))
          .setSnippetSetRunner(new ClientConfigSnippetSetRunner<Interface>())
          .setSnippetFileNames(Arrays.asList("json.snip"))
          .build();
      return Arrays.<GapicProvider<? extends Object>>asList(provider);

    } else if (id.equals("csharp")) {
      GapicProvider<? extends Object> provider = CommonGapicProvider.<Interface>newBuilder()
          .setModel(model)
          .setView(new InterfaceView())
          .setContext(new CSharpGapicContext(model, apiConfig))
          .setSnippetSetRunner(new CSharpSnippetSetRunner<Interface>())
          .setSnippetFileNames(Arrays.asList("wrapper.snip"))
          .build();
      return Arrays.<GapicProvider<? extends Object>>asList(provider);

    } else if (id.equals("go")) {
      GapicProvider<? extends Object> provider = CommonGapicProvider.<Interface>newBuilder()
          .setModel(model)
          .setView(new InterfaceView())
          .setContext(new GoGapicContext(model, apiConfig))
          .setSnippetSetRunner(new GoSnippetSetRunner<Interface>())
          .setSnippetFileNames(Arrays.asList("main.snip", "example.snip", "doc.snip"))
          .build();
      return Arrays.<GapicProvider<? extends Object>>asList(provider);

    } else if (id.equals("java")) {
      GapicProvider<? extends Object> mainProvider = CommonGapicProvider.<Interface>newBuilder()
          .setModel(model)
          .setView(new InterfaceView())
          .setContext(new JavaGapicContext(model, apiConfig))
          .setSnippetSetRunner(new JavaSnippetSetRunner<Interface>())
          .setSnippetFileNames(Arrays.asList("main.snip", "settings.snip"))
          .build();
      GapicProvider<? extends Object> packageInfoProvider = CommonGapicProvider.<Iterable<Interface>>newBuilder()
          .setModel(model)
          .setView(new InterfaceListView())
          .setContext(new JavaGapicContext(model, apiConfig))
          .setSnippetSetRunner(new JavaIterableSnippetSetRunner<Interface>())
          .setSnippetFileNames(Arrays.asList("package-info.snip"))
          .build();

      return Arrays.<GapicProvider<? extends Object>>asList(mainProvider, packageInfoProvider);

    } else if (id.equals("nodejs")) {
      GapicProvider<? extends Object> mainProvider = CommonGapicProvider.<Interface>newBuilder()
          .setModel(model)
          .setView(new InterfaceView())
          .setContext(new NodeJSGapicContext(model, apiConfig))
          .setSnippetSetRunner(new NodeJSSnippetSetRunner<Interface>())
          .setSnippetFileNames(Arrays.asList("main.snip"))
          .build();
      GapicProvider<? extends Object> clientConfigProvider = CommonGapicProvider.<Interface>newBuilder()
          .setModel(model)
          .setView(new InterfaceView())
          .setContext(new ClientConfigGapicContext(model, apiConfig))
          .setSnippetSetRunner(new ClientConfigSnippetSetRunner<Interface>())
          .setSnippetFileNames(Arrays.asList("json.snip"))
          .build();

      return Arrays.<GapicProvider<? extends Object>>asList(mainProvider, clientConfigProvider);

    } else if (id.equals("php")) {
      GapicProvider<? extends Object> provider = CommonGapicProvider.<Interface>newBuilder()
          .setModel(model)
          .setView(new InterfaceView())
          .setContext(new PhpGapicContext(model, apiConfig))
          .setSnippetSetRunner(new PhpSnippetSetRunner<Interface>())
          .setSnippetFileNames(Arrays.asList("main.snip"))
          .build();
      return Arrays.<GapicProvider<? extends Object>>asList(provider);

    } else if (id.equals("python")) {
      GapicProvider<? extends Object> mainProvider = CommonGapicProvider.<Interface>newBuilder()
          .setModel(model)
          .setView(new InterfaceView())
          .setContext(new PythonGapicContext(model, apiConfig))
          .setSnippetSetRunner(new PythonSnippetSetRunner<Interface>(new PythonInterfaceInitializer()))
          .setSnippetFileNames(Arrays.asList("main.snip"))
          .build();
      GapicProvider<? extends Object> messageProvider = CommonGapicProvider.<ProtoFile>newBuilder()
          .setModel(model)
          .setView(new ProtoFileView())
          .setContext(new PythonGapicContext(model, apiConfig))
          .setSnippetSetRunner(new PythonSnippetSetRunner<ProtoFile>(new PythonProtoFileInitializer()))
          .setSnippetFileNames(Arrays.asList("message.snip"))
          .setOutputSubPath("")
          .build();
      GapicProvider<? extends Object> clientConfigProvider = CommonGapicProvider.<Interface>newBuilder()
          .setModel(model)
          .setView(new InterfaceView())
          .setContext(new ClientConfigGapicContext(model, apiConfig))
          .setSnippetSetRunner(new ClientConfigSnippetSetRunner<Interface>())
          .setSnippetFileNames(Arrays.asList("json.snip"))
          .build();

      return Arrays.<GapicProvider<? extends Object>>asList(mainProvider, messageProvider,
          clientConfigProvider);

    } else if (id.equals("ruby")) {
      GapicProvider<? extends Object> mainProvider = CommonGapicProvider.<Interface>newBuilder()
          .setModel(model)
          .setView(new InterfaceView())
          .setContext(new RubyGapicContext(model, apiConfig))
          .setSnippetSetRunner(new RubySnippetSetRunner<Interface>())
          .setSnippetFileNames(Arrays.asList("main.snip"))
          .build();
      GapicProvider<? extends Object> messageProvider = CommonGapicProvider.<ProtoFile>newBuilder()
          .setModel(model)
          .setView(new ProtoFileView())
          .setContext(new RubyGapicContext(model, apiConfig))
          .setSnippetSetRunner(new RubySnippetSetRunner<ProtoFile>())
          .setSnippetFileNames(Arrays.asList("message.snip"))
          .build();
      GapicProvider<? extends Object> clientConfigProvider = CommonGapicProvider.<Interface>newBuilder()
          .setModel(model)
          .setView(new InterfaceView())
          .setContext(new ClientConfigGapicContext(model, apiConfig))
          .setSnippetSetRunner(new ClientConfigSnippetSetRunner<Interface>())
          .setSnippetFileNames(Arrays.asList("json.snip"))
          .build();

      return Arrays.<GapicProvider<? extends Object>>asList(mainProvider, messageProvider,
          clientConfigProvider);

    } else {
      throw new NotImplementedException("GapicProviderFactory: invalid id \"" + id + "\"");
    }
  }

  /**
   * Create the GapicProviders based on the given id
   */
  public List<GapicProvider<? extends Object>> create(Model model, ApiConfig apiConfig, String id) {
    return defaultCreate(model, apiConfig, id);
  }
}

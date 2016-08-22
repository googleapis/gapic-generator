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

import com.google.api.Service;
import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.csharp.CSharpDiscoveryContext;
import com.google.api.codegen.csharp.CSharpSnippetSetRunner;
import com.google.api.codegen.go.GoDiscoveryContext;
import com.google.api.codegen.go.GoSnippetSetRunner;
import com.google.api.codegen.java.JavaDiscoveryContext;
import com.google.api.codegen.java.JavaSnippetSetRunner;
import com.google.api.codegen.nodejs.NodeJSDiscoveryContext;
import com.google.api.codegen.nodejs.NodeJSSnippetSetRunner;
import com.google.api.codegen.php.PhpDiscoveryContext;
import com.google.api.codegen.php.PhpSnippetSetRunner;
import com.google.api.codegen.py.PythonDiscoveryContext;
import com.google.api.codegen.py.PythonDiscoveryInitializer;
import com.google.api.codegen.py.PythonSnippetSetRunner;
import com.google.api.codegen.ruby.RubyDiscoveryContext;
import com.google.api.codegen.ruby.RubySnippetSetRunner;
import com.google.protobuf.Method;

import org.apache.commons.lang3.NotImplementedException;

/**
 * MainDiscoveryProviderFactory creates DiscoveryProvider instances based on an id.
 */
public class MainDiscoveryProviderFactory implements DiscoveryProviderFactory {

  public static final String CSHARP = "csharp";
  public static final String GO = "go";
  public static final String JAVA = "java";
  public static final String NODEJS = "nodejs";
  public static final String PHP = "php";
  public static final String PYTHON = "python";
  public static final String RUBY = "ruby";

  private static final String ADC_SNIPPET_FILE = "discovery_fragment.snip";
  private static final String OAUTH_3L_SNIPPET_FILE = "3lo_discovery_fragment.snip";
  private static final String API_KEY_SNIPPET_FILE = "api_key_discovery_fragment.snip";

  public static DiscoveryProvider defaultCreate(
      Service service, ApiaryConfig apiaryConfig, String id) {
    String snippetFile;
    // TODO(saicheems): Update this as new auth types are supported.
    switch (apiaryConfig.getAuthType()) {
      case APPLICATION_DEFAULT_CREDENTIALS:
      case OAUTH_3L:
      case API_KEY:
        snippetFile = ADC_SNIPPET_FILE;
        break;
      default:
        throw new RuntimeException("unsupported auth type");
    }
    if (id.equals(CSHARP)) {
      return CommonDiscoveryProvider.newBuilder()
          .setContext(new CSharpDiscoveryContext(service, apiaryConfig))
          .setSnippetSetRunner(
              new CSharpSnippetSetRunner<Method>(SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
          .setSnippetFileName(id + "/" + snippetFile)
          .build();

    } else if (id.equals(GO)) {
      return CommonDiscoveryProvider.newBuilder()
          .setContext(new GoDiscoveryContext(service, apiaryConfig))
          .setSnippetSetRunner(
              new GoSnippetSetRunner<Method>(SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
          .setSnippetFileName(id + "/" + snippetFile)
          .build();

    } else if (id.equals(JAVA)) {
      return CommonDiscoveryProvider.newBuilder()
          .setContext(new JavaDiscoveryContext(service, apiaryConfig))
          .setSnippetSetRunner(
              new JavaSnippetSetRunner<Method>(SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
          .setSnippetFileName(id + "/" + snippetFile)
          .build();

    } else if (id.equals(NODEJS)) {
      return CommonDiscoveryProvider.newBuilder()
          .setContext(new NodeJSDiscoveryContext(service, apiaryConfig))
          .setSnippetSetRunner(
              new NodeJSSnippetSetRunner<Method>(SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
          .setSnippetFileName(id + "/" + snippetFile)
          .build();

    } else if (id.equals(PHP)) {
      return CommonDiscoveryProvider.newBuilder()
          .setContext(new PhpDiscoveryContext(service, apiaryConfig))
          .setSnippetSetRunner(
              new PhpSnippetSetRunner<Method>(SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
          .setSnippetFileName(id + "/" + snippetFile)
          .build();

    } else if (id.equals(PYTHON)) {
      return CommonDiscoveryProvider.newBuilder()
          .setContext(new PythonDiscoveryContext(service, apiaryConfig))
          .setSnippetSetRunner(
              new PythonSnippetSetRunner<Method>(
                  new PythonDiscoveryInitializer(), SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
          .setSnippetFileName("py/" + snippetFile)
          .build();

    } else if (id.equals(RUBY)) {
      return CommonDiscoveryProvider.newBuilder()
          .setContext(new RubyDiscoveryContext(service, apiaryConfig))
          .setSnippetSetRunner(
              new RubySnippetSetRunner<Method>(SnippetSetRunner.SNIPPET_RESOURCE_ROOT))
          .setSnippetFileName(id + "/" + snippetFile)
          .build();

    } else {
      throw new NotImplementedException("MainDiscoveryProviderFactory: invalid id \"" + id + "\"");
    }
  }

  @Override
  public DiscoveryProvider create(Service service, ApiaryConfig apiaryConfig, String id) {
    return defaultCreate(service, apiaryConfig, id);
  }
}

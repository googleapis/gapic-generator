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
package com.google.api.codegen.csharp;

import com.google.api.Service;
import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.CodeGeneratorUtil;
import com.google.api.codegen.DiscoveryProvider;
import com.google.api.codegen.GeneratedResult;
import com.google.common.collect.Multimap;
import com.google.protobuf.Method;

import java.io.IOException;

public class CSharpDiscoveryProvider implements DiscoveryProvider {
  private final CSharpDiscoveryContext context;
  private final CSharpSnippetSetRunner snippetSetRunner;

  public CSharpDiscoveryProvider(Service service, ApiaryConfig apiaryConfig) {
    context = new CSharpDiscoveryContext(service, apiaryConfig);
    snippetSetRunner = new CSharpSnippetSetRunner();
  }

  @Override
  public GeneratedResult generateFragments(Method method, String snippetFileName) {
    return snippetSetRunner.generate(method, snippetFileName, context);
  }

  @Override
  public void output(String outputPath, Multimap<Method, GeneratedResult> methods)
      throws IOException {
    String fullOutputPath = outputPath + "/" + context.outputRoot();
    CodeGeneratorUtil.writeGeneratedOutput(fullOutputPath, methods);
  }

  @Override
  public Service getService() {
    return context.getService();
  }
}

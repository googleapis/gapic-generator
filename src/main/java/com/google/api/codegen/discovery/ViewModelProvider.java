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

import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.discovery.transformer.MethodToViewTransformer;
import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.snippet.Doc;
import com.google.protobuf.Method;
import java.util.Map;
import java.util.TreeMap;

/*
 * Calls the MethodToViewTransformer on a method with the provided ApiaryConfig.
 */
public class ViewModelProvider implements DiscoveryProvider {

  private final ApiaryConfig apiaryConfig;
  private final CommonSnippetSetRunner snippetSetRunner;
  private final MethodToViewTransformer methodToViewTransformer;
  private final String outputRoot;

  private ViewModelProvider(
      ApiaryConfig apiaryConfig,
      CommonSnippetSetRunner snippetSetRunner,
      MethodToViewTransformer methodToViewTransformer,
      String outputRoot) {
    this.apiaryConfig = apiaryConfig;
    this.snippetSetRunner = snippetSetRunner;
    this.methodToViewTransformer = methodToViewTransformer;
    this.outputRoot = outputRoot;
  }

  @Override
  public Map<String, Doc> generate(Method method) {
    ViewModel surfaceDoc = methodToViewTransformer.transform(method, apiaryConfig);
    Doc doc = snippetSetRunner.generate(surfaceDoc);
    Map<String, Doc> docs = new TreeMap<>();
    if (doc == null) {
      return docs;
    }
    docs.put(outputRoot + "/" + surfaceDoc.outputPath(), doc);
    return docs;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private ApiaryConfig apiaryConfig;
    private CommonSnippetSetRunner snippetSetRunner;
    private MethodToViewTransformer methodToViewTransformer;
    private String outputRoot;

    private Builder() {}

    public Builder setApiaryConfig(ApiaryConfig apiaryConfig) {
      this.apiaryConfig = apiaryConfig;
      return this;
    }

    public Builder setSnippetSetRunner(CommonSnippetSetRunner snippetSetRunner) {
      this.snippetSetRunner = snippetSetRunner;
      return this;
    }

    public Builder setMethodToViewTransformer(MethodToViewTransformer methodToViewTransformer) {
      this.methodToViewTransformer = methodToViewTransformer;
      return this;
    }

    public Builder setOutputRoot(String outputRoot) {
      this.outputRoot = outputRoot;
      return this;
    }

    public ViewModelProvider build() {
      return new ViewModelProvider(
          apiaryConfig, snippetSetRunner, methodToViewTransformer, outputRoot);
    }
  }
}

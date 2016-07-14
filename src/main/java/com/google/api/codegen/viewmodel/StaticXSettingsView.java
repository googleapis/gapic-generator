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
package com.google.api.codegen.viewmodel;

import com.google.api.codegen.SnippetSetRunner;

import java.util.List;

public class StaticXSettingsView implements ViewModel {
  public String templateFileName;

  public String packageName;
  public String name;
  public String serviceAddress;
  public Integer servicePort;
  public Iterable<String> authScopes;

  public List<String> imports;
  public String outputPath;

  @Override
  public String getResourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  @Override
  public String getTemplateFileName() {
    return templateFileName;
  }

  @Override
  public String getOutputPath() {
    return outputPath;
  }
}

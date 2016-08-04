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
package com.google.api.codegen.viewmodel.testing;

import com.google.api.codegen.viewmodel.ViewModelDoc;

import java.util.List;

public class GapicSurfaceTestClassView implements ViewModelDoc {
  public String packageName;
  public String name;

  public String apiClassName;
  public String apiSettingsClassName;
  public String mockServiceClassName;

  public List<String> imports;
  public String outputPath;

  public List<GapicSurfaceTestCaseView> testCases;

  @Override
  public String getTemplateFileName() {
    return "/test.snip";
  }

  @Override
  public String getOutputPath() {
    return outputPath;
  }
}

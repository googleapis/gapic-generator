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
import com.google.auto.value.AutoValue;
import java.util.List;

@AutoValue
public abstract class SnippetsFileView implements ViewModel {

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  @Override
  public abstract String templateFileName();

  @Override
  public abstract String outputPath();

  public abstract List<ImportTypeView> imports();

  public abstract String packageName();

  public abstract String name();

  public abstract String clientTypeName();

  public abstract List<StaticLangApiMethodSnippetView> snippetMethods();

  public static Builder newBuilder() {
    return new AutoValue_SnippetsFileView.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {
    public abstract Builder templateFileName(String val);

    public abstract Builder outputPath(String val);

    public abstract Builder imports(List<ImportTypeView> val);

    public abstract Builder packageName(String val);

    public abstract Builder name(String val);

    public abstract Builder clientTypeName(String val);

    public abstract Builder snippetMethods(List<StaticLangApiMethodSnippetView> val);

    public abstract SnippetsFileView build();
  }
}

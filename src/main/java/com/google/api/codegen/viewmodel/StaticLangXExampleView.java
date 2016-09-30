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
public abstract class StaticLangXExampleView implements ViewModel {
  @Override
  public abstract String templateFileName();

  @Override
  public abstract String outputPath();

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  /** The package name of the example */
  public abstract String exampleLocalPackageName();

  /** The package name of the library */
  public abstract String libLocalPackageName();

  /** The name of the constructor of the client */
  public abstract String clientConstructorName();

  /** The name of the example of the constructor of the client */
  public abstract String clientConstructorExampleName();

  /** Type of the client */
  public abstract String clientTypeName();

  /** Imports for the example */
  public abstract List<String> imports();

  /** Methods to make examples for */
  public abstract List<StaticLangApiMethodView> apiMethods();

  public boolean getTrue() {
    return true;
  }

  public static Builder newBuilder() {
    return new AutoValue_StaticLangXExampleView.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {
    public abstract Builder templateFileName(String val);

    public abstract Builder outputPath(String val);

    public abstract Builder exampleLocalPackageName(String val);

    public abstract Builder libLocalPackageName(String val);

    public abstract Builder clientConstructorName(String val);

    public abstract Builder clientConstructorExampleName(String val);

    public abstract Builder clientTypeName(String val);

    public abstract Builder imports(List<String> val);

    public abstract Builder apiMethods(List<StaticLangApiMethodView> val);

    public abstract StaticLangXExampleView build();
  }
}

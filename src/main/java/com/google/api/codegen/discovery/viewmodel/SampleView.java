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
package com.google.api.codegen.discovery.viewmodel;

import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.auto.value.AutoValue;
import java.util.List;

@AutoValue
public abstract class SampleView implements ViewModel {

  @Override
  public abstract String templateFileName();

  @Override
  public abstract String outputPath();

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  public abstract String apiTitle();

  public abstract String apiName();

  public abstract String apiVersion();

  public abstract String className();

  public abstract List<String> imports();

  public abstract SampleBodyView body();

  public static Builder newBuilder() {
    return new AutoValue_SampleView.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {

    public abstract Builder templateFileName(String val);

    /*
     * Since the semantics of output path generation differ from GAPIC and
     * discovery, this field should only ever contain the name of the output
     * file. The rest of the path is generated and prefixed in ViewModel, where
     * there is access to the service configuration.
     */
    public abstract Builder outputPath(String val);

    public abstract Builder apiTitle(String val);

    public abstract Builder apiName(String val);

    public abstract Builder apiVersion(String val);

    public abstract Builder className(String val);

    public abstract Builder imports(List<String> val);

    public abstract Builder body(SampleBodyView val);

    public abstract SampleView build();
  }
}

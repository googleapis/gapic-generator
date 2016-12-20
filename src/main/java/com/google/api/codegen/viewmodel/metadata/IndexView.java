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
package com.google.api.codegen.viewmodel.metadata;

import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.auto.value.AutoValue;
import java.util.List;

@AutoValue
public abstract class IndexView implements ViewModel {

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  @Override
  public abstract String templateFileName();

  @Override
  public abstract String outputPath();

  public abstract IndexRequireView primaryService();

  public abstract String apiVersion();

  public abstract List<IndexRequireView> requireViews();

  public boolean hasMultipleServices() {
    return requireViews().size() > 1;
  }

  public static Builder newBuilder() {
    // Use v1 as the default version.
    return new AutoValue_IndexView.Builder().apiVersion("v1");
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder outputPath(String val);

    public abstract Builder templateFileName(String val);

    public abstract Builder primaryService(IndexRequireView val);

    public abstract Builder apiVersion(String val);

    public abstract Builder requireViews(List<IndexRequireView> val);

    public abstract IndexView build();
  }
}

/* Copyright 2016 Google LLC
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
import com.google.api.codegen.viewmodel.StaticLangApiView.Builder;
import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class StaticLangPagedResponseWrappersView implements ViewModel {
  @Override
  public abstract String templateFileName();

  public abstract FileHeaderView fileHeader();

  @Nullable
  public abstract String releaseLevelAnnotation();

  public abstract String name();

  public abstract List<StaticLangPagedResponseView> pagedResponseWrapperList();

  @Override
  public abstract String outputPath();

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  public static Builder newBuilder() {
    return new AutoValue_StaticLangPagedResponseWrappersView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder templateFileName(String val);

    public abstract Builder fileHeader(FileHeaderView val);

    public abstract Builder releaseLevelAnnotation(String releaseAnnotation);

    public abstract Builder name(String val);

    public abstract Builder pagedResponseWrapperList(List<StaticLangPagedResponseView> val);

    public abstract Builder outputPath(String val);

    public abstract StaticLangPagedResponseWrappersView build();
  }
}

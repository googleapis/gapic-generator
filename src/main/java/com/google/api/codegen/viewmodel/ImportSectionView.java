/* Copyright 2017 Google LLC
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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.List;

@AutoValue
public abstract class ImportSectionView {
  public abstract List<ImportFileView> standardImports();

  public abstract List<ImportFileView> externalImports();

  public abstract List<ImportFileView> appImports();

  public abstract List<ImportFileView> serviceImports();

  public static Builder newBuilder() {
    return new AutoValue_ImportSectionView.Builder()
        .standardImports(ImmutableList.<ImportFileView>of())
        .externalImports(ImmutableList.<ImportFileView>of())
        .appImports(ImmutableList.<ImportFileView>of())
        .serviceImports(ImmutableList.<ImportFileView>of());
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder standardImports(List<ImportFileView> val);

    public abstract Builder externalImports(List<ImportFileView> val);

    public abstract Builder appImports(List<ImportFileView> val);

    public abstract Builder serviceImports(List<ImportFileView> val);

    public abstract ImportSectionView build();
  }
}

/* Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.grpcmetadatagen.py;

import com.google.api.tools.framework.snippet.Doc;
import com.google.auto.value.AutoValue;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

@AutoValue
public abstract class PythonPackageCopierResult {
  public static PythonPackageCopierResult createPython(
      List<String> namespacePackages, Map<String, Doc> docs) {
    return new AutoValue_PythonPackageCopierResult.Builder()
        .namespacePackages(namespacePackages)
        .docs(docs)
        .build();
  }

  /**
   * A list of Python package names that should be declared as namespace packages in the package.
   */
  @Nullable
  public abstract List<String> namespacePackages();

  /** Docs to be added to the output doc map in the template rendering phase. */
  public abstract Map<String, Doc> docs();

  @AutoValue.Builder
  protected abstract static class Builder {
    public abstract Builder namespacePackages(List<String> val);

    public abstract Builder docs(Map<String, Doc> val);

    public abstract PythonPackageCopierResult build();
  }
}

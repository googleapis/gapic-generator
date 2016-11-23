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
package com.google.api.codegen.metadatagen;

import com.google.api.tools.framework.snippet.Doc;
import com.google.auto.value.AutoValue;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

@AutoValue
public abstract class PackageCopierResult {

  @AutoValue
  public abstract static class Metadata {
    // Add new constructor when adding a metadata type
    public static AutoValue_PackageCopierResult_Metadata createPython(
        List<String> namespacePackages) {
      return new AutoValue_PackageCopierResult_Metadata(namespacePackages);
    }

    /**
     * A list of Python package names that should be declared as namespace packages in the package.
     */
    @Nullable
    public abstract List<String> pythonNamespacePackages();
  }

  public static AutoValue_PackageCopierResult createPython(
      List<String> namespacePackages, Map<String, Doc> docs) {
    return new AutoValue_PackageCopierResult(Metadata.createPython(namespacePackages), docs);
  }

  /**
   * The metadata computed by the PackageCopier phase that will be passed into the template
   * rendering phase.
   */
  public abstract Metadata metadata();

  /** Docs to be added to the output doc map in the template rendering phase. */
  public abstract Map<String, Doc> docs();
}

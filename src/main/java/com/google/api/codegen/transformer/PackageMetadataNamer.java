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
package com.google.api.codegen.transformer;

import com.google.api.codegen.ReleaseLevel;

/** A PackageMetadataNamer provides language-specific strings for metadata views. */
public class PackageMetadataNamer {

  /** Returns the artifact name */
  public String getMetadataName() {
    return getNotImplementedString("PackageMetadataNamer.getMetadataName");
  }

  /** Returns the artifact identifier */
  public String getMetadataIdentifier() {
    return getNotImplementedString("PackageMetadataNamer.getMetadataIdentifier");
  }

  /** Returns the artifact identifier for a proto classes package */
  public String getProtoPackageName() {
    return getNotImplementedString("PackageMetadataNamer.getProtoPackageName");
  }

  /** Returns the artifact identifier for a gRPC classes package */
  public String getGrpcPackageName() {
    return getNotImplementedString("PackageMetadataNamer.getGrpcPackageName");
  }

  public String getOutputFileName() {
    return getNotImplementedString("PackageMetadataNamer.getOutputFileName");
  }

  public String getReleaseAnnotation(ReleaseLevel releaseLevel) {
    switch (releaseLevel) {
      case UNSET_RELEASE_LEVEL:
        // fallthrough
      case ALPHA:
        return "Alpha";
      case BETA:
        return "Beta";
      case GA:
        return "Production/Stable";
      case DEPRECATED:
        return "Inactive";
      default:
        throw new IllegalStateException("Invalid development status");
    }
  }

  /** Returns the unimplemented string message */
  public String getNotImplementedString(String feature) {
    return "$ NOT IMPLEMENTED: " + feature + " $";
  }
}

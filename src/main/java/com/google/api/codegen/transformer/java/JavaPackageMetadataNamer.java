/* Copyright 2017 Google LLC
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
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.packagegen.ArtifactType;
import com.google.api.codegen.transformer.PackageMetadataNamer;

/** A JavaPackageMetadataNamer provides nodejs specific names for metadata views. */
public class JavaPackageMetadataNamer extends PackageMetadataNamer {
  private final String packageName;
  private final ArtifactType artifactType;

  public JavaPackageMetadataNamer(String packageName, ArtifactType artifactType) {
    this.packageName = packageName;
    this.artifactType = artifactType;
  }

  private static String getMetadataIdentifier(String packageName, ArtifactType artifactType) {
    if (artifactType != null) {
      switch (artifactType) {
        case PROTOBUF:
          return "proto-" + packageName;
        case GRPC:
          return "grpc-" + packageName;
      }
    }
    return packageName;
  }

  @Override
  public String getProtoPackageName() {
    return getMetadataIdentifier(packageName, ArtifactType.PROTOBUF);
  }

  @Override
  public String getGrpcPackageName() {
    return getMetadataIdentifier(packageName, ArtifactType.GRPC);
  }

  @Override
  public String getMetadataIdentifier() {
    return getMetadataIdentifier(packageName, artifactType);
  }

  public String getProtoPackageGroup() {
    return "com.google.api.grpc";
  }
}

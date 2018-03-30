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
package com.google.api.codegen.grpcmetadatagen;

import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.grpcmetadatagen.java.JavaGrpcMetadataProvider;
import com.google.api.codegen.grpcmetadatagen.java.JavaPackageMetadataTransformer;
import com.google.api.codegen.grpcmetadatagen.py.PythonGrpcMetadataProvider;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.common.collect.ImmutableMap;

/** A factory for GrpcMetadataProvider which performs gRPC meta-data generation. */
public class GrpcMetadataProviderFactory {

  /** Create the GrpcMetadataProvider based on the given language */
  public static GrpcMetadataProvider create(
      TargetLanguage language, ArtifactType artifactType, ToolOptions options) {
    switch (language) {
      case PYTHON:
        return createForPython(options);
      case JAVA:
        return createForJava(artifactType);
      default:
        throw new IllegalArgumentException(
            "The target language \"" + language + "\" is not supported");
    }
  }

  private static GrpcMetadataProvider createForPython(ToolOptions options) {
    return new PythonGrpcMetadataProvider(options);
  }

  private static GrpcMetadataProvider createForJava(ArtifactType artifactType) {

    switch (artifactType) {
      case GRPC:
        return new JavaGrpcMetadataProvider(
            new JavaPackageMetadataTransformer(
                ImmutableMap.of(
                    "LICENSE.snip", "LICENSE",
                    "metadatagen/java/grpc/build_grpc.gradle.snip", "build.gradle"),
                artifactType));
      case PROTOBUF:
        return new JavaGrpcMetadataProvider(
            new JavaPackageMetadataTransformer(
                ImmutableMap.of(
                    "LICENSE.snip", "LICENSE",
                    "metadatagen/java/grpc/build_protobuf.gradle.snip", "build.gradle"),
                artifactType));
    }

    throw new IllegalArgumentException(
        "Java does not support the artifact type \"" + artifactType + "\"");
  }
}

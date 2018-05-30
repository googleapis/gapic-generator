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
package com.google.api.codegen.packagegen;

import com.google.api.codegen.common.CodeGenerator;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.packagegen.java.JavaGrpcPackageProvider;
import com.google.api.codegen.packagegen.java.JavaPackageTransformer;
import com.google.api.codegen.packagegen.py.PythonGrpcPackageProvider;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.common.collect.ImmutableMap;

/** A factory for package providers. So far, only grpc packages are supported. */
public class MainPackageProviderFactory {

  /** Create the PackageProvider based on the given language */
  public static CodeGenerator<Doc> create(
      TargetLanguage language,
      ArtifactType artifactType,
      ToolOptions options,
      Model model,
      PackageMetadataConfig config) {
    switch (language) {
      case PYTHON:
        return createForPython(options, model, config);
      case JAVA:
        return createForJava(artifactType, model, config);
      default:
        throw new IllegalArgumentException(
            "The target language \"" + language + "\" is not supported");
    }
  }

  private static CodeGenerator<Doc> createForPython(
      ToolOptions options, Model model, PackageMetadataConfig config) {
    return new PythonGrpcPackageProvider(options, model, config);
  }

  private static CodeGenerator<Doc> createForJava(
      ArtifactType artifactType, Model model, PackageMetadataConfig config) {
    switch (artifactType) {
      case GRPC:
        return new JavaGrpcPackageProvider(
            new JavaPackageTransformer(
                ImmutableMap.of(
                    "LICENSE.snip", "LICENSE",
                    "metadatagen/java/grpc/build_grpc.gradle.snip", "build.gradle",
                    "metadatagen/java/grpc/pom_grpc.xml.snip", "pom.xml"),
                artifactType),
            model,
            config);
      case PROTOBUF:
        return new JavaGrpcPackageProvider(
            new JavaPackageTransformer(
                ImmutableMap.of(
                    "LICENSE.snip", "LICENSE",
                    "metadatagen/java/grpc/build_protobuf.gradle.snip", "build.gradle",
                    "metadatagen/java/grpc/pom_protobuf.xml.snip", "pom.xml"),
                artifactType),
            model,
            config);
    }

    throw new IllegalArgumentException(
        "Java does not support the artifact type \"" + artifactType + "\"");
  }
}

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
package com.google.api.codegen.grpcmetadatagen;

import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.grpcmetadatagen.java.JavaGrpcMetadataProvider;
import com.google.api.codegen.grpcmetadatagen.java.JavaGrpcPackageMetadataTransformer;
import com.google.api.codegen.grpcmetadatagen.java.JavaProtoPackageMetadataTransformer;
import com.google.api.codegen.grpcmetadatagen.py.PythonGrpcMetadataProvider;
import com.google.api.tools.framework.tools.ToolOptions;

/** A factory for GrpcMetadataProvider which performs gRPC meta-data generation. */
public class GrpcMetadataProviderFactory {

  /** Create the GrpcMetadataProvider based on the given language */
  public static GrpcMetadataProvider create(
      TargetLanguage language, PackageMetadataConfig config, ToolOptions options) {
    switch (language) {
      case PYTHON:
        return new PythonGrpcMetadataProvider(options);
      case JAVA:
        if (config.generationLayer() == null) {
          throw new IllegalArgumentException("Java requires a package division to be specified");
        }
        switch (config.generationLayer()) {
          case GRPC:
            return new JavaGrpcMetadataProvider(new JavaGrpcPackageMetadataTransformer(), options);
          case PROTO:
            return new JavaGrpcMetadataProvider(new JavaProtoPackageMetadataTransformer(), options);
          default:
            throw new IllegalArgumentException(
                "Java does not support the package division \"" + config.generationLayer() + "\"");
        }
      default:
        throw new IllegalArgumentException(
            "The target language \"" + language + "\" is not supported");
    }
  }
}

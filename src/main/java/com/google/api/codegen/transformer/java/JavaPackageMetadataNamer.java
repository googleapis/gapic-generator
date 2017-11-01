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
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.grpcmetadatagen.GenerationLayer;
import com.google.api.codegen.transformer.PackageMetadataNamer;

/** A NodeJSPackageMetadataNamer provides nodejs specific names for metadata views. */
public class JavaPackageMetadataNamer extends PackageMetadataNamer {
  private final String packageName;
  private final GenerationLayer generationLayer;

  public JavaPackageMetadataNamer(String packageName, GenerationLayer generationLayer) {
    this.packageName = packageName;
    this.generationLayer = generationLayer;
  }

  private static String getMetadataIdentifier(String packageName, GenerationLayer generationLayer) {
    if (generationLayer != null) {
      switch (generationLayer) {
        case PROTO:
          return "proto-" + packageName;
        case GRPC:
          return "grpc-" + packageName;
      }
    }
    return packageName;
  }

  @Override
  public String getProtoPackageName() {
    return getMetadataIdentifier(packageName, GenerationLayer.PROTO);
  }

  @Override
  public String getGrpcPackageName() {
    return getMetadataIdentifier(packageName, GenerationLayer.GRPC);
  }

  @Override
  public String getMetadataIdentifier() {
    return getMetadataIdentifier(packageName, generationLayer);
  }
}

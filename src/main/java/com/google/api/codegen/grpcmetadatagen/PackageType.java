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
package com.google.api.codegen.grpcmetadatagen;

/** Indicates the type of the package */
public enum PackageType {

  /** Common protos package generated from core protos */
  GRPC_COMMON,

  /** API client protos package generated from API protos e.g. pubsub */
  GRPC_CLIENT;

  public static PackageType of(String packageTypeString) {
    if (packageTypeString != null) {
      return PackageType.valueOf(packageTypeString.toUpperCase());
    } else {
      return null;
    }
  }
}

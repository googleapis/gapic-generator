/* Copyright 2019 Google LLC
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
package com.google.api.codegen.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Parser;
import com.google.protobuf.util.JsonFormat.Printer;
import com.google.protobuf.util.JsonFormat.TypeRegistry;
import javax.annotation.Nonnull;

public class ConfigVersionValidator {

  public static String CONFIG_V2_MAJOR_VERSION = "2";
  public static String CONFIG_V2_VERSION = CONFIG_V2_MAJOR_VERSION + ".0.0"; // "2.0.0"

  /**
   * Throw {@link IllegalStateException} iff the given input contains fields unknown to the {@link
   * com.google.api.codegen.v2.ConfigProto} schema.
   */
  public void validateV2Config(@Nonnull com.google.api.codegen.ConfigProto configV1Proto)
      throws IllegalStateException {
    if (!configV1Proto.getConfigSchemaVersion().startsWith(CONFIG_V2_MAJOR_VERSION + ".")
        && !configV1Proto.getConfigSchemaVersion().equals(CONFIG_V2_MAJOR_VERSION)) {
      throw new IllegalStateException(
          String.format(
              "Provided ConfigProto version is %s but should be >= %s",
              configV1Proto.getConfigSchemaVersion(), CONFIG_V2_VERSION));
    }

    try {
      // Serializing utils for Config V2.
      TypeRegistry typeRegistryV2 =
          JsonFormat.TypeRegistry.newBuilder()
              .add(com.google.api.codegen.v2.ConfigProto.getDescriptor())
              .build();
      Parser parserV2 = JsonFormat.parser().usingTypeRegistry(typeRegistryV2);
      Printer printerV2 = JsonFormat.printer().usingTypeRegistry(typeRegistryV2);

      // Serialize and deserialize the Config v1 proto under the Config v2 schema to remove fields
      // unknown to Config v2 schema.
      com.google.api.codegen.v2.ConfigProto.Builder v2ProtoBuilder =
          com.google.api.codegen.v2.ConfigProto.newBuilder();
      parserV2.merge(printerV2.print(configV1Proto), v2ProtoBuilder);

      // Serializing utils for Config V2.
      TypeRegistry typeRegistryV1 =
          JsonFormat.TypeRegistry.newBuilder()
              .add(com.google.api.codegen.ConfigProto.getDescriptor())
              .build();
      Printer printerV1 = JsonFormat.printer().usingTypeRegistry(typeRegistryV1);

      String v1String = printerV2.print(v2ProtoBuilder.build());
      String v2String = printerV1.print(configV1Proto);

      // Compare the v1-serialized and v2-serialized strings of the same config proto object.
      if (!v1String.equals(v2String)) {
        throw new IllegalStateException(
            String.format(
                "Unknown fields in to ConfigProto v2 in configProto: %s",
                configV1Proto.toString()));
      }
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException(e);
    }
  }
}

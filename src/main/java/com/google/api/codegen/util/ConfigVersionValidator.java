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

import com.google.api.tools.framework.model.testing.BaselineDiffer;
import com.google.protobuf.DiscardUnknownFieldsParser;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Parser;
import java.util.Arrays;

public class ConfigVersionValidator {

  public static String CONFIG_V2_MAJOR_VERSION = "2";
  public static String CONFIG_V2_VERSION = CONFIG_V2_MAJOR_VERSION + ".0.0"; // "2.0.0"

  /**
   * Throw {@link IllegalStateException} iff the given input contains fields unknown to the {@link
   * com.google.api.codegen.v2.ConfigProto} schema. Do nothing if input is null.
   */
  public void validateV2Config(com.google.api.codegen.ConfigProto configV1Proto)
      throws IllegalStateException {
    if (!isV2Config(configV1Proto)) {
      throw new IllegalStateException(
          String.format(
              "Provided ConfigProto version is %s but should be >= %s",
              configV1Proto.getConfigSchemaVersion(), CONFIG_V2_VERSION));
    }
    if (configV1Proto == null) {
      return;
    }

    try {
      // Serialize and deserialize the Config v1 proto under the Config v2 schema to remove fields
      // unknown to Config v2 schema.
      Parser<com.google.api.codegen.v2.ConfigProto> parser =
          DiscardUnknownFieldsParser.wrap(com.google.api.codegen.v2.ConfigProto.parser());
      com.google.api.codegen.v2.ConfigProto configV2 =
          parser.parseFrom(configV1Proto.toByteString());

      // Compare the v1-serialized and v2-serialized strings of the same config proto object.
      if (!Arrays.equals(configV2.toByteArray(), configV1Proto.toByteArray())) {
        BaselineDiffer differ = new BaselineDiffer();
        throw new IllegalStateException(
            String.format(
                "Unknown fields to ConfigProto v2 in configProto:\n%s",
                differ.diff(configV1Proto.toString(), configV2.toString())));
      }
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException(e);
    }
  }

  public boolean isV2Config(com.google.api.codegen.ConfigProto configV1Proto) {
    return configV1Proto == null
        || configV1Proto.getConfigSchemaVersion().startsWith(CONFIG_V2_MAJOR_VERSION + ".");
  }
}

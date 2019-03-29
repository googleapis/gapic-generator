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

import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.FlatteningConfigProto;
import com.google.api.codegen.FlatteningGroupProto;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.protobuf.BoolValue;
import org.junit.Test;

public class ConfigValidatorTest {
  private static final ConfigProto validV2Proto =
      ConfigProto.newBuilder()
          .setConfigSchemaVersion("2.0.0")
          // This is a valid field for both Config v1 and Config v2.
          .setEnableStringFormatFunctionsOverride(BoolValue.of(true))
          .build();

  private static final ConfigVersionValidator validator = new ConfigVersionValidator();

  @Test(expected = IllegalStateException.class)
  public void testProtoIsNotConfigNextVersion() {
    ConfigProto smallProto = validV2Proto.toBuilder().setConfigSchemaVersion("1.0.0").build();
    validator.checkIsNextVersionConfig(smallProto);
  }

  @Test(expected = IllegalStateException.class)
  public void testProtoIsNotValid() {
    ConfigProto smallProto =
        validV2Proto
            .toBuilder()
            .addInterfaces(
                InterfaceConfigProto.newBuilder()
                    .addMethods(
                        MethodConfigProto.newBuilder()
                            .setFlattening(
                                FlatteningConfigProto.newBuilder()
                                    .addGroups(
                                        FlatteningGroupProto.newBuilder()
                                            .addParameters("flattenedParam")
                                            .build())
                                    .build()))
                    .build())
            .build();
    validator.checkIsNextVersionConfig(smallProto);
  }

  @Test
  public void testProtoIsValid() {
    validator.checkIsNextVersionConfig(validV2Proto);
  }
}

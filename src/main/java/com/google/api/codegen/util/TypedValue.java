/* Copyright 2016 Google LLC
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

import com.google.auto.value.AutoValue;
import org.apache.commons.lang3.StringUtils;

/** Represents a value and its associated type. */
@AutoValue
public abstract class TypedValue {

  /**
   * Create a TypedValue. The pattern "%s" in valuePattern is used by getValueAndSaveTypeNicknameIn
   * to format the final value.
   */
  public static TypedValue create(TypeName typeName, String valuePattern) {
    return new AutoValue_TypedValue(typeName, valuePattern);
  }

  public abstract TypeName getTypeName();

  public abstract String getValuePattern();

  /**
   * Renders the value given the value pattern, and adds any necessary nicknames to the given type
   * table.
   */
  public String getValueAndSaveTypeNicknameIn(TypeTable typeTable) {
    if (getValuePattern().contains("%s")) {
      String nickname = typeTable.getAndSaveNicknameFor(getTypeName());
      return StringUtils.replaceOnce(getValuePattern(), "%s", nickname);
    } else {
      return getValuePattern();
    }
  }
}

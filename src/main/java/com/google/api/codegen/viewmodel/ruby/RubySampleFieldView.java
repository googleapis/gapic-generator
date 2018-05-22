/* Copyright 2018 Google LLC
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
package com.google.api.codegen.viewmodel.ruby;

import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.util.Name;

public class RubySampleFieldView {

  private final InitCodeNode item;

  private final boolean isRequired;

  public static RubySampleFieldView createRequired(InitCodeNode item) {
    return new RubySampleFieldView(item, true);
  }

  public static RubySampleFieldView createOptional(InitCodeNode item) {
    return new RubySampleFieldView(item, false);
  }

  private RubySampleFieldView(InitCodeNode item, boolean isRequired) {
    this.item = item;
    this.isRequired = isRequired;
  }

  public String name() {
    return item.getVarName();
  }

  public String identifier() {
    return Name.from(item.getInitValueConfig().hasFormattingConfig() ? "formatted" : "")
        .join(item.getIdentifier())
        .toLowerUnderscore();
  }

  public boolean isRequired() {
    return isRequired;
  }
}

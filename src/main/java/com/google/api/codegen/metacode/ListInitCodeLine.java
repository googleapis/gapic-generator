/* Copyright 2016 Google Inc
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
package com.google.api.codegen.metacode;

import com.google.api.codegen.util.Name;
import com.google.api.tools.framework.model.TypeRef;
import com.google.auto.value.AutoValue;
import java.util.List;

/**
 * ListInitCodeLine represents an InitCodeLine that initializes a list with values from other
 * variables.
 */
@AutoValue
public abstract class ListInitCodeLine implements InitCodeLine {

  public static ListInitCodeLine create(
      TypeRef elementType, Name identifier, List<Name> elementIdentifiers) {
    return new AutoValue_ListInitCodeLine(elementType, identifier, elementIdentifiers);
  }

  public abstract TypeRef getElementType();

  @Override
  public abstract Name getIdentifier();

  public abstract List<Name> getElementIdentifiers();

  @Override
  public InitCodeLineType getLineType() {
    return InitCodeLineType.ListInitLine;
  }

  @Override
  public InitValueConfig getInitValueConfig() {
    return InitValueConfig.create();
  }
}

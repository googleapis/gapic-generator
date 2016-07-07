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
package com.google.api.codegen.transformer;

import com.google.api.codegen.CollectionConfig;
import com.google.api.codegen.LanguageUtil;
import com.google.api.codegen.metacode.InitValueConfig;
import com.google.api.tools.framework.model.TypeRef;

public class JavaIdentifierNamer implements IdentifierNamer {
  @Override
  public String getVariableName(String identifier, InitValueConfig initValueConfig) {
    if (initValueConfig.hasFormattingConfig()) {
      return "formatted" + LanguageUtil.lowerUnderscoreToUpperCamel(identifier);
    } else {
      return LanguageUtil.lowerUnderscoreToLowerCamel(identifier);
    }
  }

  @Override
  public String getSetFunctionCallName(TypeRef type, String identifier) {
    return ModelToJavaSurfaceTransformer.getSetFunctionCallName(type, identifier);
  }

  @Override
  public String getPathTemplateName(CollectionConfig collectionConfig) {
    return LanguageUtil.lowerUnderscoreToUpperUnderscore(collectionConfig.getEntityName())
        + "_PATH_TEMPLATE";
  }
}

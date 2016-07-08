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

import org.apache.commons.lang3.NotImplementedException;

public class PhpIdentifierNamer implements IdentifierNamer {

  @Override
  public String getVariableName(String identifier, InitValueConfig initValueConfig) {
    throw new NotImplementedException("PhpIdentifierNamer.getVariableName");
  }

  @Override
  public String getSetFunctionCallName(TypeRef type, String fieldName) {
    throw new NotImplementedException("PhpIdentifierNamer.getSetFunctionCallName");
  }

  @Override
  public String getPathTemplateName(CollectionConfig collectionConfig) {
    return LanguageUtil.lowerUnderscoreToLowerCamel(collectionConfig.getEntityName())
        + "NameTemplate";
  }

  @Override
  public String getPathTemplateNameGetter(CollectionConfig collectionConfig) {
    return "get"
        + LanguageUtil.lowerUnderscoreToUpperCamel(collectionConfig.getEntityName())
        + "NameTemplate";
  }

  @Override
  public String getFormatFunctionName(CollectionConfig collectionConfig) {
    return "format"
        + LanguageUtil.lowerUnderscoreToUpperCamel(collectionConfig.getEntityName())
        + "Name";
  }
}

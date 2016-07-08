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
import com.google.api.codegen.metacode.InitValueConfig;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.TypeRef;

public interface IdentifierNamer {
  public static final String NOT_IMPLEMENTED = "$ NOT IMPLEMENTED $";

  String getApiWrapperClassName(Interface interfaze);

  String getVariableName(String identifier, InitValueConfig initValueConfig);

  String getSetFunctionCallName(TypeRef type, String fieldName);

  String getPathTemplateName(CollectionConfig collectionConfig);

  String getPathTemplateNameGetter(CollectionConfig collectionConfig);

  String getFormatFunctionName(CollectionConfig collectionConfig);

  String getParseFunctionName(String var, CollectionConfig collectionConfig);

  String getEntityName(CollectionConfig collectionConfig);

  String getEntityNameParamName(CollectionConfig collectionConfig);

  String getParamName(String var);
}

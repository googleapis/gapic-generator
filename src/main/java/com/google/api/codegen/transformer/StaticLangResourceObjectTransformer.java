/* Copyright 2017 Google LLC
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

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.viewmodel.FieldCopyView;
import com.google.api.codegen.viewmodel.RequestObjectParamView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Transforms request objects to ViewModels form. */
public class StaticLangResourceObjectTransformer {

  public RequestObjectParamView generateRequestObjectParam(
      MethodContext context, FieldConfig fieldConfig) {
    SurfaceNamer namer = context.getNamer();
    FeatureConfig featureConfig = context.getFeatureConfig();
    ImportTypeTable typeTable = context.getTypeTable();
    FieldModel field = fieldConfig.getField();

    Iterable<FieldModel> requiredFields = context.getMethodConfig().getRequiredFields();
    boolean isRequired = false;
    for (FieldModel f : requiredFields) {
      if (f.getSimpleName().equals(field.getSimpleName())) {
        isRequired = true;
        break;
      }
    }

    String typeName =
        namer.getNotImplementedString(
            "StaticLangApiMethodTransformer.generateRequestObjectParam - typeName");
    String elementTypeName =
        namer.getNotImplementedString(
            "StaticLangApiMethodTransformer.generateRequestObjectParam - elementTypeName");

    if (context.getFeatureConfig().useResourceNameFormatOption(fieldConfig)) {
      if (namer.shouldImportRequestObjectParamType(field)) {
        typeName = namer.getAndSaveResourceTypeName(typeTable, fieldConfig);
      }
      if (namer.shouldImportRequestObjectParamElementType(field)) {
        // Use makeOptional to remove repeated property from type
        elementTypeName = namer.getAndSaveElementResourceTypeName(typeTable, fieldConfig);
      }
    } else {
      if (namer.shouldImportRequestObjectParamType(field)) {
        typeName = typeTable.getAndSaveNicknameFor(field);
        if (!isRequired) {
          typeName = namer.makePrimitiveTypeNullable(typeName, field);
        }
      }
      if (namer.shouldImportRequestObjectParamElementType(field)) {
        elementTypeName = typeTable.getAndSaveNicknameForElementType(field);
      }
    }

    String setCallName = namer.getFieldSetFunctionName(featureConfig, fieldConfig);
    String addCallName = namer.getFieldAddFunctionName(field);
    String getCallName = namer.getFieldGetFunctionName(field);
    String transformParamFunctionName = null;
    if (context.getFeatureConfig().useResourceNameFormatOption(fieldConfig)
        && fieldConfig.requiresParamTransformation()) {
      if (!fieldConfig.requiresParamTransformationFromAny()) {
        transformParamFunctionName = namer.getResourceOneofCreateMethod(typeTable, fieldConfig);
      }
    }

    RequestObjectParamView.Builder param = RequestObjectParamView.newBuilder();
    param.name(namer.getVariableName(field));
    param.keyName(namer.getFieldKey(field));
    param.nameAsMethodName(namer.getFieldGetFunctionName(featureConfig, fieldConfig));
    param.typeName(typeName);
    param.elementTypeName(elementTypeName);
    param.setCallName(setCallName);
    param.addCallName(addCallName);
    param.getCallName(getCallName);
    param.transformParamFunctionName(transformParamFunctionName);
    param.isMap(field.isMap());
    param.isArray(!field.isMap() && field.isRepeated());
    param.isPrimitive(namer.isPrimitive(field));
    param.isOptional(!isRequired);
    if (!isRequired) {
      param.optionalDefault(namer.getOptionalFieldDefaultValue(fieldConfig, context));
    }
    List<FieldCopyView> fieldCopyViews = new ArrayList<>();
    for (FieldModel child : context.getMethodModel().getResourceNameInputFields()) {
      FieldCopyView.Builder fieldCopy = FieldCopyView.newBuilder();
      fieldCopy.fieldGetFunction(namer.getFieldGetFunctionName(child));
      fieldCopy.fieldSetFunction(namer.getFieldSetFunctionName(child));
      fieldCopyViews.add(fieldCopy.build());
    }
    Collections.sort(fieldCopyViews);
    param.fieldCopyMethods(fieldCopyViews);

    return param.build();
  }
}

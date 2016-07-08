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
import com.google.api.codegen.surface.SurfaceFormatResourceFunction;
import com.google.api.codegen.surface.SurfaceParseResourceFunction;
import com.google.api.codegen.surface.SurfacePathTemplate;
import com.google.api.codegen.surface.SurfacePathTemplateGetterFunction;
import com.google.api.codegen.surface.SurfaceResourceIdParam;

import java.util.ArrayList;
import java.util.List;

public class CommonTransformer {

  public List<SurfacePathTemplate> generatePathTemplates(ModelToSurfaceContext context) {
    List<SurfacePathTemplate> pathTemplates = new ArrayList<>();

    for (CollectionConfig collectionConfig : context.getCollectionConfigs()) {
      SurfacePathTemplate pathTemplate = new SurfacePathTemplate();
      pathTemplate.name = context.getNamer().getPathTemplateName(collectionConfig);
      pathTemplate.pattern = collectionConfig.getNamePattern();
      pathTemplates.add(pathTemplate);
    }

    return pathTemplates;
  }

  public List<SurfaceFormatResourceFunction> generateFormatResourceFunctions(
      ModelToSurfaceContext context) {
    List<SurfaceFormatResourceFunction> functions = new ArrayList<>();

    for (CollectionConfig collectionConfig : context.getCollectionConfigs()) {
      SurfaceFormatResourceFunction function = new SurfaceFormatResourceFunction();
      function.entityName = collectionConfig.getEntityName();
      function.name = context.getNamer().getFormatFunctionName(collectionConfig);
      function.pathTemplateName = context.getNamer().getPathTemplateName(collectionConfig);
      function.pathTemplateGetterName =
          context.getNamer().getPathTemplateNameGetter(collectionConfig);
      List<SurfaceResourceIdParam> resourceIdParams = new ArrayList<>();
      for (String var : collectionConfig.getNameTemplate().vars()) {
        SurfaceResourceIdParam param = new SurfaceResourceIdParam();
        param.name = LanguageUtil.lowerUnderscoreToLowerCamel(var);
        param.templateKey = var;
        resourceIdParams.add(param);
      }
      function.resourceIdParams = resourceIdParams;

      functions.add(function);
    }

    return functions;
  }

  public List<SurfaceParseResourceFunction> generateParseResourceFunctions(
      ModelToSurfaceContext context) {
    List<SurfaceParseResourceFunction> functions = new ArrayList<>();

    for (CollectionConfig collectionConfig : context.getCollectionConfigs()) {
      for (String var : collectionConfig.getNameTemplate().vars()) {
        SurfaceParseResourceFunction function = new SurfaceParseResourceFunction();
        function.entityName =
            LanguageUtil.lowerUnderscoreToLowerCamel(collectionConfig.getEntityName());
        function.name =
            "parse"
                + LanguageUtil.lowerUnderscoreToUpperCamel(var)
                + "From"
                + LanguageUtil.lowerUnderscoreToUpperCamel(collectionConfig.getEntityName())
                + "Name";
        function.pathTemplateName = context.getNamer().getPathTemplateName(collectionConfig);
        function.pathTemplateGetterName =
            context.getNamer().getPathTemplateNameGetter(collectionConfig);
        function.entityNameParamName = function.entityName + "Name";
        function.outputResourceId = var;

        functions.add(function);
      }
    }

    return functions;
  }

  public List<SurfacePathTemplateGetterFunction> generatePathTemplateGetterFunctions(
      ModelToSurfaceContext context) {
    List<SurfacePathTemplateGetterFunction> functions = new ArrayList<>();

    for (CollectionConfig collectionConfig : context.getCollectionConfigs()) {
      SurfacePathTemplateGetterFunction function = new SurfacePathTemplateGetterFunction();
      function.name = context.getNamer().getPathTemplateNameGetter(collectionConfig);
      function.pathTemplateName = context.getNamer().getPathTemplateName(collectionConfig);
      function.pattern = collectionConfig.getNamePattern();
      functions.add(function);
    }

    return functions;
  }
}

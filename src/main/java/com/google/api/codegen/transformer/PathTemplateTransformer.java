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

import com.google.api.codegen.config.CollectionConfig;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.FormatResourceFunctionView;
import com.google.api.codegen.viewmodel.ParseResourceFunctionView;
import com.google.api.codegen.viewmodel.PathTemplateArgumentView;
import com.google.api.codegen.viewmodel.PathTemplateGetterFunctionView;
import com.google.api.codegen.viewmodel.PathTemplateView;
import com.google.api.codegen.viewmodel.ResourceIdParamView;
import com.google.api.tools.framework.model.Interface;
import java.util.ArrayList;
import java.util.List;

/** PathTemplateTransformer generates view objects for path templates from a service model. */
public class PathTemplateTransformer {

  public List<PathTemplateView> generatePathTemplates(SurfaceTransformerContext context) {
    List<PathTemplateView> pathTemplates = new ArrayList<>();

    for (CollectionConfig collectionConfig : context.getCollectionConfigs()) {
      PathTemplateView.Builder pathTemplate = PathTemplateView.newBuilder();
      pathTemplate.name(
          context.getNamer().getPathTemplateName(context.getInterface(), collectionConfig));
      pathTemplate.pattern(collectionConfig.getNamePattern());
      pathTemplates.add(pathTemplate.build());
    }

    return pathTemplates;
  }

  public List<FormatResourceFunctionView> generateFormatResourceFunctions(
      SurfaceTransformerContext context) {
    List<FormatResourceFunctionView> functions = new ArrayList<>();

    SurfaceNamer namer = context.getNamer();
    Interface service = context.getInterface();
    for (CollectionConfig collectionConfig : context.getCollectionConfigs()) {
      FormatResourceFunctionView.Builder function = FormatResourceFunctionView.newBuilder();
      function.entityName(collectionConfig.getEntityName());
      function.name(namer.getFormatFunctionName(collectionConfig));
      function.pathTemplateName(namer.getPathTemplateName(service, collectionConfig));
      function.pathTemplateGetterName(namer.getPathTemplateNameGetter(service, collectionConfig));
      function.pattern(collectionConfig.getNamePattern());
      List<ResourceIdParamView> resourceIdParams = new ArrayList<>();
      for (String var : collectionConfig.getNameTemplate().vars()) {
        ResourceIdParamView param =
            ResourceIdParamView.newBuilder()
                .name(namer.getParamName(var))
                .docName(namer.getParamDocName(var))
                .templateKey(var)
                .build();
        resourceIdParams.add(param);
      }
      function.resourceIdParams(resourceIdParams);

      functions.add(function.build());
    }

    return functions;
  }

  public List<ParseResourceFunctionView> generateParseResourceFunctions(
      SurfaceTransformerContext context) {
    List<ParseResourceFunctionView> functions = new ArrayList<>();

    SurfaceNamer namer = context.getNamer();
    Interface service = context.getInterface();
    for (CollectionConfig collectionConfig : context.getCollectionConfigs()) {
      for (String var : collectionConfig.getNameTemplate().vars()) {
        ParseResourceFunctionView.Builder function = ParseResourceFunctionView.newBuilder();
        function.entityName(collectionConfig.getEntityName());
        function.name(namer.getParseFunctionName(var, collectionConfig));
        function.pathTemplateName(namer.getPathTemplateName(service, collectionConfig));
        function.pathTemplateGetterName(namer.getPathTemplateNameGetter(service, collectionConfig));
        function.entityNameParamName(namer.getEntityNameParamName(collectionConfig));
        function.outputResourceId(var);

        functions.add(function.build());
      }
    }

    return functions;
  }

  public List<PathTemplateGetterFunctionView> generatePathTemplateGetterFunctions(
      SurfaceTransformerContext context) {
    List<PathTemplateGetterFunctionView> functions = new ArrayList<>();

    SurfaceNamer namer = context.getNamer();
    Interface service = context.getInterface();
    for (CollectionConfig collectionConfig : context.getCollectionConfigs()) {
      PathTemplateGetterFunctionView.Builder function = PathTemplateGetterFunctionView.newBuilder();
      function.name(namer.getPathTemplateNameGetter(service, collectionConfig));
      function.resourceName(namer.getPathTemplateResourcePhraseName(collectionConfig));
      function.pathTemplateName(namer.getPathTemplateName(service, collectionConfig));
      function.pattern(collectionConfig.getNamePattern());

      List<PathTemplateArgumentView> args = new ArrayList<>();
      for (String templateKey : collectionConfig.getNameTemplate().vars()) {
        String name = context.getNamer().localVarName(Name.from(templateKey));
        args.add(PathTemplateArgumentView.newBuilder().templateKey(templateKey).name(name).build());
      }
      function.args(args);
      functions.add(function.build());
    }

    return functions;
  }
}

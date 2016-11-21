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

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FixedResourceNameConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.ResourceNameConfig;
import com.google.api.codegen.config.ResourceNameMessageConfigs;
import com.google.api.codegen.config.ResourceNameOneofConfig;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.FormatResourceFunctionView;
import com.google.api.codegen.viewmodel.ParseResourceFunctionView;
import com.google.api.codegen.viewmodel.PathTemplateArgumentView;
import com.google.api.codegen.viewmodel.PathTemplateGetterFunctionView;
import com.google.api.codegen.viewmodel.PathTemplateView;
import com.google.api.codegen.viewmodel.ResourceIdParamView;
import com.google.api.codegen.viewmodel.ResourceNameFixedView;
import com.google.api.codegen.viewmodel.ResourceNameOneofView;
import com.google.api.codegen.viewmodel.ResourceNameParamView;
import com.google.api.codegen.viewmodel.ResourceNameSingleView;
import com.google.api.codegen.viewmodel.ResourceNameView;
import com.google.api.codegen.viewmodel.ResourceProtoFieldView;
import com.google.api.codegen.viewmodel.ResourceProtoView;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/** PathTemplateTransformer generates view objects for path templates from a service model. */
public class PathTemplateTransformer {

  public List<PathTemplateView> generatePathTemplates(SurfaceTransformerContext context) {
    List<PathTemplateView> pathTemplates = new ArrayList<>();

    InterfaceConfig interfaceConfig = context.getInterfaceConfig();
    for (SingleResourceNameConfig resourceNameConfig :
        interfaceConfig.getSingleResourceNameConfigs()) {
      PathTemplateView.Builder pathTemplate = PathTemplateView.newBuilder();
      pathTemplate.name(
          context.getNamer().getPathTemplateName(context.getInterface(), resourceNameConfig));
      pathTemplate.pattern(resourceNameConfig.getNamePattern());
      pathTemplates.add(pathTemplate.build());
    }

    return pathTemplates;
  }

  public List<ResourceNameView> generateResourceNames(SurfaceTransformerContext context) {
    return generateResourceNames(context, context.getApiConfig().getResourceNameConfigs().values());
  }

  public List<ResourceNameView> generateResourceNames(
      SurfaceTransformerContext context, Iterable<ResourceNameConfig> configs) {
    List<ResourceNameView> resourceNames = new ArrayList<>();
    int index = 1;
    for (ResourceNameConfig config : configs) {
      switch (config.getResourceNameType()) {
        case SINGLE:
          resourceNames.add(
              generateResourceNameSingle(context, index, (SingleResourceNameConfig) config));
          break;
        case ONEOF:
          resourceNames.add(
              generateResourceNameOneof(context, index, (ResourceNameOneofConfig) config));
          break;
        case FIXED:
          resourceNames.add(
              generateResourceNameFixed(context, index, (FixedResourceNameConfig) config));
          break;
        default:
          throw new IllegalStateException("Unexpected resource-name type.");
      }
      index += 1;
    }
    return resourceNames;
  }

  private ResourceNameSingleView generateResourceNameSingle(
      SurfaceTransformerContext context, int index, SingleResourceNameConfig config) {
    SurfaceNamer namer = context.getNamer();
    ResourceNameSingleView.Builder builder =
        ResourceNameSingleView.newBuilder()
            .typeName(namer.getResourceTypeName(config))
            .paramName(namer.getResourceParameterName(config))
            .propertyName(namer.getResourcePropertyName(config))
            .enumName(namer.getResourceEnumName(config))
            .docName(config.getEntityName())
            .index(index)
            .pattern(config.getNamePattern());
    List<ResourceNameParamView> params = new ArrayList<>();
    int varIndex = 0;
    for (String var : config.getNameTemplate().vars()) {
      ResourceNameParamView.Builder paramBuilder = ResourceNameParamView.newBuilder();
      paramBuilder.index(varIndex++);
      paramBuilder.nameAsParam(namer.getParamName(var));
      paramBuilder.nameAsProperty(namer.getPropertyName(var));
      paramBuilder.docName(namer.getParamDocName(var));
      params.add(paramBuilder.build());
    }
    builder.params(params);
    return builder.build();
  }

  private ResourceNameOneofView generateResourceNameOneof(
      SurfaceTransformerContext context, int index, ResourceNameOneofConfig config) {
    SurfaceNamer namer = context.getNamer();
    ResourceNameOneofView.Builder builder = ResourceNameOneofView.newBuilder();
    builder.typeName(namer.getResourceTypeName(config));
    builder.paramName(namer.getResourceParameterName(config));
    builder.propertyName(namer.getResourcePropertyName(config));
    builder.enumName(namer.getResourceEnumName(config));
    builder.docName(config.getEntityName());
    builder.index(index);
    builder.children(generateResourceNames(context, config.getResourceNameConfigs()));
    return builder.build();
  }

  private ResourceNameFixedView generateResourceNameFixed(
      SurfaceTransformerContext context, int index, FixedResourceNameConfig config) {
    SurfaceNamer namer = context.getNamer();
    ResourceNameFixedView.Builder builder = ResourceNameFixedView.newBuilder();
    builder.typeName(namer.getResourceTypeName(config));
    builder.paramName(namer.getResourceParameterName(config));
    builder.propertyName(namer.getResourcePropertyName(config));
    builder.enumName(namer.getResourceEnumName(config));
    builder.docName(config.getEntityName());
    builder.index(index);
    builder.value(config.getFixedValue());
    return builder.build();
  }

  public List<ResourceProtoView> generateResourceProtos(SurfaceTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    ResourceNameMessageConfigs resourceConfigs =
        context.getApiConfig().getResourceNameMessageConfigs();
    Map<String, ResourceNameConfig> resourceNameConfigs =
        context.getApiConfig().getResourceNameConfigs();
    ListMultimap<String, Field> fieldsByMessage = ArrayListMultimap.create();
    for (ProtoFile protoFile : context.getModel().getFiles()) {
      for (MessageType msg : protoFile.getMessages()) {
        for (Field field : msg.getFields()) {
          if (resourceConfigs.fieldHasResourceName(field)) {
            fieldsByMessage.put(msg.getFullName(), field);
          }
        }
      }
    }
    List<ResourceProtoView> protos = new ArrayList<>();
    for (Entry<String, Collection<Field>> entry : fieldsByMessage.asMap().entrySet()) {
      String msgName = entry.getKey();
      Collection<Field> fields = new ArrayList<Field>(entry.getValue());
      ResourceProtoView.Builder protoBuilder = ResourceProtoView.newBuilder();
      protoBuilder.protoClassName(namer.getTypeNameConverter().getTypeName(msgName).getNickname());
      List<ResourceProtoFieldView> fieldViews = new ArrayList<>();
      for (Field field : fields) {
        String fieldName = field.getSimpleName();
        FieldConfig fieldConfig = FieldConfig.createDefaultFieldConfig(field);
        String fieldResourceName = resourceConfigs.getFieldResourceName(field);
        String fieldTypeName;
        String fieldElementTypeName;
        if (fieldResourceName.equals("*")) {
          fieldTypeName = "IResourceName";
          fieldElementTypeName = "IResourceName";
        } else {
          ResourceNameConfig resourceNameConfig = resourceNameConfigs.get(fieldResourceName);
          String fieldTypeSimpleName = namer.getResourceTypeName(resourceNameConfig);
          fieldTypeName =
              context
                  .getTypeTable()
                  .getAndSaveNicknameForTypedResourceName(fieldConfig, fieldTypeSimpleName);
          fieldElementTypeName =
              context
                  .getTypeTable()
                  .getAndSaveNicknameForResourceNameElementType(fieldConfig, fieldTypeSimpleName);
        }
        ResourceProtoFieldView fieldView =
            ResourceProtoFieldView.newBuilder()
                .typeName(fieldTypeName)
                .elementTypeName(fieldElementTypeName)
                .isAny(fieldResourceName.equals("*"))
                .isRepeated(field.getType().isRepeated())
                .propertyName(namer.getResourceNameFieldGetFunctionName(fieldConfig))
                .underlyingPropertyName(namer.publicMethodName(Name.from(fieldName)))
                .build();
        fieldViews.add(fieldView);
      }
      protoBuilder.fields(fieldViews);
      protos.add(protoBuilder.build());
    }
    return protos;
  }

  public List<FormatResourceFunctionView> generateFormatResourceFunctions(
      SurfaceTransformerContext context) {
    List<FormatResourceFunctionView> functions = new ArrayList<>();

    SurfaceNamer namer = context.getNamer();
    Interface service = context.getInterface();
    InterfaceConfig interfaceConfig = context.getInterfaceConfig();
    for (SingleResourceNameConfig resourceNameConfig :
        interfaceConfig.getSingleResourceNameConfigs()) {
      FormatResourceFunctionView.Builder function = FormatResourceFunctionView.newBuilder();
      function.entityName(resourceNameConfig.getEntityName());
      function.name(namer.getFormatFunctionName(resourceNameConfig));
      function.pathTemplateName(namer.getPathTemplateName(service, resourceNameConfig));
      function.pathTemplateGetterName(namer.getPathTemplateNameGetter(service, resourceNameConfig));
      function.pattern(resourceNameConfig.getNamePattern());
      List<ResourceIdParamView> resourceIdParams = new ArrayList<>();
      for (String var : resourceNameConfig.getNameTemplate().vars()) {
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
    InterfaceConfig interfaceConfig = context.getInterfaceConfig();
    for (SingleResourceNameConfig resourceNameConfig :
        interfaceConfig.getSingleResourceNameConfigs()) {
      for (String var : resourceNameConfig.getNameTemplate().vars()) {
        ParseResourceFunctionView.Builder function = ParseResourceFunctionView.newBuilder();
        function.entityName(resourceNameConfig.getEntityName());
        function.name(namer.getParseFunctionName(var, resourceNameConfig));
        function.pathTemplateName(namer.getPathTemplateName(service, resourceNameConfig));
        function.pathTemplateGetterName(
            namer.getPathTemplateNameGetter(service, resourceNameConfig));
        function.entityNameParamName(namer.getEntityNameParamName(resourceNameConfig));
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
    InterfaceConfig interfaceConfig = context.getInterfaceConfig();
    for (SingleResourceNameConfig resourceNameConfig :
        interfaceConfig.getSingleResourceNameConfigs()) {
      PathTemplateGetterFunctionView.Builder function = PathTemplateGetterFunctionView.newBuilder();
      function.name(namer.getPathTemplateNameGetter(service, resourceNameConfig));
      function.resourceName(namer.getPathTemplateResourcePhraseName(resourceNameConfig));
      function.pathTemplateName(namer.getPathTemplateName(service, resourceNameConfig));
      function.pattern(resourceNameConfig.getNamePattern());

      List<PathTemplateArgumentView> args = new ArrayList<>();
      for (String templateKey : resourceNameConfig.getNameTemplate().vars()) {
        String name = context.getNamer().localVarName(Name.from(templateKey));
        args.add(PathTemplateArgumentView.newBuilder().templateKey(templateKey).name(name).build());
      }
      function.args(args);
      functions.add(function.build());
    }

    return functions;
  }
}

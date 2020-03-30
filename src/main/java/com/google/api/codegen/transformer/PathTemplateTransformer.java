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
package com.google.api.codegen.transformer;

import com.google.api.codegen.config.FixedResourceNameConfig;
import com.google.api.codegen.config.GapicInterfaceContext;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.InterfaceContext;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodContext;
import com.google.api.codegen.config.ResourceNameConfig;
import com.google.api.codegen.config.ResourceNameOneofConfig;
import com.google.api.codegen.config.ResourceNameType;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.viewmodel.FormatResourceFunctionView;
import com.google.api.codegen.viewmodel.ParseResourceFunctionView;
import com.google.api.codegen.viewmodel.PathTemplateGetterFunctionView;
import com.google.api.codegen.viewmodel.PathTemplateView;
import com.google.api.codegen.viewmodel.ResourceIdParamView;
import com.google.api.codegen.viewmodel.ResourceNameFixedView;
import com.google.api.codegen.viewmodel.ResourceNameOneofView;
import com.google.api.codegen.viewmodel.ResourceNameParamView;
import com.google.api.codegen.viewmodel.ResourceNameSingleView;
import com.google.api.codegen.viewmodel.ResourceNameView;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/** PathTemplateTransformer generates view objects for path templates from a service model. */
public class PathTemplateTransformer {
  private static final String VAR_PLACE_HOLDER = "__GAPIC_VARIABLE__";

  public List<PathTemplateView> generatePathTemplates(InterfaceContext context) {
    List<PathTemplateView> pathTemplates = new ArrayList<>();
    if (!context.getFeatureConfig().enableStringFormatFunctions()) {
      return pathTemplates;
    }
    for (SingleResourceNameConfig resourceNameConfig :
        getSingleResourceNameConfigsUsedByInterface(context)) {
      PathTemplateView.Builder pathTemplate = PathTemplateView.newBuilder();
      pathTemplate.name(
          context.getNamer().getPathTemplateName(context.getInterfaceConfig(), resourceNameConfig));
      pathTemplate.pattern(resourceNameConfig.getNamePattern());
      pathTemplates.add(pathTemplate.build());
    }

    return pathTemplates;
  }

  private static List<SingleResourceNameConfig> getSingleResourceNameConfigsUsedByInterface(
      InterfaceContext context) {
    TreeMap<String, SingleResourceNameConfig> resources = new TreeMap<>();
    for (ResourceNameConfig resourceNameConfig : getResourceNameConfigsUsedByInterface(context)) {
      if (resourceNameConfig.getResourceNameType() == ResourceNameType.SINGLE) {
        resources.put(
            resourceNameConfig.getEntityId(), (SingleResourceNameConfig) resourceNameConfig);
      }
      if (resourceNameConfig.getResourceNameType() == ResourceNameType.ONEOF) {
        // Add SingleResourceNameConfigs derived from gapic config v1 and v2
        ResourceNameOneofConfig oneofConfig = (ResourceNameOneofConfig) resourceNameConfig;
        for (SingleResourceNameConfig resource : oneofConfig.getSingleResourceNameConfigs()) {
          resources.put(resource.getEntityId(), resource);
        }

        // Add SingleResourceNameConfigs derived from patterns next so that when collision,
        // those derived from patterns will win
        if (context.getFeatureConfig().enableStringFormatFunctionsForOneofs()) {
          for (SingleResourceNameConfig resource :
              oneofConfig.getPatternsAsSingleResourceNameConfigs()) {
            resources.put(resource.getEntityId(), resource);
          }
        }
        // Add the SingleResourceNameConfig derived from the first pattern last as if it is a
        // single-pattern resource name for backward-compatibility.
        // We need this for all dynamic languages.
        Optional<SingleResourceNameConfig> firstPattern =
            oneofConfig.getFirstPatternAsSingleResourceNameConfig();
        if (firstPattern.isPresent()) {
          resources.put(firstPattern.get().getEntityId(), firstPattern.get());
        }
      }
    }
    return ImmutableList.copyOf(resources.values());
  }

  private static List<ResourceNameConfig> getResourceNameConfigsUsedByInterface(
      InterfaceContext context) {
    InterfaceConfig interfaceConfig = context.getInterfaceConfig();
    Set<String> foundSet = new HashSet<>();
    List<ResourceNameConfig> resourceNameConfigs = new ArrayList<>();
    for (SingleResourceNameConfig config : interfaceConfig.getSingleResourceNameConfigs()) {
      resourceNameConfigs.add(config);
      foundSet.add(config.getEntityId());
    }
    for (MethodConfig methodConfig : interfaceConfig.getMethodConfigs()) {
      MethodContext methodContext = context.asRequestMethodContext(methodConfig.getMethodModel());
      for (String fieldNamePattern : methodConfig.getFieldNamePatterns().values()) {
        addSingleResourceNameConfigsUsedByInterface(
            methodContext, fieldNamePattern, foundSet, resourceNameConfigs);
        addResourceNameOneofConfigsUsedByInterface(
            context, fieldNamePattern, foundSet, resourceNameConfigs);
      }
    }
    return resourceNameConfigs;
  }

  private static void addSingleResourceNameConfigsUsedByInterface(
      MethodContext methodContext,
      String fieldNamePattern,
      Set<String> foundSet,
      List<ResourceNameConfig> resourceNameConfigs) {
    SingleResourceNameConfig resourceNameConfig =
        methodContext.getSingleResourceNameConfig(fieldNamePattern);
    if (resourceNameConfig != null && !foundSet.contains(resourceNameConfig.getEntityId())) {
      resourceNameConfigs.add(resourceNameConfig);
      foundSet.add(resourceNameConfig.getEntityId());
    }
  }

  private static void addResourceNameOneofConfigsUsedByInterface(
      InterfaceContext context,
      String fieldNamePattern,
      Set<String> foundSet,
      List<ResourceNameConfig> resourceNameConfigs) {
    ResourceNameConfig resourceNameConfig =
        context.getProductConfig().getResourceNameConfigs().get(fieldNamePattern);
    if (resourceNameConfig != null
        && resourceNameConfig.getResourceNameType() == ResourceNameType.ONEOF) {
      ResourceNameOneofConfig oneofConfig = (ResourceNameOneofConfig) resourceNameConfig;

      if (!foundSet.contains(resourceNameConfig.getEntityId())) {
        resourceNameConfigs.add(resourceNameConfig);
        foundSet.add(resourceNameConfig.getEntityId());
      }
    }
  }

  public List<ResourceNameView> generateResourceNames(GapicInterfaceContext context) {
    return generateResourceNames(
        context, context.getProductConfig().getResourceNameConfigs().values());
  }

  List<ResourceNameView> generateResourceNames(
      InterfaceContext context, Iterable<ResourceNameConfig> configs) {
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
      InterfaceContext context, int index, SingleResourceNameConfig config) {
    SurfaceNamer namer = context.getNamer();
    ResourceNameSingleView.Builder builder =
        ResourceNameSingleView.newBuilder()
            .typeName(namer.getResourceTypeName(config))
            .paramName(namer.getResourceParameterName(config))
            .propertyName(namer.getResourcePropertyName(config))
            .enumName(namer.getResourceEnumName(config))
            .docName(config.getEntityName().toLowerUnderscore())
            .index(index)
            .pattern(config.getNamePattern())
            .commonResourceName(config.getCommonResourceName());
    List<ResourceNameParamView> params = new ArrayList<>();
    int varIndex = 0;
    for (String var : config.getNameTemplate().vars()) {
      ResourceNameParamView.Builder paramBuilder =
          ResourceNameParamView.newBuilder()
              .index(varIndex++)
              .nameAsParam(namer.getParamName(var))
              .nameAsProperty(namer.getPropertyName(var))
              .docName(namer.getParamDocName(var));
      params.add(paramBuilder.build());
    }
    builder.params(params);
    return builder.build();
  }

  private ResourceNameOneofView generateResourceNameOneof(
      InterfaceContext context, int index, ResourceNameOneofConfig config) {
    SurfaceNamer namer = context.getNamer();
    ResourceNameOneofView.Builder builder =
        ResourceNameOneofView.newBuilder()
            .typeName(namer.getResourceTypeName(config))
            .paramName(namer.getResourceParameterName(config))
            .propertyName(namer.getResourcePropertyName(config))
            .enumName(namer.getResourceEnumName(config))
            .docName(config.getEntityName().toLowerUnderscore())
            .index(index)
            .children(generateResourceNames(context, config.getResourceNameConfigs()));
    return builder.build();
  }

  private ResourceNameFixedView generateResourceNameFixed(
      InterfaceContext context, int index, FixedResourceNameConfig config) {
    SurfaceNamer namer = context.getNamer();
    ResourceNameFixedView.Builder builder =
        ResourceNameFixedView.newBuilder()
            .typeName(namer.getResourceTypeName(config))
            .paramName(namer.getResourceParameterName(config))
            .propertyName(namer.getResourcePropertyName(config))
            .enumName(namer.getResourceEnumName(config))
            .docName(config.getEntityName().toLowerUnderscore())
            .index(index)
            .value(config.getFixedValue());
    return builder.build();
  }

  public List<FormatResourceFunctionView> generateFormatResourceFunctions(
      InterfaceContext context) {
    List<FormatResourceFunctionView> functions = new ArrayList<>();
    if (!context.getFeatureConfig().enableStringFormatFunctions()) {
      return functions;
    }

    SurfaceNamer namer = context.getNamer();
    InterfaceConfig interfaceConfig = context.getInterfaceConfig();
    for (SingleResourceNameConfig resourceNameConfig :
        getSingleResourceNameConfigsUsedByInterface(context)) {
      FormatResourceFunctionView.Builder function =
          FormatResourceFunctionView.newBuilder()
              .resourceName(namer.getResourceTypeName(resourceNameConfig))
              .entityName(resourceNameConfig.getEntityName().toLowerUnderscore())
              .name(namer.getFormatFunctionName(interfaceConfig, resourceNameConfig))
              .pathTemplateName(namer.getPathTemplateName(interfaceConfig, resourceNameConfig))
              .pathTemplateGetterName(
                  namer.getPathTemplateNameGetter(interfaceConfig, resourceNameConfig))
              .pattern(resourceNameConfig.getNamePattern())
              .isResourceNameDeprecated(resourceNameConfig.getDeprecated());
      List<ResourceIdParamView> resourceIdParams = new ArrayList<>();
      for (String variable : resourceNameConfig.getNameTemplate().vars()) {
        ResourceIdParamView param =
            ResourceIdParamView.newBuilder()
                .name(namer.getParamName(variable))
                .docName(namer.getParamDocName(variable))
                .templateKey(variable)
                .build();
        resourceIdParams.add(param);
      }
      function.resourceIdParams(resourceIdParams);

      functions.add(function.build());
    }

    return functions;
  }

  public List<ParseResourceFunctionView> generateParseResourceFunctions(InterfaceContext context) {
    List<ParseResourceFunctionView> functions = new ArrayList<>();
    if (!context.getFeatureConfig().enableStringFormatFunctions()) {
      return functions;
    }

    SurfaceNamer namer = context.getNamer();
    InterfaceConfig interfaceConfig = context.getInterfaceConfig();
    for (SingleResourceNameConfig resourceNameConfig :
        getSingleResourceNameConfigsUsedByInterface(context)) {
      for (String var : resourceNameConfig.getNameTemplate().vars()) {
        ParseResourceFunctionView.Builder function =
            ParseResourceFunctionView.newBuilder()
                .entityName(resourceNameConfig.getEntityName().toLowerUnderscore())
                .name(namer.getParseFunctionName(var, resourceNameConfig))
                .pathTemplateName(namer.getPathTemplateName(interfaceConfig, resourceNameConfig))
                .pathTemplateGetterName(
                    namer.getPathTemplateNameGetter(interfaceConfig, resourceNameConfig))
                .entityNameTypeName(namer.getResourceTypeName(resourceNameConfig))
                .entityNameParamName(namer.getEntityNameParamName(resourceNameConfig))
                .isResourceNameDeprecated(resourceNameConfig.getDeprecated())
                .outputResourceId(var);
        functions.add(function.build());
      }
    }

    return functions;
  }

  public List<PathTemplateGetterFunctionView> generatePathTemplateGetterFunctions(
      GapicInterfaceContext context) {
    List<PathTemplateGetterFunctionView> functions = new ArrayList<>();

    SurfaceNamer namer = context.getNamer();
    InterfaceConfig interfaceConfig = context.getInterfaceConfig();
    for (SingleResourceNameConfig resourceNameConfig :
        getSingleResourceNameConfigsUsedByInterface(context)) {
      PathTemplateGetterFunctionView.Builder function =
          PathTemplateGetterFunctionView.newBuilder()
              .name(namer.getPathTemplateNameGetter(interfaceConfig, resourceNameConfig))
              .resourceName(namer.getPathTemplateResourcePhraseName(resourceNameConfig))
              .entityName(namer.getEntityName(resourceNameConfig))
              .pathTemplateName(namer.getPathTemplateName(interfaceConfig, resourceNameConfig))
              .pattern(resourceNameConfig.getNamePattern());
      functions.add(function.build());
    }
    return functions;
  }
}

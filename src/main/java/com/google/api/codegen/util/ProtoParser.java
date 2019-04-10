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
package com.google.api.codegen.util;

import static com.google.api.FieldBehavior.REQUIRED;

import com.google.api.AnnotationsProto;
import com.google.api.ClientProto;
import com.google.api.FieldBehaviorProto;
import com.google.api.HttpRule;
import com.google.api.Resource;
import com.google.api.ResourceProto;
import com.google.api.ResourceSet;
import com.google.api.pathtemplate.PathTemplate;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.longrunning.OperationInfo;
import com.google.longrunning.OperationsProto;
import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.DescriptorProtos.FileOptions;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolMessageEnum;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

// Utils for parsing possibly-annotated protobuf API IDL.
public class ProtoParser {
  private final boolean enableProtoAnnotations;

  public boolean isProtoAnnotationsEnabled() {
    return enableProtoAnnotations;
  }

  public ProtoParser(boolean enableProtoAnnotations) {
    this.enableProtoAnnotations = enableProtoAnnotations;
  }

  @SuppressWarnings("unchecked")
  @Nullable
  private <T, O extends Message, E extends ProtoElement> T getProtoExtension(
      E element, GeneratedExtension<O, T> extension) {
    // Use this method as the chokepoint for all annotations processing, so we can toggle on/off
    // annotations processing in one place.
    if (enableProtoAnnotations) {
      return (T) element.getOptionFields().get(extension.getDescriptor());
    } else {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  @Nullable
  private <T extends ProtocolMessageEnum, O extends Message, E extends ProtoElement>
      List<EnumValueDescriptor> getProtoExtensionForEnumValue(
          E element, GeneratedExtension<O, List<T>> extension) {
    // Use this method as the chokepoint for all annotations processing for enum values
    // so we can toggle on/off annotations processing in one place.
    if (enableProtoAnnotations) {
      return (List<EnumValueDescriptor>) element.getOptionFields().get(extension.getDescriptor());
    } else {
      return null;
    }
  }

  /* Return the name of the field representing the header parameter. */
  public ImmutableSet<String> getHeaderParams(Method method) {
    ImmutableSet.Builder<String> allParams = ImmutableSet.builder();

    if (!enableProtoAnnotations) {
      return allParams.build();
    }

    HttpRule topRule = method.getDescriptor().getMethodAnnotation(AnnotationsProto.http);
    if (topRule == null) {
      return allParams.build();
    }
    String firstParam = getHeaderParam(topRule);
    if (firstParam != null) allParams.add(firstParam);

    // Additional bindings should only be one-deep, according to the API client config spec.
    // No need to recurse on additional bindings' additional bindings.
    for (HttpRule rule : topRule.getAdditionalBindingsList()) {
      String headerParam = getHeaderParam(rule);
      if (headerParam != null) allParams.add(headerParam);
    }

    return allParams.build();
  }

  // Finds the header param from a HttpRule and add the non-null value to the running set.
  @Nullable
  private String getHeaderParam(HttpRule httpRule) {

    String urlVar;
    if (!Strings.isNullOrEmpty(httpRule.getPost())) {
      urlVar = httpRule.getPost();
    } else if (!Strings.isNullOrEmpty(httpRule.getDelete())) {
      urlVar = httpRule.getDelete();
    } else if (!Strings.isNullOrEmpty(httpRule.getGet())) {
      urlVar = httpRule.getGet();
    } else if (!Strings.isNullOrEmpty(httpRule.getPatch())) {
      urlVar = httpRule.getPatch();
    } else if (!Strings.isNullOrEmpty(httpRule.getPut())) {
      urlVar = httpRule.getPut();
    } else {
      return null;
    }

    PathTemplate pathTemplate = PathTemplate.create(urlVar);
    return pathTemplate.singleVar();
  }

  @Nullable
  public Resource getResource(Field element) {
    return getProtoExtension(element, ResourceProto.resource);
  }

  /** Return the ResourceSet a resource field. Return null if none found. */
  @Nullable
  public ResourceSet getResourceSet(Field element) {
    return getProtoExtension(element, ResourceProto.resourceSet);
  }

  /**
   * Return the name of the referent Resource or ResourceSet, e.g. "Shelf", for a field with
   * resource_reference.
   */
  public String getResourceReferenceName(
      Field field,
      Map<Resource, ProtoFile> allResources,
      Map<ResourceSet, ProtoFile> allResourceSets) {
    String resourceName = getResourceReference(field);
    if (!Strings.isNullOrEmpty(resourceName)) {
      String fullyQualifiedResourceName = resourceName;
      if (!resourceName.contains(".")) {
        fullyQualifiedResourceName =
            String.format("%s.%s", getProtoPackage(field.getFile()), resourceName);
      }

      // Look in the given Resource and ResourceSet collections.
      for (Resource resource : allResources.keySet()) {
        ProtoFile protoFile = allResources.get(resource);
        if (getResourceFullName(resource, protoFile).equals(fullyQualifiedResourceName)
            || field.getFile().equals(protoFile)
                && resource.getSymbol().equals(fullyQualifiedResourceName)) {
          return resource.getSymbol();
        }
      }
      for (ResourceSet resourceSet : allResourceSets.keySet()) {
        ProtoFile protoFile = allResourceSets.get(resourceSet);
        if (getResourceSetFullName(resourceSet, protoFile).equals(fullyQualifiedResourceName)
            || field.getFile().equals(protoFile)
                && resourceSet.getSymbol().equals(fullyQualifiedResourceName)) {
          return resourceSet.getSymbol();
        }
      }
    }

    return null;
  }

  public String getResourceOrSetEntityName(Field field) {
    if (getResource(field) != null) {
      return getResourceEntityName(field);
    }
    if (getResourceSet(field) != null) {
      return getResourceSetEntityName(field);
    }
    return null;
  }

  /** Return the entity name, e.g. "shelf" for a resource field. */
  String getResourceEntityName(Field field) {
    Resource resource = getResource(field);
    if (resource != null && !Strings.isNullOrEmpty(resource.getSymbol())) {
      return resource.getSymbol();
    }
    return field.getParent().getSimpleName();
  }

  /** Return the entity name, e.g. "shelf" for a resource set field. */
  private String getResourceSetEntityName(Field field) {
    ResourceSet resourceSet = getResourceSet(field);
    if (resourceSet != null && !Strings.isNullOrEmpty(resourceSet.getSymbol())) {
      return resourceSet.getSymbol();
    }
    return field.getParent().getSimpleName();
  }

  /** Get long running settings. */
  public OperationInfo getLongRunningOperation(Method method) {
    return method.getDescriptor().getMethodAnnotation(OperationsProto.operationInfo);
  }

  /* Return a Map of Resources to their containing Protofile. Includes Resources
   * defined inside MessageTypes.
   * The name map keys are package-qualified names of Resources. */
  public Map<Resource, ProtoFile> getResourceDefs(
      List<ProtoFile> protoFile, DiagCollector diagCollector) {
    return getResourceOrSetDefs(
        protoFile,
        diagCollector,
        ResourceProto.resourceDefinition,
        ResourceProto.resource,
        Resource::getSymbol,
        (resource, baseNameToSet) -> resource.toBuilder().setSymbol(baseNameToSet).build());
  }

  /* Return a Map of ResourceSets to their containing Protofile. Includes ResourceSets
   * defined inside MessageTypes.
   * The name map keys are package-qualified names of ResourceSets. */
  public Map<ResourceSet, ProtoFile> getResourceSetDefs(
      List<ProtoFile> protoFile, DiagCollector diagCollector) {
    return getResourceOrSetDefs(
        protoFile,
        diagCollector,
        null,
        ResourceProto.resourceSet,
        ResourceSet::getSymbol,
        (resourceSet, baseNameToSet) -> resourceSet.toBuilder().setSymbol(baseNameToSet).build());
  }

  /* Return a Map of Resource or ResourceSet elements to their containing ProtoFile.
   * Includes Resource[Sets] defined inside MessageTypes. */
  private <T> Map<T, ProtoFile> getResourceOrSetDefs(
      List<ProtoFile> protoFiles,
      DiagCollector diagCollector,
      @Nullable GeneratedExtension<FileOptions, List<T>> fileExtension,
      GeneratedExtension<FieldOptions, T> fieldExtension,
      Function<T, String> getNameFunc,
      BiFunction<T, String, T> setNameFunc) {
    ImmutableMap.Builder<T, ProtoFile> definitions = ImmutableMap.builder();

    for (ProtoFile protoFile : protoFiles) {

      // Maps base names to Resource[Sets].
      Map<String, T> localDefs = new LinkedHashMap<>();

      // Get Resource definitions from protofile options.
      if (fileExtension != null) {
        List<T> resourcesAtFileLevel = getProtoExtension(protoFile, fileExtension);
        if (resourcesAtFileLevel != null) {

          for (T definition : resourcesAtFileLevel) {
            String baseName = getNameFunc.apply(definition);
            if (Strings.isNullOrEmpty(baseName)) {
              diagCollector.addDiag(
                  Diag.error(
                      SimpleLocation.TOPLEVEL,
                      "There is a %s option with"
                          + " no name defined in proto file %s. %s.name is required.",
                      fileExtension.getDescriptor().getFullName(),
                      protoFile.getFullName(),
                      fileExtension.getDescriptor().getFullName()));
            }
            if (localDefs.put(baseName, definition) != null) {
              diagCollector.addDiag(
                  Diag.error(
                      SimpleLocation.TOPLEVEL,
                      "Multiple %s defintions with the name"
                          + " %s are defined in proto file %s. Values for %s.name must be unique.",
                      fieldExtension.getDescriptor().getFullName(),
                      baseName,
                      protoFile.getFullName(),
                      fieldExtension.getDescriptor().getFullName()));
            }
          }
        }
      }

      // Get Resource[Set] definitions from fields in message types.
      for (MessageType message : protoFile.getMessages()) {
        for (Field field : message.getFields()) {
          T definition = getProtoExtension(field, fieldExtension);
          if (definition != null) {
            if (Strings.isNullOrEmpty(getNameFunc.apply(definition))) {
              String baseName = getResourceEntityName(field);
              definition = setNameFunc.apply(definition, baseName);
            }
            String baseName = getNameFunc.apply(definition);
            if (localDefs.put(baseName, definition) != null) {
              diagCollector.addDiag(
                  Diag.error(
                      SimpleLocation.TOPLEVEL,
                      "Multiple %s defintions with the name"
                          + " %s are defined in proto file %s. Values for %s.name must be unique.",
                      fieldExtension.getDescriptor().getFullName(),
                      baseName,
                      protoFile.getFullName(),
                      fieldExtension.getDescriptor().getFullName()));
            }
          }
        }
      }

      // Fully qualify the names of the Resource[Set]s.
      for (String baseName : localDefs.keySet()) {
        T def = localDefs.get(baseName);
        definitions.put(def, protoFile);
      }
    }
    return definitions.build();
  }

  /* Return a list of method signatures, aka flattenings, specified on a given method.
   * This flattens the repeated additionalSignatures into the returned list of MethodSignatures. */
  public List<List<String>> getMethodSignatures(Method method) {
    List<String> commaDelimSignatures = getProtoExtension(method, ClientProto.methodSignature);
    if (commaDelimSignatures == null) {
      return ImmutableList.of();
    }
    return ImmutableList.copyOf(
        commaDelimSignatures
            .stream()
            .map(
                s ->
                    (Arrays.stream(s.split(","))
                        .map(String::trim)
                        .filter(param -> !Strings.isNullOrEmpty(param))
                        .collect(Collectors.toList())))
            .collect(Collectors.toList()));
  }

  /** Return the names of required parameters of a method. */
  public List<String> getRequiredFields(Method method) {
    MessageType inputMessage = method.getInputMessage();
    return inputMessage
        .getFields()
        .stream()
        .filter(this::isFieldRequired)
        .map(Field::getSimpleName)
        .collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  /* Returns if a field is required, according to the proto annotations. */
  private boolean isFieldRequired(Field field) {
    List<EnumValueDescriptor> fieldBehaviors =
        getProtoExtensionForEnumValue(field, FieldBehaviorProto.fieldBehavior);
    return fieldBehaviors != null && fieldBehaviors.contains(REQUIRED.getValueDescriptor());
  }

  /** Return the resource reference for the given field, according to the proto annotations. */
  public String getResourceReference(Field field) {
    return getProtoExtension(field, ResourceProto.resourceReference);
  }

  /** Return whether the method has the HttpRule for GET. */
  public boolean isHttpGetMethod(Method method) {
    return !Strings.isNullOrEmpty(
        method.getDescriptor().getMethodAnnotation(AnnotationsProto.http).getGet());
  }

  /** The hostname for this service (e.g. "foo.googleapis.com"). */
  public String getServiceAddress(Interface service) {
    return getProtoExtension(service, ClientProto.defaultHost);
  }

  /** The OAuth scopes for this service (e.g. "https://cloud.google.com/auth/cloud-platform"). */
  public List<String> getAuthScopes(Interface service) {
    String oAuth = getProtoExtension(service, ClientProto.oauthScopes);
    if (!Strings.isNullOrEmpty(oAuth)) {
      return ImmutableList.copyOf(
          Arrays.stream(oAuth.split(",")).map(String::trim).collect(Collectors.toList()));
    }
    return ImmutableList.of();
  }

  public String getProtoPackage(ProtoFile file) {
    return file.getProto().getPackage();
  }

  @VisibleForTesting
  // Exposed for test mocking.
  public String getProtoPackage(Method method) {
    return getProtoPackage(method.getFile());
  }

  private String getResourceFullName(Resource resource, ProtoFile file) {
    return String.format("%s.%s", getProtoPackage(file), resource.getSymbol());
  }

  private String getResourceSetFullName(ResourceSet resource, ProtoFile file) {
    return String.format("%s.%s", getProtoPackage(file), resource.getSymbol());
  }

  public ImmutableMap<String, String> getFieldNamePatterns(Method method) {
    ImmutableMap.Builder<String, String> resultCollector = ImmutableMap.builder();
    // Only look two levels deep in the request object, so fields of fields of the request object.
    getFieldNamePatterns(method.getInputMessage(), resultCollector, "", 2);
    return resultCollector.build();
  }

  /**
   * Recursively populates the given map builder with field name patterns, up to a given depth.
   *
   * <p>A field name pattern entry maps a field name String, which can be a dot-separated nested
   * field such as "shelf.name", to the String name of the resource/resourceSet/resource-reference
   * entity that is represented by that field.
   *
   * <p>Note: this method does not check for circular references.
   *
   * @param messageType the starting messageType from which to parse fields for resource names
   * @param resultCollector collects the resulting field name patterns
   * @param fieldNamePrefix a nested field is prefixed by the parents' names, dot-separated
   * @param depth number of levels deep in which to parse the messageType; must be positive int
   */
  private void getFieldNamePatterns(
      MessageType messageType,
      ImmutableMap.Builder<String, String> resultCollector,
      String fieldNamePrefix,
      int depth) {
    if (depth < 1) throw new IllegalStateException("depth must be positive");
    for (Field field : messageType.getFields()) {
      String fieldNameKey = fieldNamePrefix + field.getSimpleName();

      if (field.getType().isMessage() && depth > 1) {
        getFieldNamePatterns(
            field.getType().getMessageType(), resultCollector, fieldNameKey + ".", depth - 1);
      }

      String reference = getResourceReference(field);
      if (!Strings.isNullOrEmpty(reference)) {
        resultCollector.put(fieldNameKey, getSimpleName(reference));
        continue;
      }

      Resource resource = getResource(field);
      if (resource != null) {
        String resourceName = resource.getSymbol();
        if (Strings.isNullOrEmpty(resourceName)) {
          resourceName = field.getParent().getSimpleName();
        }
        resultCollector.put(fieldNameKey, getSimpleName(resourceName));
        continue;
      }

      ResourceSet resourceSet = getResourceSet(field);
      if (resourceSet != null) {
        resultCollector.put(fieldNameKey, getSimpleName(resourceSet.getSymbol()));
      }
    }
  }

  private static String getSimpleName(String resourceFullName) {
    if (resourceFullName.contains(".")) {
      return resourceFullName.substring(resourceFullName.lastIndexOf(".") + 1);
    }
    return resourceFullName;
  }

  /** Register all extensions needed to process API protofiles. */
  public static void registerAllExtensions(ExtensionRegistry extensionRegistry) {
    OperationsProto.registerAllExtensions(extensionRegistry);
    AnnotationsProto.registerAllExtensions(extensionRegistry);
    ClientProto.registerAllExtensions(extensionRegistry);
    ResourceProto.registerAllExtensions(extensionRegistry);
    FieldBehaviorProto.registerAllExtensions(extensionRegistry);
  }
}

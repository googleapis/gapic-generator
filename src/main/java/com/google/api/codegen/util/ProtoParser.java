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
import com.google.api.HttpRule;
import com.google.api.MethodSignature;
import com.google.api.OAuth;
import com.google.api.OperationData;
import com.google.api.Resource;
import com.google.api.ResourceSet;
import com.google.api.codegen.transformer.FeatureConfig;
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
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.DescriptorProtos.FileOptions;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolMessageEnum;
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

  public ProtoParser(boolean enableProtoAnnotations) {
    this.enableProtoAnnotations = enableProtoAnnotations;
  }

  public ProtoParser(FeatureConfig featureConfig) {
    this.enableProtoAnnotations = featureConfig.enableProtoAnnotations();
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
  // GeneratedMessage.GeneratedExtension<DescriptorProtos.FieldOptions, List<FieldBehavior>>
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

  @Nullable
  public String getHeaderParam(Method method) {
    if (!enableProtoAnnotations) {
      return null;
    }

    HttpRule httpRule = method.getDescriptor().getMethodAnnotation(AnnotationsProto.http);
    if (httpRule == null) {
      return null;
    }

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
    return getProtoExtension(element, AnnotationsProto.resource);
  }

  /** Return the ResourceSet a resource field. Return null if none found. */
  @Nullable
  public ResourceSet getResourceSet(Field element) {
    return getProtoExtension(element, AnnotationsProto.resourceSet);
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

      TypeRef resourceType = field.getModel().getSymbolTable().lookupType(resourceName);
      if (resourceType != null) {
        // Look for the Resource or ResourceSet field in the target message.
        MessageType messageType = resourceType.getMessageType();
        for (Field resourceField : messageType.getFields()) {
          String entityName = getResourceOrSetEntityName(resourceField);
          if (!Strings.isNullOrEmpty(entityName)) {
            return entityName;
          }
        }
      }

      // Look in the given Resource and ResourceSet collections.
      for (Resource resource : allResources.keySet()) {
        ProtoFile protoFile = allResources.get(resource);
        if (getResourceFullName(resource, protoFile).equals(resourceName)
            || field.getFile().equals(protoFile) && resource.getName().equals(resourceName)) {
          return resource.getName();
        }
      }
      for (ResourceSet resourceSet : allResourceSets.keySet()) {
        ProtoFile protoFile = allResourceSets.get(resourceSet);
        if (getResourceSetFullName(resourceSet, protoFile).equals(resourceName)
            || field.getFile().equals(protoFile) && resourceSet.getName().equals(resourceName)) {
          return resourceSet.getName();
        }
      }

      return resourceType.getMessageType().getSimpleName();
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
    if (resource != null && !Strings.isNullOrEmpty(resource.getName())) {
      return resource.getName();
    }
    return field.getParent().getSimpleName();
  }

  /** Return the entity name, e.g. "shelf" for a resource set field. */
  private String getResourceSetEntityName(Field field) {
    ResourceSet resourceSet = getResourceSet(field);
    if (resourceSet != null && !Strings.isNullOrEmpty(resourceSet.getName())) {
      return resourceSet.getName();
    }
    return field.getParent().getSimpleName();
  }

  /** Get long running settings. */
  public OperationData getLongRunningOperation(Method method) {
    return method.getDescriptor().getMethodAnnotation(AnnotationsProto.operation);
  }

  /* Return a Map of Resources to their containing Protofile.
   * The name map keys are package-qualified names of Resources. */
  public Map<Resource, ProtoFile> getResourceDefs(
      List<ProtoFile> protoFile, DiagCollector diagCollector) {
    return getResourceOrSetDefs(
        protoFile,
        diagCollector,
        AnnotationsProto.resourceDefinition,
        AnnotationsProto.resource,
        Resource::getName,
        (resource, baseNameToSet) -> resource.toBuilder().setName(baseNameToSet).build());
  }

  /* Return a Map of ResourceSets to their containing Protofile.
   * The name map keys are package-qualified names of ResourceSets. */
  public Map<ResourceSet, ProtoFile> getResourceSetDefs(
      List<ProtoFile> protoFile, DiagCollector diagCollector) {
    return getResourceOrSetDefs(
        protoFile,
        diagCollector,
        AnnotationsProto.resourceSetDefinition,
        AnnotationsProto.resourceSet,
        ResourceSet::getName,
        (resourceSet, baseNameToSet) -> resourceSet.toBuilder().setName(baseNameToSet).build());
  }

  /* Return a Map of Resource or ResourceSet elements to their containing ProtoFile. */
  private <T> Map<T, ProtoFile> getResourceOrSetDefs(
      List<ProtoFile> protoFiles,
      DiagCollector diagCollector,
      GeneratedExtension<FileOptions, List<T>> fileExtension,
      GeneratedExtension<FieldOptions, T> fieldExtension,
      Function<T, String> getNameFunc,
      BiFunction<T, String, T> setNameFunc) {
    ImmutableMap.Builder<T, ProtoFile> definitions = ImmutableMap.builder();

    for (ProtoFile protoFile : protoFiles) {

      // Maps base names to Resource[Sets].
      Map<String, T> localDefs = new LinkedHashMap<>();

      // Get Resource definitions from protofile options.
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

  @SuppressWarnings("unchecked")
  /* Return a list of method signatures, aka flattenings, specified on a given method.
   * This flattens the repeated additionalSignatures into the returned list of MethodSignatures. */
  public List<MethodSignature> getMethodSignatures(Method method) {
    List<MethodSignature> methodSignatures =
        getProtoExtension(method, AnnotationsProto.methodSignature);
    if (methodSignatures == null) {
      return ImmutableList.of();
    }
    return ImmutableList.copyOf(methodSignatures);
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
        getProtoExtensionForEnumValue(field, AnnotationsProto.fieldBehavior);
    return fieldBehaviors != null && fieldBehaviors.contains(REQUIRED.getValueDescriptor());
  }

  /** Return the resource reference for the given field, according to the proto annotations. */
  public String getResourceReference(Field field) {
    return getProtoExtension(field, AnnotationsProto.resourceReference);
  }

  /** Return whether the method has the HttpRule for GET. */
  public boolean isHttpGetMethod(Method method) {
    return !Strings.isNullOrEmpty(
        method.getDescriptor().getMethodAnnotation(AnnotationsProto.http).getGet());
  }

  /** The hostname for this service (e.g. "foo.googleapis.com"). */
  public String getServiceAddress(Interface service) {
    return getProtoExtension(service, AnnotationsProto.defaultHost);
  }

  /** The OAuth scopes for this service (e.g. "https://cloud.google.com/auth/cloud-platform"). */
  public List<String> getAuthScopes(Interface service) {
    OAuth oAuth = getProtoExtension(service, AnnotationsProto.oauth);
    if (oAuth != null) {
      return ImmutableList.copyOf(oAuth.getScopesList());
    }
    return ImmutableList.of();
  }

  public String getProtoPackage(ProtoFile file) {
    return file.getProto().getPackage();
  }

  private String getResourceFullName(Resource resource, ProtoFile file) {
    return String.format("%s.%s", resource.getName(), getProtoPackage(file));
  }

  private String getResourceSetFullName(ResourceSet resource, ProtoFile file) {
    return String.format("%s.%s", resource.getName(), getProtoPackage(file));
  }
}

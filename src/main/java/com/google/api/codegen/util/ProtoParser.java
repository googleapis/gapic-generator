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

import com.google.api.AnnotationsProto;
import com.google.api.MethodSignature;
import com.google.api.Resource;
import com.google.api.ResourceSet;
import com.google.api.Retry;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.longrunning.OperationTypes;
import com.google.longrunning.OperationsProto;
import com.google.protobuf.Api;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

// Utils for parsing possibly-annotated protobuf API IDL.
public class ProtoParser {

  @Nullable
  public Resource getResource(Field element) {
    return (Resource) element.getOptionFields().get(AnnotationsProto.resource.getDescriptor());
  }

  /** Return the ResourceSet a resource field. Return null if none found. */
  @Nullable
  public ResourceSet getResourceSet(Field element) {
    return (ResourceSet)
        element.getOptionFields().get(AnnotationsProto.resourceSet.getDescriptor());
  }

  /** Returns a base package name for an API's client. */
  @Nullable
  public String getPackageName(Model model) {
    if (model.getServiceConfig().getApisCount() > 0) {
      Api api = model.getServiceConfig().getApis(0);
      Interface apiInterface = model.getSymbolTable().lookupInterface(api.getName());
      if (apiInterface != null) {
        return apiInterface.getFile().getFullName();
      }
    }
    return null;
  }

  /** Return the entity name, e.g. "shelf", for a field with resource_type. */
  public String getResourceTypeEntityName(Field field) {
    String resourceName = getResourceType(field);
    if (!Strings.isNullOrEmpty(resourceName)) {
      if (field.getType().isMessage()
          && resourceName.equals(field.getType().getMessageType().getFullName())) {
        // We don't care if the resource type of the field is the field itself.
        return null;
      }
      // Parse out Resources from ResourceSets.
      String[] resourceParts = resourceName.split(":");
      if (resourceParts.length > 2) {
        throw new IllegalArgumentException(
            String.format(
                "Field %s has resource_type '%s', which has too many ':' characters.",
                field.getSimpleName(), resourceName));
      }
      if (resourceParts.length == 2) {
        // Just use the Resource base_name from one of the Resources within a ResourceSet.
        return resourceParts[1];
      }

      TypeRef resourceType = field.getModel().getSymbolTable().lookupType(resourceName);
      if (resourceType == null) {
        return resourceName;
      }

      // Look for the Resource or ResourceSet field in the target message.
      MessageType messageType = resourceType.getMessageType();
      for (Field resourceField : messageType.getFields()) {
        String entityName = getResourceOrSetEntityName(resourceField);
        if (!Strings.isNullOrEmpty(entityName)) {
          return entityName;
        }
      }

      return resourceType.getMessageType().getSimpleName().toLowerCase();
    }
    // return field.getParent().getFullName();
    return null;
  }

  /** Return the entity name, e.g. "shelf" for a resource or resource set field. */
  public String getDefaultResourceEntityName(Field field) {
    return field.getParent().getSimpleName().toLowerCase();
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

  /**
   * Return the entity name, e.g. "shelf" for a resource field, or use the given default name if no
   * explicit name override was found.
   *
   * @param field The field to f
   * @param defaultEntityName The value to return if no custom name was found.
   */
  // TODO(andrealin): Remove this method.
  public String getResourceEntityName(Field field, String defaultEntityName) {
    Resource resource = getResource(field);
    if (resource != null && !Strings.isNullOrEmpty(resource.getBaseName())) {
      return resource.getBaseName();
    }
    return defaultEntityName;
  }

  /** Return the entity name, e.g. "shelf" for a resource field. */
  public String getResourceEntityName(Field field) {
    return getResourceEntityName(field, getDefaultResourceEntityName(field));
  }

  /** Return the entity name, e.g. "shelf" for a resource set field. */
  public String getResourceSetEntityName(Field field) {
    ResourceSet resourceSet = getResourceSet(field);
    if (resourceSet != null && !Strings.isNullOrEmpty(resourceSet.getBaseName())) {
      return resourceSet.getBaseName();
    }
    return getDefaultResourceEntityName(field);
  }

  /** Get long running settings. */
  public OperationTypes getLongRunningOperation(Method method) {
    return method.getDescriptor().getMethodAnnotation(OperationsProto.operationTypes);
  }

  /* Return a list of method signatures, aka flattenings, specified on a given method.
   * This flattens the repeated additionalSignatures into the returned list of MethodSignatures. */
  public List<MethodSignature> getMethodSignatures(Method method) {
    if (!method.getOptionFields().containsKey(AnnotationsProto.methodSignature.getDescriptor())) {
      return ImmutableList.of();
    }
    // Variable methodSignature will always be nonnull, so we had to check for presence above.
    MethodSignature methodSignature =
        method.getDescriptor().getMethodAnnotation(AnnotationsProto.methodSignature);
    // Let's only recurse once when we look for additional MethodSignatures.
    List<MethodSignature> additionalSignatures = methodSignature.getAdditionalSignaturesList();
    return ImmutableList.<MethodSignature>builder()
        .add(methodSignature)
        .addAll(additionalSignatures)
        .build();
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

  /** Returns if a field is required, according to the proto annotations. */
  private boolean isFieldRequired(Field field) {
    return Optional.ofNullable(
            (Boolean) field.getOptionFields().get(AnnotationsProto.required.getDescriptor()))
        .orElse(false);
  }

  /** Return the extra retry codes for the given method. */
  public Retry getRetry(Method method) {
    return method.getDescriptor().getMethodAnnotation(AnnotationsProto.retry);
  }

  /** Return the resource type for the given field, according to the proto annotations. */
  public String getResourceType(Field field) {
    return (String) field.getOptionFields().get(AnnotationsProto.resourceType.getDescriptor());
  }

  /** Return whether the method has the HttpRule for GET. */
  public boolean isHttpGetMethod(Method method) {
    return !Strings.isNullOrEmpty(
        method.getDescriptor().getMethodAnnotation(AnnotationsProto.http).getGet());
  }

  /** The hostname for this service (e.g. "foo.googleapis.com"). */
  public String getServiceAddress(Interface service) {
    return service.getProto().getOptions().getExtension(AnnotationsProto.defaultHost);
  }

  /** The OAuth scopes for this service (e.g. "https://cloud.google.com/auth/cloud-platform"). */
  public List<String> getAuthScopes(Interface service) {
    return service.getProto().getOptions().getExtension(AnnotationsProto.oauth).getScopesList();
  }
}

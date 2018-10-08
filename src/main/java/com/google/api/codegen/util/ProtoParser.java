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
import com.google.api.codegen.config.ProtoMethodModel;
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
  //
  // /** Return the path, e.g. "shelves/*" for a resource field. Return null if no path found. */
  // public String getResourcePath(Field element) {
  //   Resource resource =
  //       (Resource) element.getOptionFields().get(AnnotationsProto.resource.getDescriptor());
  //   if (resource != null) {
  //     return resource.getPath();
  //   }
  //   return null;
  // }

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

  /** Return the entity name, e.g. "shelf" for a resource field. */
  public String getResourceMessage(Field field) {
    String resourceName =
        (String) field.getOptionFields().get(AnnotationsProto.resourceType.getDescriptor());
    if (!Strings.isNullOrEmpty(resourceName)) {
      if (field.getType().isMessage()
          && resourceName.equals(field.getType().getMessageType().getFullName())) {
        // We don't care if the resource type of the field is the field itself.
        return null;
      }
      TypeRef resourceType = field.getModel().getSymbolTable().lookupType(resourceName);
      if (resourceType == null) {
        return resourceName;
      }
      return resourceType.getMessageType().getSimpleName().toLowerCase();
    }
    // return field.getParent().getFullName();
    return null;
  }

  /** Return the entity name, e.g. "shelf" for a resource field. */
  public String getDefaultResourceEntityName(Field field) {
    return field.getParent().getSimpleName().toLowerCase();
  }

  /** Return the entity name, e.g. "shelf" for a resource field. */
  public String getResourceEntityName(Field field, String defaultEntityName) {
    Resource resource =
        (Resource) field.getOptionFields().get(AnnotationsProto.resource.getDescriptor());
    if (resource != null && !Strings.isNullOrEmpty(resource.getBaseName())) {
      return resource.getBaseName();
    }
    return defaultEntityName;
  }

  /** Return the entity name, e.g. "shelf" for a resource set field. */
  public String getResourceSetEntityName(Field field, String defaultEntityName) {
    ResourceSet resourceSet =
        (ResourceSet) field.getOptionFields().get(AnnotationsProto.resourceSet.getDescriptor());
    if (resourceSet != null && !Strings.isNullOrEmpty(resourceSet.getBaseName())) {
      return resourceSet.getBaseName();
    }
    return defaultEntityName;
  }

  /** Get long running settings. */
  public OperationTypes getLongRunningOperation(Method method) {
    return method.getDescriptor().getMethodAnnotation(OperationsProto.operationTypes);
  }

  /* Return a list of method signatures, aka flattenings, specified on a given method.
   * This flattens the repeated additionalSignatures into the returned list of MethodSignatures. */
  public List<MethodSignature> getMethodSignatures(ProtoMethodModel method) {
    MethodSignature methodSignature =
        method
            .getProtoMethod()
            .getDescriptor()
            .getMethodAnnotation(AnnotationsProto.methodSignature);
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
  public boolean isFieldRequired(Field field) {
    return Optional.ofNullable(
            (Boolean) field.getOptionFields().get(AnnotationsProto.required.getDescriptor()))
        .orElse(false);
  }

  /** Return the extra retry codes for the given method. */
  public Retry getRetry(Method method) {
    return method.getDescriptor().getMethodAnnotation(AnnotationsProto.retry);
  }

  /** Return the extra retry codes for the given method. */
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

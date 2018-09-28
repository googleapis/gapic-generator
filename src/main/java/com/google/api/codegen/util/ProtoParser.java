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
import com.google.api.Resource;
import com.google.api.Retry;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Strings;
import com.google.longrunning.OperationTypes;
import com.google.longrunning.OperationsProto;
import com.google.protobuf.Api;
import javax.annotation.Nullable;

// Utils for parsing possibly-annotated protobuf API IDL.
public class ProtoParser {
  private static ProtoParser protoParser = new ProtoParser();

  public static ProtoParser getProtoParser() {
    return protoParser;
  }

  /** Return the path, e.g. "shelves/*" for a resource field. Return null if no path found. */
  public String getResourcePath(Field element) {
    Resource resource =
        (Resource) element.getOptionFields().get(AnnotationsProto.resource.getDescriptor());
    if (resource != null) {
      return resource.getPath();
    }
    return null;
  }

  /** Returns a base package name for an API's client. */
  @Nullable
  public static String getPackageName(Model model) {
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
  public static String getResourceMessage(Field field) {
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
  public static String getResourceEntityName(Field field) {
    Resource resource =
        (Resource) field.getOptionFields().get(AnnotationsProto.resource.getDescriptor());
    if (resource != null && !Strings.isNullOrEmpty(resource.getBaseName())) {
      return resource.getBaseName();
    }
    return field.getParent().getSimpleName().toLowerCase();
  }

  // TODO(andrealin): Remove this
  public static String getResourceType(Field field) {
    return (String) field.getOptionFields().get(AnnotationsProto.resourceType.getDescriptor());
  }

  /** Get long running settings. */
  public OperationTypes getLongRunningOperation(Method method) {
    return method.getDescriptor().getMethodAnnotation(OperationsProto.operationTypes);
  }

  /** Return the extra retry codes for the given method. */
  public Retry getRetry(Method method) {
    return method.getDescriptor().getMethodAnnotation(AnnotationsProto.retry);
  }

  /** Return whether the method has the HttpRule for GET. */
  public boolean isHttpGetMethod(Method method) {
    return !Strings.isNullOrEmpty(
        method.getDescriptor().getMethodAnnotation(AnnotationsProto.http).getGet());
  }
}

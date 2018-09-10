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
import com.google.api.codegen.config.ProtoMethodModel;
import com.google.api.codegen.configgen.transformer.LanguageTransformer;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
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

  /** Return the path, e.g. "shelves/*" for a resource field. Return null if no path found. */
  public static String getResourcePath(Field element) {
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
    } else {
      ProtoFile root = (ProtoFile) model.getRoots().iterator().next();
      if (root == null) {
        return null;
      }
      return root.getFullName();
    }
    return null;
  }

  /** Return the entity name, e.g. "shelf" for a resource field. */
  public static String getResourceEntityName(Field field) {
    return field.getParent().getSimpleName().toLowerCase();
  }

  /* Return a list of method signatures, aka flattenings, specified on a given method. */
  public static List<MethodSignature> getMethodSignatures(ProtoMethodModel method) {
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

  public static List<String> getRequiredFields(Method method) {
    MessageType inputMessage = method.getInputMessage();
    return inputMessage
        .getFields()
        .stream()
        .filter(ProtoParser::isFieldRequired)
        .map(Field::getSimpleName)
        .collect(Collectors.toList());
  }

  /** Returns if a field is required, according to the proto annotations. */
  public static boolean isFieldRequired(Field field) {
    return Optional.ofNullable(
            (Boolean) field.getOptionFields().get(AnnotationsProto.required.getDescriptor()))
        .orElse(false);
  }

  /** Get long running settings. * */
  public static OperationTypes getLongRunningOperation(Method method) {
    return method.getDescriptor().getMethodAnnotation(OperationsProto.operationTypes);
  }

  public static String listToString(List<String> list) {
    StringBuilder paramsAsString = new StringBuilder();
    list.forEach(p -> paramsAsString.append(p).append(", "));
    return paramsAsString.toString();
  }

  @Nullable
  public static String getFormattedPackageName(String language, String basePackageName) {
    LanguageTransformer.LanguageFormatter formatter =
        LanguageTransformer.LANGUAGE_FORMATTERS.get(language.toLowerCase());
    return formatter.getFormattedPackageName(basePackageName);
  }
}

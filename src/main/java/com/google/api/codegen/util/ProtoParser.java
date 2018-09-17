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
import com.google.api.Retry;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.configgen.transformer.LanguageTransformer;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.base.Strings;
import com.google.protobuf.Api;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

// Utils for parsing possibly-annotated protobuf API IDL.
public class ProtoParser {

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

  @Nullable
  public static String getFormattedPackageName(String language, String basePackageName) {
    LanguageTransformer.LanguageFormatter formatter =
        LanguageTransformer.LANGUAGE_FORMATTERS.get(language.toLowerCase());
    return formatter.getFormattedPackageName(basePackageName);
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

  /**
   * Return the mapping of each method in apiInterface to its counterpart in interfaceConfigProto.
   */
  public Map<Method, MethodConfigProto> getProtoMethodToConfigMap(
      Interface apiInterface, InterfaceConfigProto interfaceConfigProto) {
    Map<String, Method> methodNamestoProtoMethod =
        apiInterface.getMethods().stream().collect(Collectors.toMap(Method::getSimpleName, m -> m));
    Map<String, MethodConfigProto> methodNamestoMethodConfigProto =
        interfaceConfigProto
            .getMethodsList()
            .stream()
            .collect(Collectors.toMap(MethodConfigProto::getName, m -> m));

    return methodNamestoProtoMethod
        .entrySet()
        .stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getValue, entry -> methodNamestoMethodConfigProto.get(entry.getKey())));
  }
}

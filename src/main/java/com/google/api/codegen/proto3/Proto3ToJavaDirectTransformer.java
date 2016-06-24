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
package com.google.api.codegen.proto3;

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.LanguageUtil;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.PageStreamingConfig;
import com.google.api.codegen.java.direct.JavaClass;
import com.google.api.codegen.java.direct.JavaField;
import com.google.api.codegen.java.direct.JavaQualifier;
import com.google.api.codegen.java.direct.JavaType;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Proto3ToJavaDirectTransformer {
  private Interface service;
  private ApiConfig apiConfig;
  private Proto3JavaTypeTable typeTable = new Proto3JavaTypeTable();

  public static List<JavaClass> defaultTransform(Interface service, ApiConfig apiConfig) {
    return new Proto3ToJavaDirectTransformer(service, apiConfig).transform();
  }

  public Proto3ToJavaDirectTransformer(Interface service, ApiConfig apiConfig) {
    this.service = service;
    this.apiConfig = apiConfig;
  }

  public List<JavaClass> transform() {
    JavaClass xapiClass = new JavaClass();

    xapiClass.packageName = apiConfig.getPackageName();
    xapiClass.name = getApiWrapperClassName();
    xapiClass.qualifiers = Sets.newHashSet(JavaQualifier.Public);

    xapiClass.implementsList = Arrays.asList(new JavaType("AutoCloseable"));

    xapiClass.classComponents = new ArrayList<>();
    xapiClass.classComponents.addAll(generateMembers());

    return Arrays.asList(xapiClass);
  }

  public String getApiWrapperClassName() {
    return service.getSimpleName() + "Api";
  }

  public String getSettingsClassName() {
    return service.getSimpleName() + "Settings";
  }

  public List<JavaField> generateMembers() {
    List<JavaField> members = new ArrayList<>();

    Set<JavaQualifier> privateAndFinal =
        Sets.newTreeSet(Arrays.asList(JavaQualifier.Private, JavaQualifier.Final));

    JavaField settingsField = new JavaField();
    settingsField.qualifiers = privateAndFinal;
    settingsField.type = new JavaType(getSettingsClassName());
    settingsField.name = "settings";
    members.add(settingsField);

    JavaField channelField = new JavaField();
    channelField.qualifiers = privateAndFinal;
    channelField.type = new JavaType("ManagedChannel");
    channelField.name = "channel";
    members.add(channelField);

    JavaField executorField = new JavaField();
    executorField.qualifiers = privateAndFinal;
    executorField.type = new JavaType("ScheduledExecutorService");
    executorField.name = "executor";
    members.add(executorField);

    JavaField closeablesField = new JavaField();
    closeablesField.qualifiers = privateAndFinal;
    closeablesField.type = new JavaType("List");
    closeablesField.type.parameterizedTypes = Arrays.asList(new JavaType("AutoCloseable"));
    closeablesField.name = "closeables";
    closeablesField.initialization = "new ArrayList<>()";
    members.add(closeablesField);

    members.addAll(generateApiCallables());

    return members;
  }

  public List<JavaField> generateApiCallables() {
    List<JavaField> callableMembers = new ArrayList<>();

    for (Method method : service.getMethods()) {
      MethodConfig methodConfig = apiConfig.getInterfaceConfig(service).getMethodConfig(method);
      callableMembers.addAll(generateApiCallables(method, methodConfig));
    }

    return callableMembers;
  }

  public List<JavaField> generateApiCallables(Method method, MethodConfig methodConfig) {
    List<JavaField> callableFields = new ArrayList<>();

    JavaField callableField = new JavaField();
    callableField.qualifiers =
        Sets.newTreeSet(Arrays.asList(JavaQualifier.Private, JavaQualifier.Final));

    JavaType callableType = new JavaType("ApiCallable");
    String inTypeName = typeTable.importAndGetShortestName(method.getInputType());
    String outTypeName = typeTable.importAndGetShortestName(method.getOutputType());
    callableType.parameterizedTypes =
        Arrays.asList(new JavaType(inTypeName), new JavaType(outTypeName));
    callableField.type = callableType;

    String methodName = LanguageUtil.upperCamelToLowerCamel(method.getSimpleName());
    callableField.name = methodName + "Callable";

    callableFields.add(callableField);

    if (methodConfig.isPageStreaming()) {
      PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();

      JavaField pageStreamingCallableField = new JavaField();
      pageStreamingCallableField.qualifiers =
          Sets.newTreeSet(Arrays.asList(JavaQualifier.Private, JavaQualifier.Final));

      JavaType pageStreamingCallableType = new JavaType("ApiCallable");

      String pageAccessorTypeName =
          typeTable.importAndGetShortestName("com.google.api.gax.core.PageAccessor");
      String resourceTypeName =
          typeTable.importAndGetShortestNameForElementType(
              pageStreaming.getResourcesField().getType());
      JavaType pageAccessorType = new JavaType(pageAccessorTypeName);
      pageAccessorType.parameterizedTypes = Arrays.asList(new JavaType(resourceTypeName));

      pageStreamingCallableType.parameterizedTypes =
          Arrays.asList(new JavaType(inTypeName), pageAccessorType);

      pageStreamingCallableField.type = pageStreamingCallableType;

      pageStreamingCallableField.name = methodName + "PagedCallable";

      callableFields.add(pageStreamingCallableField);
    }

    return callableFields;
  }
}

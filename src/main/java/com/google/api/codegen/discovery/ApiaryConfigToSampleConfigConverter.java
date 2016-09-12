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
package com.google.api.codegen.discovery;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.DiscoveryImporter;
import com.google.protobuf.Field;
import com.google.protobuf.Field.Cardinality;
import com.google.protobuf.Method;
import com.google.protobuf.Type;

public class ApiaryConfigToSampleConfigConverter {

  private static final String EMPTY_TYPE_URL = "Empty";
  private static final String KEY_FIELD_NAME = "key";
  private static final String VALUE_FIELD_NAME = "value";
  private static final String PAGE_TOKEN_FIELD_NAME = "nextPageToken";

  /**
   * Converts the provided configuration into a SampleConfig.
   */
  public static SampleConfig convert(Method method, ApiaryConfig apiaryConfig) {
    String apiName = apiaryConfig.getApiName();
    String apiVersion = apiaryConfig.getApiVersion();
    String apiNameVersion = String.join(".", apiName, apiVersion);
    return SampleConfig.newBuilder()
        .apiTitle(apiaryConfig.getApiTitle())
        .apiName(apiName)
        .apiVersion(apiVersion)
        .apiNameVersion(apiNameVersion)
        .apiTypeName(apiName)
        .apiTypeNameOverride("")
        .methods(createMethods(method, apiaryConfig))
        .build();
  }

  /**
   * Creates a method.
   */
  private static Map<String, MethodInfo> createMethods(Method method, ApiaryConfig apiaryConfig) {
    String methodName = method.getName();
    LinkedList<String> nameComponents = new LinkedList<>(Arrays.asList(methodName.split("\\.")));
    nameComponents.removeFirst(); // Removes the API name.

    Map<String, FieldInfo> fields = new HashMap<>();
    TypeInfo requestBodyType = null;
    for (String fieldName : apiaryConfig.getMethodParams(methodName)) {
      Field field =
          apiaryConfig.getField(apiaryConfig.getType(method.getRequestTypeUrl()), fieldName);
      // If one of the method arguments is a Message, we parse that separately
      // as the request body.
      if (fieldName.equals(DiscoveryImporter.REQUEST_FIELD_NAME)) {
        requestBodyType = createTypeInfo(field, method, apiaryConfig);
        continue;
      }
      fields.put(field.getName(), createFieldInfo(field, method, apiaryConfig));
    }

    TypeInfo requestType = createTypeInfo(method, true);
    TypeInfo responseType = null;
    String responseTypeUrl = method.getResponseTypeUrl();
    if (!responseTypeUrl.equals(DiscoveryImporter.EMPTY_TYPE_NAME)
        && !responseTypeUrl.equals(EMPTY_TYPE_URL)) {
      responseType = createTypeInfo(method, false);
    }

    boolean isPageStreaming = isPageStreaming(method, apiaryConfig);
    FieldInfo pageStreamingResourceField = null;
    if (isPageStreaming) {
      pageStreamingResourceField =
          createFieldInfo(
              getPageStreamingResouceField(apiaryConfig.getType(method.getResponseTypeUrl())),
              method,
              apiaryConfig);
    }
    MethodInfo methodInfo =
        MethodInfo.newBuilder()
            .nameComponents(nameComponents)
            .fields(fields)
            .requestType(requestType)
            .requestBodyType(requestBodyType)
            .responseType(responseType)
            .isPageStreaming(isPageStreaming)
            .pageStreamingResourceField(pageStreamingResourceField)
            .build();
    return Collections.singletonMap(method.getName(), methodInfo);
  }

  /**
   * Creates a field.
   */
  private static FieldInfo createFieldInfo(Field field, Method method, ApiaryConfig apiaryConfig) {
    return FieldInfo.newBuilder()
        .name(field.getName())
        .description(apiaryConfig.getDescription(method.getRequestTypeUrl(), field.getName()))
        .type(createTypeInfo(field, method, apiaryConfig))
        .build();
  }

  /**
   * Creates the type of a field.
   */
  private static TypeInfo createTypeInfo(Field field, Method method, ApiaryConfig apiaryConfig) {
    boolean isMap =
        apiaryConfig.getAdditionalProperties(method.getResponseTypeUrl(), field.getName()) != null;
    boolean isArray = !isMap && (field.getCardinality() == Cardinality.CARDINALITY_REPEATED);

    TypeInfo mapKey = null;
    TypeInfo mapValue = null;
    boolean isMessage = false;
    MessageTypeInfo messageTypeInfo = null;

    if (isMap) {
      Type type = apiaryConfig.getType(field.getTypeUrl());
      mapKey = createTypeInfo(apiaryConfig.getField(type, KEY_FIELD_NAME), method, apiaryConfig);
      mapValue =
          createTypeInfo(apiaryConfig.getField(type, VALUE_FIELD_NAME), method, apiaryConfig);
    } else if (field.getKind() == Field.Kind.TYPE_MESSAGE) {
      isMessage = true;
      messageTypeInfo = createMessageTypeInfo(field, method, apiaryConfig, false);
    }
    return TypeInfo.newBuilder()
        .kind(field.getKind())
        .isMap(isMap)
        .mapKey(mapKey)
        .mapValue(mapValue)
        .isArray(isArray)
        .isMessage(isMessage)
        .message(messageTypeInfo)
        .build();
  }

  /**
   * Creates the type of a method's request and response messages.
   *
   * Serves as a wrapper over createMessageInfo that produces a message type
   * which contains only the type's name. If isRequest is true, the type name
   * will be "request$", and the correct upper-camel type name otherwise.
   */
  private static TypeInfo createTypeInfo(Method method, boolean isRequest) {
    String typeName =
        isRequest ? DiscoveryImporter.REQUEST_FIELD_NAME : method.getResponseTypeUrl();
    MessageTypeInfo messageTypeInfo =
        MessageTypeInfo.newBuilder()
            .typeName(typeName)
            .typeNameOverride("")
            .fields(new HashMap<String, FieldInfo>())
            .build();
    return TypeInfo.newBuilder()
        .kind(Field.Kind.TYPE_MESSAGE)
        .isMap(false)
        .mapKey(null)
        .mapValue(null)
        .isArray(false)
        .isMessage(true)
        .message(messageTypeInfo)
        .build();
  }

  /**
   * Creates a message type from a type and a field.
   *
   * If deep is false, the fields of the message are not generated. This is
   * mostly to avoid cycles.
   */
  private static MessageTypeInfo createMessageTypeInfo(
      Field field, Method method, ApiaryConfig apiaryConfig, boolean deep) {
    String typeName = field.getTypeUrl();
    Type type = apiaryConfig.getType(typeName);
    Map<String, FieldInfo> fields = new HashMap<>();
    if (deep) {
      for (Field field2 : type.getFieldsList()) {
        fields.put(field2.getName(), createFieldInfo(field2, method, apiaryConfig));
      }
    }
    return MessageTypeInfo.newBuilder()
        .typeName(typeName)
        .typeNameOverride("")
        .fields(fields)
        .build();
  }

  /**
   * Returns true if method is page streaming.
   */
  private static boolean isPageStreaming(Method method, ApiaryConfig apiaryConfig) {
    Type type = apiaryConfig.getType(method.getResponseTypeUrl());
    if (type == null) {
      return false;
    }
    // If the response type contains a field named "nextPageToken", we can
    // safely assume that the method is page streaming.
    for (Field field : type.getFieldsList()) {
      if (field.getName().equals(PAGE_TOKEN_FIELD_NAME)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the resource field of a page streaming response type.
   */
  private static Field getPageStreamingResouceField(Type type) {
    // We assume the first field with repeated cardinality is the right one.
    for (Field field : type.getFieldsList()) {
      if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
        return field;
      }
    }
    return null;
  }
}

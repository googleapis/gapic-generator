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

import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.DiscoveryImporter;
import com.google.api.codegen.util.Name;
import com.google.protobuf.Field;
import com.google.protobuf.Field.Cardinality;
import com.google.protobuf.Method;
import com.google.protobuf.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ApiaryConfigToSampleConfigConverter {

  private static final String EMPTY_TYPE_URL = "Empty";
  private static final String KEY_FIELD_NAME = "key";
  private static final String VALUE_FIELD_NAME = "value";
  private static final String PAGE_TOKEN_FIELD_NAME = "nextPageToken";

  public static SampleConfig convert(Method method, ApiaryConfig apiaryConfig) {
    return SampleConfig.newBuilder()
        .apiTitle(apiaryConfig.getApiTitle())
        .apiName(apiaryConfig.getApiName())
        .apiVersion(apiaryConfig.getApiVersion())
        .methodInfo(createMethodInfo(method, apiaryConfig))
        .build();
  }

  private static boolean isPageStreaming(Method method, ApiaryConfig apiaryConfig) {
    Type type = apiaryConfig.getType(method.getResponseTypeUrl());
    if (type == null) {
      return false;
    }
    for (Field field : type.getFieldsList()) {
      System.out.println("\tfieldName: " + field.getName());
      if (field.getName().equals(PAGE_TOKEN_FIELD_NAME)) {
        return true;
      }
    }
    return false;
  }

  private static TypeInfo createTypeInfoFromField(
      Field field, Method method, ApiaryConfig apiaryConfig) {
    TypeInfo.Builder typeInfo = TypeInfo.newBuilder();

    String fieldName = field.getName();
    typeInfo.kind(field.getKind());

    boolean isMap =
        apiaryConfig.getAdditionalProperties(method.getResponseTypeUrl(), fieldName) != null;
    boolean isArray = !isMap && (field.getCardinality() == Cardinality.CARDINALITY_REPEATED);
    typeInfo.isMap(isMap);

    typeInfo.isArray(isArray);

    typeInfo.isMessage(false);
    typeInfo.message(null);

    Type type = apiaryConfig.getType(field.getTypeUrl());
    if (isMap) {
      typeInfo.mapKey(
          createTypeInfoFromField(
              apiaryConfig.getField(type, KEY_FIELD_NAME), method, apiaryConfig));
      typeInfo.mapValue(
          createTypeInfoFromField(
              apiaryConfig.getField(type, VALUE_FIELD_NAME), method, apiaryConfig));
    } else if (field.getKind() == Field.Kind.TYPE_MESSAGE) {
      typeInfo.isMessage(true);
      typeInfo.message(createMessageTypeInfo(type, field, method, apiaryConfig, false));
    }

    return typeInfo.build();
  }

  private static String getPackagePrefix(ApiaryConfig apiaryConfig) {
    return String.join(".", Arrays.asList(apiaryConfig.getApiName(), apiaryConfig.getApiVersion()));
  }

  private static TypeInfo createTypeInfoFromType(
      Method method, ApiaryConfig apiaryConfig, boolean isRequest) {
    TypeInfo.Builder typeInfo = TypeInfo.newBuilder();
    typeInfo.kind(Field.Kind.TYPE_MESSAGE);
    typeInfo.isMap(false);
    typeInfo.isArray(false);
    typeInfo.isMessage(true);

    MessageTypeInfo.Builder messageTypeInfo = MessageTypeInfo.newBuilder();
    if (isRequest) {
      LinkedList<String> pieces = new LinkedList<>(Arrays.asList(method.getName().split("\\.")));
      // TODO(garrettjones): Should I do this differently?
      messageTypeInfo.name(Name.lowerCamel(pieces.removeLast()).toUpperCamel());
      messageTypeInfo.packagePath(String.join(".", pieces));
    } else {
      messageTypeInfo.name(method.getResponseTypeUrl());
      messageTypeInfo.packagePath("");
    }
    messageTypeInfo.packagePrefix(getPackagePrefix(apiaryConfig));
    messageTypeInfo.fields(new ArrayList<FieldInfo>());
    typeInfo.message(messageTypeInfo.build());
    return typeInfo.build();
  }

  private static MessageTypeInfo createMessageTypeInfo(
      Type type, Field field, Method method, ApiaryConfig apiaryConfig, boolean deep) {
    MessageTypeInfo.Builder messageTypeInfo = MessageTypeInfo.newBuilder();
    messageTypeInfo.name(field.getTypeUrl());
    messageTypeInfo.packagePrefix(getPackagePrefix(apiaryConfig));
    messageTypeInfo.packagePath("");
    List<FieldInfo> fields = new ArrayList<>();
    if (deep) {
      for (Field field2 : type.getFieldsList()) {
        fields.add(createFieldInfo(field2, method, apiaryConfig));
      }
    }
    messageTypeInfo.fields(fields);
    return messageTypeInfo.build();
  }

  private static FieldInfo createFieldInfo(Field field, Method method, ApiaryConfig apiaryConfig) {
    String fieldName = field.getName();
    return FieldInfo.newBuilder()
        .name(fieldName)
        .description(apiaryConfig.getDescription(method.getRequestTypeUrl(), fieldName))
        .type(createTypeInfoFromField(field, method, apiaryConfig))
        .build();
  }

  private static MethodInfo createMethodInfo(Method method, ApiaryConfig apiaryConfig) {
    MethodInfo.Builder methodInfo = MethodInfo.newBuilder();
    String methodName = method.getName();

    LinkedList<String> resources = new LinkedList<>(Arrays.asList(methodName.split("\\.")));
    resources.removeFirst(); // Removes the API name prefix from the methodName.
    // While there is a separate function for retrieving method resources from
    // apiaryConfig, it's perhaps better to rely on a single source (methodName)
    // for the information. When overrides via yaml are available, it's
    // preferred to only have to edit one variable.
    methodInfo.resources(resources);
    methodInfo.name(methodName);
    //methodInfo.inputType(createTypeInfo(createMessageTypeInfo(methodName)));
    methodInfo.inputType(createTypeInfoFromType(method, apiaryConfig, true));
    boolean isPageStreaming = isPageStreaming(method, apiaryConfig);
    methodInfo.pageStreamingResourceField(null);

    List<FieldInfo> fields = new ArrayList<>();
    Type type = apiaryConfig.getType(method.getRequestTypeUrl());
    // TODO(saicheems): Clean up everything below :(
    methodInfo.isPageStreaming(isPageStreaming);
    for (String fieldName : apiaryConfig.getMethodParams(methodName)) {
      Field field = apiaryConfig.getField(type, fieldName);
      // If one of the method arguments is a Message, we parse that separately.
      if (fieldName.equals(DiscoveryImporter.REQUEST_FIELD_NAME)) {
        methodInfo.inputRequestType(createTypeInfoFromField(field, method, apiaryConfig));
        continue;
      }
      fields.add(createFieldInfo(field, method, apiaryConfig));
    }
    methodInfo.fields(fields);

    if (isPageStreaming) {
      Field pageStreamingResourceField =
          getPageStreamingResouceField(apiaryConfig.getType(method.getResponseTypeUrl()));
      FieldInfo fieldInfo = createFieldInfo(pageStreamingResourceField, method, apiaryConfig);
      methodInfo.pageStreamingResourceField(fieldInfo);
    }

    String responseTypeUrl = method.getResponseTypeUrl();
    if (!responseTypeUrl.equals(DiscoveryImporter.EMPTY_TYPE_NAME)
        && !responseTypeUrl.equals(EMPTY_TYPE_URL)) {
      methodInfo.outputType(createTypeInfoFromType(method, apiaryConfig, false));
    }
    return methodInfo.build();
  }

  private static Field getPageStreamingResouceField(Type type) {
    for (Field field : type.getFieldsList()) {
      if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
        return field;
      }
    }
    return null;
  }
}

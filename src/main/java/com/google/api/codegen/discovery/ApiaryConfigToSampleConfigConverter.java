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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import com.google.api.client.util.Strings;
import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.DiscoveryImporter;
import com.google.api.codegen.util.Name;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Field;
import com.google.protobuf.Field.Cardinality;
import com.google.protobuf.Method;
import com.google.protobuf.Type;

public class ApiaryConfigToSampleConfigConverter {

  private static final String EMPTY_TYPE_URL = "Empty";
  private static final String KEY_FIELD_NAME = "key";
  private static final String VALUE_FIELD_NAME = "value";
  private static final String PAGE_TOKEN_FIELD_NAME = "pageToken";

  public static SampleConfig convert(Method method, ApiaryConfig apiaryConfig) {
    return SampleConfig.newBuilder()
        .apiTitle(apiaryConfig.getApiTitle())
        .apiName(apiaryConfig.getApiName())
        .apiVersion(apiaryConfig.getApiVersion())
        .methodInfo(createMethodInfo(method, apiaryConfig))
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
    methodInfo.inputType(createTypeInfo(createMessageTypeInfo(methodName)));
    boolean isPageStreaming = false;
    methodInfo.pageStreamingResourceField(null);

    List<FieldInfo> fields = new ArrayList<>();
    Type type = apiaryConfig.getType(method.getRequestTypeUrl());
    for (Field field : apiaryConfig.getType(method.getRequestTypeUrl()).getFieldsList()) {
      if (field.getName().equals(PAGE_TOKEN_FIELD_NAME)) {
        isPageStreaming = true;
        break;
      }
    }
    // TODO(saicheems): Clean up everything below :(
    methodInfo.isPageStreaming(isPageStreaming);
    for (String fieldName : apiaryConfig.getMethodParams(methodName)) {
      // If one of the method arguments is a Message, we parse that separately.
      if (fieldName.equals(DiscoveryImporter.REQUEST_FIELD_NAME)) {
        MessageTypeInfo.Builder requestBodyType = MessageTypeInfo.newBuilder();
        Field field =
            apiaryConfig.getField(apiaryConfig.getType(method.getRequestTypeUrl()), fieldName);
        requestBodyType.name(field.getTypeUrl());
        requestBodyType.packagePath("");
        requestBodyType.fields(new ArrayList<FieldInfo>());
        methodInfo.inputRequestType(createTypeInfo(requestBodyType.build()));
        continue;
      }
      Field field = apiaryConfig.getField(type, fieldName);
      fields.add(
          createFieldInfo(
              createTypeInfo(field, method, apiaryConfig, 0), field, method, apiaryConfig));
    }
    methodInfo.fields(fields);

    if (isPageStreaming) {
      Field pageStreamingResourceField =
          getPageStreamingResouceField(apiaryConfig.getType(method.getResponseTypeUrl()));
      FieldInfo fieldInfo =
          createFieldInfo(
              createTypeInfo(pageStreamingResourceField, method, apiaryConfig, 0),
              pageStreamingResourceField,
              method,
              apiaryConfig);
      methodInfo.pageStreamingResourceField(fieldInfo);
    }

    String responseTypeUrl = method.getResponseTypeUrl();
    if (!responseTypeUrl.equals(DiscoveryImporter.EMPTY_TYPE_NAME)
        && !responseTypeUrl.equals(EMPTY_TYPE_URL)) {
      methodInfo.outputType(
          createTypeInfo(
              createMessageTypeInfo(
                  apiaryConfig.getType(method.getResponseTypeUrl()),
                  method,
                  apiaryConfig,
                  false,
                  0)));
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

  private static FieldInfo createFieldInfo(
      TypeInfo typeInfo, Field field, Method method, ApiaryConfig apiaryConfig) {
    String fieldName = field.getName();
    return FieldInfo.newBuilder()
        .name(fieldName)
        .description(apiaryConfig.getDescription(method.getRequestTypeUrl(), fieldName))
        .type(typeInfo)
        .build();
  }

  private static TypeInfo createTypeInfo(
      Field field, Method method, ApiaryConfig apiaryConfig, int level) {
    // Apparently if we go deep enough we can hit a NullPointerException, it's
    // not necessary to recurse so deeply for types so let's only go surface
    // level.
    if (level > 1) {
      return null;
    }
    TypeInfo.Builder typeInfo = TypeInfo.newBuilder();
    String fieldName = field.getName();
    typeInfo.kind(field.getKind());

    boolean isMap =
        apiaryConfig.getAdditionalProperties(method.getResponseTypeUrl(), fieldName) != null;
    boolean isArray = !isMap && (field.getCardinality() == Cardinality.CARDINALITY_REPEATED);
    typeInfo.isMap(isMap);

    Type type = apiaryConfig.getType(field.getTypeUrl());
    if (isMap) {
      typeInfo.mapKey(
          createTypeInfo(
              apiaryConfig.getField(type, KEY_FIELD_NAME), method, apiaryConfig, level + 1));
      typeInfo.mapValue(
          createTypeInfo(
              apiaryConfig.getField(type, VALUE_FIELD_NAME), method, apiaryConfig, level + 1));
    }
    typeInfo.isArray(isArray);

    typeInfo.isMessage(false);
    typeInfo.message(null);
    if (field.getKind() == Field.Kind.TYPE_MESSAGE) {
      typeInfo.isMessage(true);
      typeInfo.message(createMessageTypeInfo(type, field, method, apiaryConfig, level + 1));
    }
    return typeInfo.build();
  }

  private static TypeInfo createTypeInfo(MessageTypeInfo messageTypeInfo) {
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

  private static MessageTypeInfo createMessageTypeInfo(
      Type type, Field field, Method method, ApiaryConfig apiaryConfig, int level) {
    MessageTypeInfo.Builder messageTypeInfo = MessageTypeInfo.newBuilder();
    messageTypeInfo.name(field.getTypeUrl());
    messageTypeInfo.packagePath(method.getName());
    List<FieldInfo> fields = new ArrayList<>();
    for (Field field2 : type.getFieldsList()) {
      fields.add(
          createFieldInfo(
              createTypeInfo(field2, method, apiaryConfig, level), field2, method, apiaryConfig));
    }
    messageTypeInfo.fields(fields);
    return messageTypeInfo.build();
  }

  private static MessageTypeInfo createMessageTypeInfo(
      Type type, Method method, ApiaryConfig apiaryConfig, boolean isRequestType, int level) {
    MessageTypeInfo.Builder messageTypeInfo = MessageTypeInfo.newBuilder();
    // Unfortunately we need isRequestType to get the right name. method.get(Request|Response)Name()
    // returns a synthetic label used by the DiscoveryImporter if it's a request, but the real name
    // if it's a response...
    if (isRequestType) {
      messageTypeInfo.name(apiaryConfig.getSyntheticNameMapping().get(method.getRequestTypeUrl()));
    } else {
      messageTypeInfo.name(method.getResponseTypeUrl());
    }
    messageTypeInfo.packagePath(method.getName());
    List<FieldInfo> fields = new ArrayList<>();
    // TODO(saicheems): While this section may be useful eventually, I don't want to write a
    // cycle detection algorithm right now, given that we don't use message
    // fields down the line. It's worth looking into though.
    for (Field field : type.getFieldsList()) {
      fields.add(
          createFieldInfo(
              createTypeInfo(field, method, apiaryConfig, level), field, method, apiaryConfig));
    }
    messageTypeInfo.fields(fields);
    return messageTypeInfo.build();
  }

  private static MessageTypeInfo createMessageTypeInfo(String methodName) {
    LinkedList<String> resources = new LinkedList<>(Arrays.asList(methodName.split("\\.")));

    String shortName = resources.removeLast();
    // This method should only be used to create a MessageTypeInfo from a
    // method's name, and so the final segment of the name should always be in
    // lower-camel format. Regardless, we enforce via try-catch just in case.
    try {
      shortName = Name.lowerCamel(shortName).toUpperCamel();
    } catch (IllegalArgumentException e) {
    }
    String packagePath = String.join(".", resources);
    return MessageTypeInfo.newBuilder()
        .name(shortName)
        .packagePath(packagePath)
        .fields(new ArrayList<FieldInfo>())
        .build();
  }
}

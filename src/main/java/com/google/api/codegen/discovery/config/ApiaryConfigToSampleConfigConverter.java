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
package com.google.api.codegen.discovery.config;

import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.DiscoveryImporter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import com.google.protobuf.Field;
import com.google.protobuf.Field.Kind;
import com.google.protobuf.Method;
import com.google.protobuf.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ApiaryConfigToSampleConfigConverter {

  private static final String KEY_FIELD_NAME = "key";
  private static final String VALUE_FIELD_NAME = "value";
  private static final ImmutableList<String> PAGE_TOKEN_NAMES =
      ImmutableList.of("pageToken", "nextPageToken");

  private final List<Method> methods;
  private final ApiaryConfig apiaryConfig;
  private final TypeNameGenerator typeNameGenerator;

  private final Map<String, List<String>> methodNameComponents;

  public ApiaryConfigToSampleConfigConverter(
      List<Method> methods, ApiaryConfig apiaryConfig, TypeNameGenerator typeNameGenerator) {
    this.methods = methods;
    this.apiaryConfig = apiaryConfig;
    this.typeNameGenerator = typeNameGenerator;
    typeNameGenerator.setApiCanonicalNameAndVersion(
        apiaryConfig.getServiceCanonicalName(), apiaryConfig.getApiVersion());

    methodNameComponents = new HashMap<String, List<String>>();
    // Since methodNameComponents are used to generate the request type name, we
    // produce them here for ease of access.
    for (Method method : methods) {
      LinkedList<String> split = new LinkedList<>(Arrays.asList(method.getName().split("\\.")));
      methodNameComponents.put(method.getName(), split);
    }
  }

  /** Converts the class' configuration into a SampleConfig. */
  public SampleConfig convert() {
    String apiName = apiaryConfig.getApiName();
    String apiVersion = typeNameGenerator.getApiVersion(apiaryConfig.getApiVersion());
    Map<String, MethodInfo> methods = new HashMap<String, MethodInfo>();
    for (Method method : this.methods) {
      methods.put(method.getName(), createMethod(method));
    }
    String apiTypeName = typeNameGenerator.getApiTypeName(apiaryConfig.getServiceCanonicalName());
    return SampleConfig.newBuilder()
        .apiTitle(apiaryConfig.getApiTitle())
        .apiCanonicalName(apiaryConfig.getServiceCanonicalName())
        .apiName(apiName)
        .apiVersion(apiVersion)
        .versionModule(apiaryConfig.getVersionModule())
        .apiTypeName(apiTypeName)
        .packagePrefix(
            typeNameGenerator.getPackagePrefix(
                apiName, apiaryConfig.getServiceCanonicalName(), apiVersion))
        .methods(methods)
        .authType(apiaryConfig.getAuthType())
        .authInstructionsUrl(apiaryConfig.getAuthInstructionsUrl())
        .build();
  }

  /** Creates a method. */
  private MethodInfo createMethod(Method method) {
    // The order of fields must be preserved, so we use an ImmutableMap.
    ImmutableMap.Builder<String, FieldInfo> fieldsBuilder = new ImmutableMap.Builder<>();
    TypeInfo requestBodyType = null;
    Type requestType = apiaryConfig.getType(method.getRequestTypeUrl());
    for (String fieldName : apiaryConfig.getMethodParams(method.getName())) {
      Field field = apiaryConfig.getField(requestType, fieldName);
      // If one of the method arguments has the field name "request$", it's the
      // request body.
      if (fieldName.equals(DiscoveryImporter.REQUEST_FIELD_NAME)) {
        requestBodyType = createTypeInfo(field, method);
        continue;
      }
      fieldsBuilder.put(field.getName(), createFieldInfo(field, requestType, method));
    }
    ImmutableMap<String, FieldInfo> fields = fieldsBuilder.build();

    TypeInfo requestTypeInfo = createTypeInfo(method, true);
    TypeInfo responseTypeInfo = null;
    String responseTypeUrl = typeNameGenerator.getResponseTypeUrl(method.getResponseTypeUrl());
    if (!Strings.isNullOrEmpty(responseTypeUrl)) {
      responseTypeInfo = createTypeInfo(method, false);
    }

    // Heuristic implementation interprets method to be page streaming iff one of the names
    // "pageToken" or "nextPageToken" occurs among the fields of both the method's response type and
    // either the method's request (query parameters) or request body.
    boolean isPageStreamingResourceSetterInRequestBody = false;
    String requestPageTokenName = "";
    if (requestBodyType != null) {
      Map<String, FieldInfo> requestBodyFields = requestBodyType.message().fields();
      for (String tokenName : PAGE_TOKEN_NAMES) {
        if (requestBodyFields.containsKey(tokenName)) {
          isPageStreamingResourceSetterInRequestBody = true;
          requestPageTokenName = tokenName;
          break;
        }
      }
    }
    boolean hasResponsePageToken = false;
    String responsePageTokenName = "";
    Type responseType = apiaryConfig.getType(method.getResponseTypeUrl());
    if (responseType != null) {
      String fieldName;
      FIELDS:
      for (Field field : responseType.getFieldsList()) {
        fieldName = field.getName();
        for (String tokenName : PAGE_TOKEN_NAMES) {
          if (fieldName.equals(tokenName)) {
            hasResponsePageToken = true;
            responsePageTokenName = tokenName;
            break FIELDS;
          }
        }
      }
    }
    boolean isPageStreaming = false;
    if (hasResponsePageToken) {
      if (isPageStreamingResourceSetterInRequestBody) {
        isPageStreaming = true;
      } else {
        Table<Type, String, Field> requestFields = apiaryConfig.getFields();
        for (String tokenName : PAGE_TOKEN_NAMES) {
          if (requestFields.contains(requestType, tokenName)) {
            isPageStreaming = true;
            requestPageTokenName = tokenName;
            break;
          }
        }
      }
    }
    FieldInfo pageStreamingResourceField = null;
    if (isPageStreaming) {
      Field field = getPageStreamingResourceField(responseType);
      pageStreamingResourceField = createFieldInfo(field, responseType, method);
    }

    boolean hasMediaUpload = apiaryConfig.getMediaUpload().contains(method.getName());

    MethodInfo methodInfo =
        MethodInfo.newBuilder()
            .verb(apiaryConfig.getHttpMethod(method.getName()))
            .nameComponents(
                typeNameGenerator.getMethodNameComponents(
                    methodNameComponents.get(method.getName())))
            .fields(fields)
            .requestType(requestTypeInfo)
            .requestBodyType(requestBodyType)
            .responseType(responseTypeInfo)
            .isPageStreaming(isPageStreaming)
            .pageStreamingResourceField(pageStreamingResourceField)
            .isPageStreamingResourceSetterInRequestBody(isPageStreamingResourceSetterInRequestBody)
            .requestPageTokenName(requestPageTokenName)
            .responsePageTokenName(responsePageTokenName)
            .hasMediaUpload(hasMediaUpload)
            // Ignore media download for methods supporting media upload, as
            // Apiary cannot combine both in a single request, and no sensible
            // use cases are known for download with a method supporting upload.
            // https://developers.google.com/discovery/v1/using#discovery-doc-methods
            .hasMediaDownload(
                !hasMediaUpload && apiaryConfig.getMediaDownload().contains(method.getName()))
            .authScopes(apiaryConfig.getAuthScopes(method.getName()))
            .build();
    return methodInfo;
  }

  /** Creates a field. */
  private FieldInfo createFieldInfo(Field field, Type containerType, Method method) {
    String example = "";
    TypeInfo typeInfo = createTypeInfo(field, method);
    if (typeInfo.kind() == Field.Kind.TYPE_STRING) {
      String fieldPattern =
          apiaryConfig.getFieldPattern().get(containerType.getName(), field.getName());
      String stringFormat = apiaryConfig.getStringFormat(containerType.getName(), field.getName());
      example = typeNameGenerator.getFieldPatternExample(fieldPattern);
      if (!Strings.isNullOrEmpty(example)) {
        // Generates an example of the format: `ex: "projects/my-project/logs/my-log"`
        example = "ex: " + example;
      } else {
        example = typeNameGenerator.getStringFormatExample(stringFormat);
      }
    }
    return FieldInfo.newBuilder()
        .name(field.getName())
        .type(typeInfo)
        .cardinality(field.getCardinality())
        .example(example)
        .description(
            Strings.nullToEmpty(
                apiaryConfig.getDescription(method.getRequestTypeUrl(), field.getName())))
        .build();
  }

  /** Creates the type of a field. */
  private TypeInfo createTypeInfo(Field field, Method method) {
    boolean isMap =
        apiaryConfig.getAdditionalProperties(method.getResponseTypeUrl(), field.getName()) != null;
    boolean isArray = !isMap && (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED);

    TypeInfo mapKey = null;
    TypeInfo mapValue = null;
    boolean isMessage = false;
    MessageTypeInfo messageTypeInfo = null;

    if (isMap) {
      Type type = apiaryConfig.getType(field.getTypeUrl());
      mapKey = createTypeInfo(apiaryConfig.getField(type, KEY_FIELD_NAME), method);
      mapValue = createTypeInfo(apiaryConfig.getField(type, VALUE_FIELD_NAME), method);
    } else if (field.getKind() == Field.Kind.TYPE_MESSAGE) {
      isMessage = true;
      messageTypeInfo = createMessageTypeInfo(field, method, apiaryConfig, true);
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
   * <p>Serves as a wrapper over createMessageInfo that produces a message type which contains only
   * the type's name. The semantics of the method name change if the message is the request or
   * response type. For a request type, typeName is some combination of the methodNameComponents,
   * and for a response type, typeName is parsed from the configuration.
   */
  private TypeInfo createTypeInfo(Method method, boolean isRequest) {
    String typeName =
        isRequest
            ? typeNameGenerator.getRequestTypeName(methodNameComponents.get(method.getName()))
            : typeNameGenerator.getMessageTypeName(
                typeNameGenerator.getResponseTypeUrl(method.getResponseTypeUrl()));
    String subpackage = typeNameGenerator.getSubpackage(isRequest);
    MessageTypeInfo messageTypeInfo =
        MessageTypeInfo.newBuilder()
            .typeName(typeName)
            .subpackage(subpackage)
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
   * <p>If deep is false, the fields of the message are not explored or generated. Since there is no
   * detection and defense against cycles, only set deep to true if the fields of the message are
   * important.
   */
  private MessageTypeInfo createMessageTypeInfo(
      Field field, Method method, ApiaryConfig apiaryConfig, boolean deep) {
    Type type = apiaryConfig.getType(field.getTypeUrl());
    String typeName = typeNameGenerator.getMessageTypeName(field.getTypeUrl());
    Map<String, FieldInfo> fields = new HashMap<>();
    if (deep) {
      for (Field field2 : type.getFieldsList()) {
        // TODO(saicheems): We ignore messages as an easy way around cycles for the moment.
        if (field2.getKind() != Kind.TYPE_MESSAGE) {
          fields.put(field2.getName(), createFieldInfo(field2, type, method));
        }
      }
    }
    return MessageTypeInfo.newBuilder()
        .typeName(typeName)
        .subpackage(typeNameGenerator.getSubpackage(false))
        .fields(fields)
        .build();
  }

  /**
   * Returns the resource field of a page streaming response type.
   *
   * <p>The heuristic implemented returns the first field within type that has a repeated
   * cardinality, if one exists. Otherwise it returns the first field that is of type string.
   */
  private Field getPageStreamingResourceField(Type type) {
    // We assume the first field with repeated cardinality is the right one.
    for (Field field : type.getFieldsList()) {
      if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
        return field;
      }
    }
    // If there is no field of repeated cardinality then we assume the first
    // message of type string is the page streaming resource.
    for (Field field : type.getFieldsList()) {
      if (field.getKind() == Kind.TYPE_STRING) {
        return field;
      }
    }
    return null;
  }
}

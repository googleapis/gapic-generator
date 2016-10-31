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
package com.google.api.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.Documentation;
import com.google.api.Service;
import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableTable;
import com.google.protobuf.Api;
import com.google.protobuf.Field;
import com.google.protobuf.Field.Kind;
import com.google.protobuf.Method;
import com.google.protobuf.Type;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

// TODO(pongad): Move discovery related files into a separate dir.
// Delay for now to reduce merge conflict.

/**
 * DiscoveryImporter parses a Discovery Doc into a {@link com.google.api.Service} object and an
 * additional {@link ApiaryConfig} object for data not accommodated by Service.
 *
 * <p>The current implementation also provides a main method used for manual sanity checking.
 */
public class DiscoveryImporter {

  public static final String REQUEST_FIELD_NAME = "request$";
  public static final String EMPTY_TYPE_NAME = "empty$";
  public static final String EMPTY_TYPE_URL = "Empty";
  public static final String ELEMENTS_FIELD_NAME = "elements$";
  private static final String SYNTHETIC_NAME_PREFIX = "synthetic$";

  private Service service = null;
  private final Map<String, Type> types = new TreeMap<>();
  private final Map<String, Method> methods = new TreeMap<>();
  private final ApiaryConfig config = new ApiaryConfig();
  private int syntheticCount = 0;

  // Usage: Field.Kind kind = TYPE_TABLE.get(discoveryDocTypeName, discoveryDocFormatName)
  private static final ImmutableTable<String, String, Field.Kind> TYPE_TABLE =
      ImmutableTable.<String, String, Field.Kind>builder()
          .put("any", "", Field.Kind.TYPE_UNKNOWN)
          .put("boolean", "", Field.Kind.TYPE_BOOL)
          .put("integer", "int32", Field.Kind.TYPE_INT32)
          .put("integer", "uint32", Field.Kind.TYPE_UINT32)
          .put("number", "double", Field.Kind.TYPE_DOUBLE)
          .put("number", "float", Field.Kind.TYPE_FLOAT)
          .put("string", "", Field.Kind.TYPE_STRING)
          .build();

  private static Iterable<Map.Entry<String, JsonNode>> iterFields(final JsonNode n) {
    return new Iterable<Map.Entry<String, JsonNode>>() {
      @Override
      public Iterator<Map.Entry<String, JsonNode>> iterator() {
        if (n != null) {
          return n.fields();
        }
        return Collections.<Map.Entry<String, JsonNode>>emptyIterator();
      }
    };
  }

  private static Iterable<JsonNode> elements(final JsonNode n) {
    return new Iterable<JsonNode>() {
      @Override
      public Iterator<JsonNode> iterator() {
        if (n != null) {
          return n.elements();
        }
        return Collections.<JsonNode>emptyIterator();
      }
    };
  }

  /**
   * Parses the file.
   *
   * <p>Imports all RPC methods under "resources" and all types under "schemas". Since the discovery
   * doc and Service have different ways to handle nested structures, "synthetic" types are made as
   * a glue layer. All such types have names beginning with "synthetic$"
   */
  public static DiscoveryImporter parse(Reader reader) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode disco = mapper.readTree(reader);

    DiscoveryImporter importer = new DiscoveryImporter();
    for (Map.Entry<String, JsonNode> field : iterFields(disco.get("schemas"))) {
      importer.addType(field.getKey(), field.getValue());
    }
    importer.addMethods(disco);

    Service.Builder builder = Service.newBuilder().setName(disco.get("baseUrl").asText());

    Documentation.Builder docs = Documentation.newBuilder();
    if (disco.get("documentationLink") != null) {
      docs.setDocumentationRootUrl(disco.get("documentationLink").asText());
    }
    builder.setTitle(disco.get("title").asText());
    // substitute Documentation overview field for lack of API revision field
    if (disco.get("revision") != null) {
      docs.setOverview(disco.get("revision").asText());
    } else {
      docs.setOverview("0");
    }
    builder.setDocumentation(docs.build());

    for (Type type : importer.types.values()) {
      builder.addTypes(type);
    }
    String apiName = disco.get("name").asText();
    String apiVersion = disco.get("version").asText();
    Api.Builder apiBuilder = Api.newBuilder().setName(apiName).setVersion(apiVersion);
    for (Method method : importer.methods.values()) {
      apiBuilder.addMethods(method);
    }
    Service serv = builder.addApis(apiBuilder).build();
    importer.service = serv;

    importer.config.setApiTitle(serv.getTitle());
    importer.config.setApiName(serv.getApis(0).getName());
    importer.config.setApiVersion(serv.getApis(0).getVersion());

    importer.config.getTypes().putAll(importer.types);
    for (Type type : importer.types.values()) {
      for (Field field : type.getFieldsList()) {
        importer.config.getFields().put(type, field.getName(), field);
      }
    }
    if (disco.get("canonicalName") != null) {
      importer.config.setServiceCanonicalName(disco.get("canonicalName").asText());
    } else {
      importer.config.setServiceCanonicalName(lowerCamelToUpperCamel(apiName));
    }
    importer.config.setServiceVersion(apiVersion);

    return importer;
  }

  public Service getService() {
    return service;
  }

  public Map<String, Type> getTypes() {
    return types;
  }

  public Map<String, Method> getMethods() {
    return methods;
  }

  public ApiaryConfig getConfig() {
    return config;
  }

  /** Parses the type and adds it to {@code types}. Used for both top-level types and synthetics. */
  private void addType(String name, JsonNode root) {
    Type.Builder builder = Type.newBuilder();
    builder.setName(name);
    for (Map.Entry<String, JsonNode> field : iterFields(root.get("properties"))) {
      builder.addFields(fieldFrom(name, field.getKey(), field.getValue()));
    }
    types.put(name, builder.build());
  }

  /** Parses a field of an object. Might create more synthetic types to express nested types. */
  private Field fieldFrom(String typeName, String fieldName, JsonNode root) {
    if (root.get("description") != null) {
      config.getFieldDescription().put(typeName, fieldName, root.get("description").asText());
    }

    Field.Builder builder = Field.newBuilder();
    builder.setName(fieldName);
    if (root.get("$ref") != null) {
      return builder
          .setKind(Field.Kind.TYPE_MESSAGE)
          .setCardinality(Field.Cardinality.CARDINALITY_OPTIONAL)
          .setTypeUrl(root.get("$ref").asText())
          .build();
    }

    if (root.get("type") == null) {
      throw new IllegalArgumentException(
          "unspecified type for field " + fieldName + " in type " + typeName);
    }

    if (root.get("repeated") != null && root.get("repeated").asBoolean()) {
      return arrayFieldFrom(builder, typeName, fieldName, root);
    }

    String typeText = root.get("type").asText();
    if (root.get("enum") != null) {
      return builder
          .setCardinality(Field.Cardinality.CARDINALITY_OPTIONAL)
          .setKind(Kind.TYPE_ENUM)
          .setTypeUrl(typeText)
          .build();
    }

    if (TYPE_TABLE.containsRow(typeText)) {
      return builder
          .setCardinality(Field.Cardinality.CARDINALITY_OPTIONAL)
          .setKind(
              getFieldKind(typeName, fieldName, typeText, root.get("format"), root.get("pattern")))
          .build();
    }

    if (typeText.equals("array")) {
      return arrayFieldFrom(builder, typeName, fieldName, root.get("items"));
    }

    if (typeText.equals("object")) {
      if (root.get("additionalProperties") != null) {
        String propertyType = createSyntheticTypeForProperty(root.get("additionalProperties"));
        config.getAdditionalProperties().put(typeName, fieldName, true);
        return builder
            .setKind(Field.Kind.TYPE_MESSAGE)
            .setTypeUrl(propertyType)
            .setCardinality(Field.Cardinality.CARDINALITY_REPEATED)
            .build();
      } else {
        String propertyType = typeName + "." + lowerCamelToUpperCamel(fieldName);
        addType(propertyType, root);
        return builder
            .setKind(Field.Kind.TYPE_MESSAGE)
            .setTypeUrl(propertyType)
            .setCardinality(Field.Cardinality.CARDINALITY_OPTIONAL)
            .build();
      }
    }
    throw new IllegalArgumentException("unknown type: " + typeText);
  }

  private Field arrayFieldFrom(
      Field.Builder builder, String typeName, String fieldName, JsonNode items) {
    builder.setCardinality(Field.Cardinality.CARDINALITY_REPEATED);

    if (items.get("type") != null) {
      String typeText = items.get("type").asText();
      if (TYPE_TABLE.containsRow(typeText)) {
        return builder
            .setKind(
                getFieldKind(
                    typeName, fieldName, typeText, items.get("format"), items.get("pattern")))
            .build();
      } else if (typeText.equals("object")) {
        String elementTypeName = typeName + "." + lowerCamelToUpperCamel(fieldName);
        addType(elementTypeName, items);
        return builder.setKind(Field.Kind.TYPE_MESSAGE).setTypeUrl(elementTypeName).build();
      } else if (typeText.equals("array")) {
        String elementTypeName = getNewSyntheticName();
        Type elementType =
            Type.newBuilder()
                .setName(elementTypeName)
                .addFields(fieldFrom(elementTypeName, ELEMENTS_FIELD_NAME, items))
                .build();
        types.put(elementTypeName, elementType);
        return builder.setKind(Field.Kind.TYPE_MESSAGE).setTypeUrl(elementTypeName).build();
      }
      throw new IllegalArgumentException("unknown type: " + typeText);
    }

    if (items.get("$ref") != null) {
      return builder
          .setKind(Field.Kind.TYPE_MESSAGE)
          .setTypeUrl(items.get("$ref").asText())
          .build();
    }
    throw new IllegalArgumentException("cannot find type of array: " + fieldName);
  }

  /**
   * Parses {@code root} as a member of "additionalProperties" of an object and adds it to {@code
   * types}.
   *
   * <p>Properties are expressed as types, but are not strictly types as defined by discovery. They
   * are not to be instantiated. Rather they provide a "schema" describing how data should be laid
   * out.
   */
  private String createSyntheticTypeForProperty(JsonNode root) {
    String typeName = getNewSyntheticName();
    String fieldName = "key";
    Field.Kind valueKind;
    String valueType;

    if (root.get("type") != null) {
      String valueTypeText = root.get("type").asText();
      if (TYPE_TABLE.containsRow(valueTypeText)) {
        valueKind =
            getFieldKind(
                typeName, fieldName, valueTypeText, root.get("format"), root.get("pattern"));
        valueType = null;
      } else if (valueTypeText.equals("object") || valueTypeText.equals("array")) {
        valueKind = Field.Kind.TYPE_MESSAGE;
        valueType = getNewSyntheticName();
        addType(valueType, root);
      } else {
        throw new IllegalArgumentException("unknown type: " + valueTypeText);
      }
    } else if (root.get("$ref") != null) {
      valueKind = Field.Kind.TYPE_MESSAGE;
      valueType = root.get("$ref").asText();
    } else {
      throw new IllegalArgumentException("making map without property");
    }

    Field.Builder valueBuilder = Field.newBuilder().setName("value").setKind(valueKind);
    if (valueType != null) {
      valueBuilder.setTypeUrl(valueType);
    }

    Type synthetic =
        Type.newBuilder()
            .setName(typeName)
            .addFields(Field.newBuilder().setName(fieldName).setKind(Field.Kind.TYPE_STRING))
            .addFields(valueBuilder)
            .build();
    types.put(typeName, synthetic);
    return typeName;
  }

  private String getNewSyntheticName() {
    String name = SYNTHETIC_NAME_PREFIX + syntheticCount;
    syntheticCount++;
    return name;
  }

  /**
   * Parses and adds methods listed in {@code root} into {@code methods}. For each method, its
   * namespace is recorded in {@link ApiaryConfig#resources} and its parameter order in {@link
   * ApiaryConfig#methodParams}.
   */
  private void addMethods(JsonNode root) {
    addMethods(root, new ArrayDeque<String>());
  }

  private void addMethods(JsonNode root, Deque<String> resources) {
    for (Map.Entry<String, JsonNode> resource : iterFields(root.get("resources"))) {
      resources.addLast(resource.getKey());
      addMethods(resource.getValue(), resources);
      resources.removeLast();
    }
    for (Map.Entry<String, JsonNode> method : iterFields(root.get("methods"))) {
      Method m = methodFrom(method.getValue());
      methods.put(m.getName(), m);
      config.getResources().putAll(m.getName(), resources);
    }
  }

  /**
   * Parses a single method.
   *
   * <p>In discovery, a method can take multiple parameters, but in Service they can only take one.
   * For this reason, a synthetic type is created for each method to "pull together" the parameters.
   * For example, if a discovery-doc method takes two parameters, a string {@code s} and a number
   * {@code i}, it will be instead structured as having one parameter. The type of the parameter
   * will be a message with two fields: a string {@code s} and a number {@code i}.
   */
  private Method methodFrom(JsonNode root) {
    String methodName = root.get("id").asText();
    Method.Builder builder = Method.newBuilder().setName(methodName);
    if (root.get("httpMethod") != null) {
      config.getFieldHttpMethod().put(methodName, root.get("httpMethod").asText());
    }

    String synthetic = getNewSyntheticName();
    Type.Builder typeBuilder = Type.newBuilder().setName(synthetic);
    for (Map.Entry<String, JsonNode> field : iterFields(root.get("parameters"))) {
      typeBuilder.addFields(fieldFrom(synthetic, field.getKey(), field.getValue()));
    }

    for (JsonNode param : elements(root.get("parameterOrder"))) {
      config.getMethodParams().put(methodName, param.asText());
    }

    if (root.get("request") != null) {
      typeBuilder.addFields(fieldFrom(synthetic, REQUEST_FIELD_NAME, root.get("request")));
      config.getMethodParams().put(methodName, REQUEST_FIELD_NAME);
    }

    builder.setResponseTypeUrl(responseTypeUrl(root));

    Type type = typeBuilder.build();
    types.put(synthetic, type);
    builder.setRequestTypeUrl(synthetic);

    if (root.get("supportsMediaUpload") != null && root.get("supportsMediaUpload").asBoolean()) {
      config.getMediaUpload().add(methodName);
    }

    if (root.get("supportsMediaDownload") != null
        && root.get("supportsMediaDownload").asBoolean()) {
      config.getMediaDownload().add(methodName);
    }

    for (JsonNode scope : elements(root.get("scopes"))) {
      config.getAuthScopes().put(methodName, scope.asText());
    }

    // TODO: add google.api.http
    return builder.build();
  }

  private String responseTypeUrl(JsonNode root) {
    JsonNode response = root.get("response");
    if (response == null) {
      return EMPTY_TYPE_NAME;
    }
    return response.get("$ref").asText();
  }

  /**
   * Maps the discovery doc type ({@code kindName}) and format {@code formatNode} into {@link
   * Field.Kind}.
   *
   * <p>If {@code kindName} is not {@code "string"}, {@code TYPE_TABLE} is consulted for the
   * appropriate {@link Field.Kind}.
   *
   * <p>Otherwise, if {@code kindName} is {@code "int64"} or {@code "uint64"}, the corresponding
   * {@link Field.Kind} is returned.
   *
   * <p>Otherwise, the returned {@link Field.Kind} is simply {@link Field.Kind.TYPE_STRING}, and its
   * format, if exists, is recorded in {@link ApiaryConfig#stringFormat}.
   */
  private Field.Kind getFieldKind(
      String type, String field, String kindName, JsonNode formatNode, JsonNode patternNode) {
    String format = "";
    if (formatNode != null) {
      format = formatNode.asText();
    }
    if (kindName.equals("string")) {
      if (formatNode != null) {
        switch (format) {
          case "int64":
            return Field.Kind.TYPE_INT64;
          case "uint64":
            return Field.Kind.TYPE_UINT64;
          default:
            config.getStringFormat().put(type, field, format);
            // fall through
        }
      }
      if (patternNode != null) {
        config.getFieldPattern().put(type, field, patternNode.asText());
      }
      return Field.Kind.TYPE_STRING;
    }
    Field.Kind kind = TYPE_TABLE.get(kindName, format);
    if (kind == null) {
      throw new IllegalArgumentException(String.format("unknown type: %s(%s)", kindName, format));
    }
    return kind;
  }

  private static String lowerCamelToUpperCamel(String s) {
    return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, s);
  }
}

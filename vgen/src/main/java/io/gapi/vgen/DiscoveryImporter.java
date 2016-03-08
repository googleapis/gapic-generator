package io.gapi.vgen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.Documentation;
import com.google.api.Service;
import com.google.common.collect.ImmutableTable;
import com.google.protobuf.Api;
import com.google.protobuf.Field;
import com.google.protobuf.Method;
import com.google.protobuf.Type;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// TODO(pongad): Move discovery related files into a separate dir.
// Delay for now to reduce merge conflict.

/**
 * DiscoveryImporter parses Discovery Doc into {@link com.google.api.Service} objects.
 *
 * The current implementation also provides a main method used for manual sanity checking.
 */
public class DiscoveryImporter {

  private static final String SYNTHETIC_NAME_PREFIX = "synthetic$";

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

  private static Iterable<Map.Entry<String, JsonNode>> iterFields(JsonNode n) {
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

  private static Iterable<JsonNode> elements(JsonNode n) {
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

  // Used for manual testing only; this should be deleted once fragment generation works
  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      System.out.println("need file");
      return;
    }
    for (String f : args) {
      System.out.println(String.format("=== %s ===", f));
      parseAndDump(f);
      System.out.println("");
    }
  }

  // Used for manual testing only; this should be deleted once fragment generation works
  private static void parseAndDump(String file) throws IOException {
    DiscoveryImporter importer = parse(new InputStreamReader(new FileInputStream(new File(file))));
    System.out.println(importer.service);
    System.out.println("apiParams:");
    for (Map.Entry<String, Collection<String>> entry :
        importer.config.apiParams.asMap().entrySet()) {
      System.out.println(entry.getKey() + " " + entry.getValue());
    }
    System.out.println("resources:");
    for (Map.Entry<String, Collection<String>> entry :
        importer.config.resources.asMap().entrySet()) {
      System.out.println(entry.getKey() + " " + entry.getValue());
    }
  }

  /**
   * Parses the file.
   *
   * Imports all RPC methods under "resources" and all types under "schemas".
   * Since the discovery doc and Service have different ways to handle nested structures,
   * "synthetic" types are made as a glue layer.
   * All such types have names beginning with "synthetic$"
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

    if (disco.get("title") != null) {
      builder.setTitle(disco.get("title").asText());
    }
    if (disco.get("documentationLink") != null) {
      builder.setDocumentation(
          Documentation.newBuilder()
              .setSummary(disco.get("title").asText())
              .setDocumentationRootUrl(disco.get("documentationLink").asText()));
    }

    for (Type type : importer.types.values()) {
      builder.addTypes(type);
    }
    Api.Builder apiBuilder =
        Api.newBuilder()
            .setName(disco.get("name").asText())
            .setVersion(disco.get("version").asText());
    for (Method method : importer.methods.values()) {
      apiBuilder.addMethods(method);
    }
    Service serv = builder.addApis(apiBuilder).build();
    importer.service = serv;
    importer.config.types = importer.types;
    return importer;
  }

  private Service service = null;
  private final Map<String, Type> types = new TreeMap<>();
  private final Map<String, Method> methods = new TreeMap<>();
  private final ApiaryConfig config = new ApiaryConfig();
  private int syntheticCount = 0;

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

  /**
   * Parses the type and adds it to {@code types}. Used for both top-level types and synthetics.
   */
  private void addType(String name, JsonNode root) {
    Type.Builder builder = Type.newBuilder();
    builder.setName(name);
    for (Map.Entry<String, JsonNode> field : iterFields(root.get("properties"))) {
      builder.addFields(fieldFrom(name, field.getKey(), field.getValue()));
    }
    types.put(name, builder.build());
  }

  /**
   * Parses a field of an object. Might create more synthetic types to express nested types.
   */
  private Field fieldFrom(String typeName, String fieldName, JsonNode root) {
    if (root.get("description") != null) {
      config.fieldDescription.put(typeName, fieldName, root.get("description").asText());
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
      return builder.build();
    }

    String typeText = root.get("type").asText();
    if (TYPE_TABLE.containsRow(typeText)) {
      return builder
          .setCardinality(Field.Cardinality.CARDINALITY_OPTIONAL)
          .setKind(getFieldKind(typeName, fieldName, typeText, root.get("format")))
          .build();
    }

    if (typeText.equals("array")) {
      builder.setCardinality(Field.Cardinality.CARDINALITY_REPEATED);

      if (root.get("items").get("type") != null) {
        typeText = root.get("items").get("type").asText();
        if (TYPE_TABLE.containsRow(typeText)) {
          return builder
              .setKind(getFieldKind(typeName, fieldName, typeText, root.get("items").get("format")))
              .build();
        } else if (typeText.equals("object")) {
          String elementTypeName = getNewSyntheticName();
          addType(elementTypeName, root.get("items"));
          return builder.setKind(Field.Kind.TYPE_MESSAGE).setTypeUrl(elementTypeName).build();
        } else if (typeText.equals("array")) {
          String elementTypeName = getNewSyntheticName();
          Type elementType =
              Type.newBuilder()
                  .setName(elementTypeName)
                  .addFields(fieldFrom(elementTypeName, "elements", root.get("items")))
                  .build();
          types.put(elementTypeName, elementType);
          return builder.setKind(Field.Kind.TYPE_MESSAGE).setTypeUrl(elementTypeName).build();
        }
        throw new IllegalArgumentException("unknown type: " + typeText);
      }

      if (root.get("items").get("$ref") != null) {
        return builder
            .setKind(Field.Kind.TYPE_MESSAGE)
            .setTypeUrl(root.get("items").get("$ref").asText())
            .build();
      }
      throw new IllegalArgumentException("cannot find type of array: " + fieldName);
    }

    if (typeText.equals("object")) {
      if (root.get("additionalProperties") != null) {
        String propertyType = createSyntheticTypeForProperty(root.get("additionalProperties"));
        builder.setTypeUrl(propertyType);
        config.additionalProperties.put(typeName, fieldName, true);
      } else {
        String propertyType = getNewSyntheticName();
        builder.setTypeUrl(propertyType);
        addType(propertyType, root);
      }
      return builder
          .setKind(Field.Kind.TYPE_MESSAGE)
          .setCardinality(Field.Cardinality.CARDINALITY_REPEATED)
          .build();
    }
    throw new IllegalArgumentException("unknown type: " + typeText);
  }

  /**
   * Parses {@code root} as a member of "additionalProperties" of an object
   * and adds it to {@code types}.
   *
   * Properties are expressed as types, but are not strictly types as defined by discovery.
   * They are not to be instantiated. Rather they provide a "schema" describing how data should
   * be laid out.
   */
  private String createSyntheticTypeForProperty(JsonNode root) {
    String typeName = getNewSyntheticName();
    String fieldName = "key";
    Field.Kind valueKind;
    String valueType;

    if (root.get("type") != null) {
      String valueTypeText = root.get("type").asText();
      if (TYPE_TABLE.containsRow(valueTypeText)) {
        valueKind = getFieldKind(typeName, fieldName, valueTypeText, root.get("format"));
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
   * Parses and adds methods listed in {@code root} into {@code methods}.
   * For each method, its namespace is recorded in {@link ApiaryConfig#resources}
   * and its parameter order in {@link ApiaryConfig#apiParams}.
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
      config.resources.putAll(m.getName(), resources);
    }
  }

  /**
   * Parses a single method.
   *
   * In discovery, a method can take multiple parameters, but in Service they can only take one.
   * For this reason, a synthetic type is created for each method to "pull together" the parameters.
   * For example, if a discovery-doc method takes two parameters,
   * a string {@code s} and a number {@code i},
   * it will be instead structured as having one parameter.
   * The type of the parameter will be a message with two fields:
   * a string {@code s} and a number {@code i}.
   */
  private Method methodFrom(JsonNode root) {
    String methodName = root.get("id").asText();
    Method.Builder builder = Method.newBuilder().setName(methodName);

    String synthetic = getNewSyntheticName();
    Type.Builder typeBuilder = Type.newBuilder().setName(synthetic);
    for (Map.Entry<String, JsonNode> field : iterFields(root.get("parameters"))) {
      typeBuilder.addFields(fieldFrom(synthetic, field.getKey(), field.getValue()));
    }

    for (JsonNode param : elements(root.get("parameterOrder"))) {
      config.apiParams.put(methodName, param.asText());
    }

    if (root.get("request") != null) {
      typeBuilder.addFields(fieldFrom(synthetic, "request$", root.get("request")));
      config.apiParams.put(methodName, "request$");
    }

    if (root.get("response") != null) {
      builder.setResponseTypeUrl(root.get("response").get("$ref").asText());
    } else {
      builder.setResponseTypeUrl("empty$"); // TODO(pongad): make explicit empty?
    }

    Type type = typeBuilder.build();
    types.put(synthetic, type);
    builder.setRequestTypeUrl(synthetic);

    // TODO: add google.api.http
    return builder.build();
  }

  /**
   * Maps the discovery doc type ({@code kindName}) and format {@code formatNode}
   * into {@link Field.Kind}.
   *
   * If {@code kindName} is not {@code "string"},
   * {@code TYPE_TABLE} is consulted for the appropriate {@link Field.Kind}.
   * Otherwise, the returned {@link Field.Kind} is simply {@link Field.Kind.TYPE_STRING},
   * and its format, if exists, is recorded in {@link ApiaryConfig#stringFormat}.
   */
  private Field.Kind getFieldKind(String type, String field, String kindName, JsonNode formatNode) {
    if (kindName.equals("string")) {
      if (formatNode != null) {
        config.stringFormat.put(type, field, formatNode.asText());
      }
      return Field.Kind.TYPE_STRING;
    }
    String format = "";
    if (formatNode != null) {
      format = formatNode.asText();
    }
    Field.Kind kind = TYPE_TABLE.get(kindName, format);
    if (kind == null) {
      throw new IllegalArgumentException(String.format("unknown type: %s(%s)", kindName, format));
    }
    return kind;
  }
}

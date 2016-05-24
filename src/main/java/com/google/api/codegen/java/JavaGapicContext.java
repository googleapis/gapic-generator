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
package com.google.api.codegen.java;

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.GapicContext;
import com.google.api.codegen.MethodConfig;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.FieldSelector;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.io.File;
import java.util.Arrays;

/**
 * A GapicContext specialized for Java.
 */
public class JavaGapicContext extends GapicContext implements JavaContext {

  /**
   * The package prefix protoc uses if no java package option was provided.
   */
  private static final String DEFAULT_JAVA_PACKAGE_PREFIX = "com.google.protos";

  /**
   * A map from primitive types in proto to Java counterparts.
   */
  private static final ImmutableMap<Type, String> PRIMITIVE_TYPE_MAP =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "boolean")
          .put(Type.TYPE_DOUBLE, "double")
          .put(Type.TYPE_FLOAT, "float")
          .put(Type.TYPE_INT64, "long")
          .put(Type.TYPE_UINT64, "long")
          .put(Type.TYPE_SINT64, "long")
          .put(Type.TYPE_FIXED64, "long")
          .put(Type.TYPE_SFIXED64, "long")
          .put(Type.TYPE_INT32, "int")
          .put(Type.TYPE_UINT32, "int")
          .put(Type.TYPE_SINT32, "int")
          .put(Type.TYPE_FIXED32, "int")
          .put(Type.TYPE_SFIXED32, "int")
          .put(Type.TYPE_STRING, "java.lang.String")
          .put(Type.TYPE_BYTES, "com.google.protobuf.ByteString")
          .build();

  /**
   * A map from primitive types in proto to zero value in Java
   */
  private static final ImmutableMap<Type, String> PRIMITIVE_ZERO_VALUE =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "false")
          .put(Type.TYPE_DOUBLE, "0.0")
          .put(Type.TYPE_FLOAT, "0.0F")
          .put(Type.TYPE_INT64, "0L")
          .put(Type.TYPE_UINT64, "0L")
          .put(Type.TYPE_SINT64, "0L")
          .put(Type.TYPE_FIXED64, "0L")
          .put(Type.TYPE_SFIXED64, "0L")
          .put(Type.TYPE_INT32, "0")
          .put(Type.TYPE_UINT32, "0")
          .put(Type.TYPE_SINT32, "0")
          .put(Type.TYPE_FIXED32, "0")
          .put(Type.TYPE_SFIXED32, "0")
          .put(Type.TYPE_STRING, "\"\"")
          .put(Type.TYPE_BYTES, "ByteString.EMPTY")
          .build();

  private JavaContextCommon javaCommon;

  public JavaGapicContext(Model model, ApiConfig apiConfig) {
    super(model, apiConfig);
  }

  @Override
  public void resetState(JavaContextCommon javaCommon) {
    this.javaCommon = javaCommon;
  }

  public JavaContextCommon java() {
    return javaCommon;
  }

  // Snippet Helpers
  // ===============

  /**
   * Takes a fully-qualified type name and returns its simple name, and also saves the type in the
   * import list.
   */
  public String getTypeName(String typeName) {
    int lastDotIndex = typeName.lastIndexOf('.');
    if (lastDotIndex < 0) {
      throw new IllegalArgumentException("expected fully qualified name");
    }
    String shortTypeName = typeName.substring(lastDotIndex + 1);
    return javaCommon.getMinimallyQualifiedName(typeName, shortTypeName);
  }

  /**
   * Adds the given type name to the import list. Returns an empty string so that the output is not
   * affected in snippets.
   */
  public String addImport(String typeName) {
    // used for its side effect of adding the type to the import list if the short name
    // hasn't been imported yet
    getTypeName(typeName);
    return "";
  }

  /**
   * Gets the java package for the given proto file.
   */
  public String getJavaPackage(ProtoFile file) {
    String packageName = file.getProto().getOptions().getJavaPackage();
    if (Strings.isNullOrEmpty(packageName)) {
      return DEFAULT_JAVA_PACKAGE_PREFIX + "." + file.getFullName();
    }
    return packageName;
  }

  /**
   * Gets the name of the class which is the grpc container for this service interface.
   */
  public String getGrpcName(Interface service) {
    String fullName =
        String.format("%s.%sGrpc", getJavaPackage(service.getFile()), service.getSimpleName());
    return getTypeName(fullName);
  }

  /**
   * Given a TypeRef, returns the return statement for that type. Specifically, this will return an
   * empty string for the empty type (we don't want a return statement for void).
   */
  public String methodReturnStatement(TypeRef type) {
    if (messages().isEmptyType(type)) {
      return "";
    } else {
      return "return ";
    }
  }

  /**
   * Given a TypeRef, returns the String form of the type to be used as a return value. Special
   * case: this will return "void" for the Empty return type.
   */
  public String methodReturnTypeName(TypeRef type) {
    if (messages().isEmptyType(type)) {
      return "void";
    } else {
      return typeName(type);
    }
  }

  /**
   * Returns the Java representation of a reference to a type.
   */
  public String typeName(TypeRef type) {
    if (type.isMap()) {
      String mapTypeName = getTypeName("java.util.Map");
      return String.format(
          "%s<%s, %s>",
          mapTypeName,
          basicTypeNameBoxed(type.getMapKeyField().getType()),
          basicTypeNameBoxed(type.getMapValueField().getType()));
    } else if (type.isRepeated()) {
      String listTypeName = getTypeName("java.util.List");
      return String.format("%s<%s>", listTypeName, basicTypeNameBoxed(type));
    } else {
      return basicTypeName(type);
    }
  }

  /**
   * Returns the Java representation of a zero value for that type, to be used in code sample doc.
   *
   * Parametric types may use the diamond operator, since the return value will be used only in
   * initialization.
   */
  public String zeroValue(TypeRef type) {
    // Don't call getTypeName; we don't need to import these.
    if (type.isMap()) {
      return "new HashMap<>()";
    }
    if (type.isRepeated()) {
      return "new ArrayList<>()";
    }
    if (PRIMITIVE_ZERO_VALUE.containsKey(type.getKind())) {
      return PRIMITIVE_ZERO_VALUE.get(type.getKind());
    }
    if (type.isMessage()) {
      return typeName(type) + ".newBuilder().build()";
    }
    return "null";
  }

  public String returnTypeOrEmpty(TypeRef returnType) {
    return messages().isEmptyType(returnType) ? "" : typeName(returnType);
  }

  /**
   * Returns the Java representation of a type, without cardinality, in boxed form.
   */
  public String basicTypeNameBoxed(TypeRef type) {
    return javaCommon.boxedTypeName(basicTypeName(type));
  }

  /**
   * Returns the Java representation of a type, without cardinality. If the type is a Java
   * primitive, basicTypeName returns it in unboxed form.
   */
  public String basicTypeName(TypeRef type) {
    String result = PRIMITIVE_TYPE_MAP.get(type.getKind());
    if (result != null) {
      if (result.contains(".")) {
        // Fully qualified type name, use regular type name resolver. Can skip boxing logic
        // because those types are already boxed.
        return getTypeName(result);
      }
      return result;
    }
    switch (type.getKind()) {
      case TYPE_MESSAGE:
        return getTypeName(type.getMessageType());
      case TYPE_ENUM:
        return getTypeName(type.getEnumType());
      default:
        throw new IllegalArgumentException("unknown type kind: " + type.getKind());
    }
  }

  /**
   * Gets the full name of the message or enum type in Java.
   */
  public String getTypeName(ProtoElement elem) {
    // Construct the full name in Java
    String name = getJavaPackage(elem.getFile());
    if (!elem.getFile().getProto().getOptions().getJavaMultipleFiles()) {
      String outerClassName = elem.getFile().getProto().getOptions().getJavaOuterClassname();
      if (outerClassName.isEmpty()) {
        outerClassName = getFileClassName(elem.getFile());
      }
      name = name + "." + outerClassName;
    }
    String shortName = elem.getFullName().substring(elem.getFile().getFullName().length() + 1);
    name = name + "." + shortName;

    return javaCommon.getMinimallyQualifiedName(name, shortName);
  }

  /**
   * Gets the class name for the given proto file.
   */
  private String getFileClassName(ProtoFile file) {
    String baseName = Files.getNameWithoutExtension(new File(file.getSimpleName()).getName());
    return lowerUnderscoreToUpperCamel(baseName);
  }

  public String defaultTokenValue(Field field) {
    if (field.getType().getKind().equals(Type.TYPE_STRING)) {
      return "\"\"";
    } else if (field.getType().getKind().equals(Type.TYPE_BYTES)) {
      String byteStringTypeName = getTypeName("com.google.protobuf.ByteString");
      return byteStringTypeName + ".EMPTY";
    } else {
      throw new IllegalArgumentException(
          String.format(
              "Unsupported type for field %s - found %s, "
                  + "but expected TYPE_STRING or TYPE_BYTES",
              field.getFullName(),
              field.getType().getKind()));
    }
  }

  // Workaround for the fact that quotes can't be used in a snippet @join
  public String partitionKeyCode(ImmutableList<FieldSelector> discriminatorFields) {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < discriminatorFields.size(); i++) {
      if (i > 0) {
        buf.append(" + \"|\" + ");
      }
      String simpleName = discriminatorFields.get(i).getLastField().getSimpleName();
      buf.append("request.get" + lowerUnderscoreToUpperCamel(simpleName) + "()");
    }
    return buf.toString();
  }

  public Method getFirstFlattenedMethod(Interface service) {
    for (Method method : service.getMethods()) {
      MethodConfig methodConfig =
          getApiConfig().getInterfaceConfig(service).getMethodConfig(method);
      if (methodConfig.isFlattening()) {
        return method;
      }
    }
    throw new RuntimeException("No flattened methods available.");
  }

  public String getTitle() {
    return getModel().getServiceConfig().getTitle();
  }

  public String getMultilineHeading(String heading) {
    final char[] array = new char[heading.length()];
    Arrays.fill(array, '=');
    String eqsString = new String(array);
    return String.format("%s\n%s\n%s", eqsString, heading, eqsString);
  }
}

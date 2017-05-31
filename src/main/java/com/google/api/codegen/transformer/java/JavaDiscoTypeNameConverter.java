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
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.LanguageUtil;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.ResourceNameConfig;
import com.google.api.codegen.config.ResourceNameType;
import com.google.api.codegen.discovery.DiscoveryNode;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.transformer.SchemaTypeNameConverter;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.api.codegen.util.TypedValue;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.tools.framework.model.EnumValue;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import java.io.File;

/** The ModelTypeTable for Java.
 *
 * Analogous to JavaModelTypeNameConverter for Discovery Doc schemas. */
//TODO(andrealin) remove the implements clause.
public class JavaDiscoTypeNameConverter implements SchemaTypeNameConverter {
  private TypeNameConverter typeNameConverter;


  /** The package prefix to use if no java package option was provided. */
  private static final String DEFAULT_JAVA_PACKAGE_PREFIX = "com.google.cloud.messages";

  public JavaDiscoTypeNameConverter(String implicitPackageName) {
    this.typeNameConverter = new JavaTypeTable(implicitPackageName);
  }

  private static String javaPrimitiveType(Schema.Type type, Schema.Format format) {
    switch (type) {
      case INTEGER:
        switch (format) {
          case INT32:
            return "int";
          case UINT32:
          default:
            return "long";
        }
      case NUMBER:
        switch (format) {
          case FLOAT:
            return "float";
          case DOUBLE:
          default:
            return "double";
        }
      case BOOLEAN:
        return "boolean";
      case STRING:
        return "java.lang.String";
      case OBJECT:
        return "java.lang.Object";
      default:
        return null;
    }
  }

  private static String javaPrimitiveZeroValue(Schema.Type type) {
    switch (type) {
      case INTEGER:
      case NUMBER:
        return "0";
      case BOOLEAN:
        return "false";
      case STRING:
        return "\"\"";
      default:
        return null;
    }
  }

  @Override
  public TypeName getTypeNameInImplicitPackage(String shortName) {
    return typeNameConverter.getTypeNameInImplicitPackage(shortName);
  }

  @Override
  public TypedValue getSnippetZeroValue(Schema schema) {
    return null;
  }

  @Override
  public TypedValue getImplZeroValue(Schema schema) {
    return null;
  }

  @Override
  public String renderPrimitiveValue(Schema schema, String value) {
    return null;
  }

  public TypeName getTypeName(Schema.Type type) {
    if (type == Schema.Type.ARRAY) {
      TypeName listTypeName = typeNameConverter.getTypeName("java.util.List");
      TypeName elementTypeName = getTypeNameForElementType(type, true);
      return new TypeName(
          listTypeName.getFullName(), listTypeName.getNickname(), "%s<%i>", elementTypeName);
    } else {
      return getTypeNameForElementType(type, false);
    }
  }

  @Override
  public TypedValue getEnumValue(TypeRef type, EnumValue value) {
    return TypedValue.create(getTypeName(type), "%s." + value.getSimpleName());
  }

  @Override
  public TypeName getTypeNameForElementType(TypeRef type) {
    return getTypeNameForElementType(type, true);
  }

  /**
   * Returns the Java representation of a type, without cardinality. If the type is a Java
   * primitive, basicTypeName returns it in unboxed form.
   */
  private TypeName getTypeNameForElementType(
      Schema.Type type, Schema.Format format, boolean shouldBoxPrimitives) {
    String primitiveTypeName = javaPrimitiveType(type, format);
    if (primitiveTypeName != null) {
      if (primitiveTypeName.contains(".")) {
        // Fully qualified type name, use regular type name resolver. Can skip boxing logic
        // because those types are already boxed.
        return typeNameConverter.getTypeName(primitiveTypeName);
      } else {
        if (shouldBoxPrimitives) {
          return new TypeName(JavaTypeTable.getBoxedTypeName(primitiveTypeName));
        } else {
          return new TypeName(primitiveTypeName);
        }
      }
    }
    switch (type) {

      case TYPE_MESSAGE:
        return getTypeName(type.getMessageType());
      case TYPE_ENUM:
        return getTypeName(type.getEnumType());
      case ANY:
        return "java.lang.Object";
      case ARRAY:
        break;
      case BOOLEAN:
        break;
      case EMPTY:
        break;
      case INTEGER:
        break;
      case NUMBER:
        break;
      case OBJECT:
        break;
      case STRING:
        break;
      default:
        throw new IllegalArgumentException("unknown type kind: " + type.getKind());
    }
  }

  @Override
  public String renderPrimitiveValue(TypeRef type, String value) {
    Type primitiveType = type.getKind();
    if (!PRIMITIVE_TYPE_MAP.containsKey(primitiveType)) {
      throw new IllegalArgumentException(
          "Initial values are only supported for primitive types, got type "
              + type
              + ", with value "
              + value);
    }
    switch (primitiveType) {
      case TYPE_BOOL:
        return value.toLowerCase();
      case TYPE_FLOAT:
        return value + "F";
      case TYPE_INT64:
      case TYPE_UINT64:
        return value + "L";
      case TYPE_STRING:
        return "\"" + value + "\"";
      case TYPE_BYTES:
        return "ByteString.copyFromUtf8(\"" + value + "\")";
      default:
        // Types that do not need to be modified (e.g. TYPE_INT32) are handled here
        return value;
    }
  }

  /**
   * Returns the Java representation of a zero value for that type, to be used in code sample doc.
   *
   * <p>Parametric types may use the diamond operator, since the return value will be used only in
   * initialization.
   */
  @Override
  public TypedValue getSnippetZeroValue(TypeRef type) {
    // Don't call importAndGetShortestName; we don't need to import these.
    if (type.isMap()) {
      return TypedValue.create(typeNameConverter.getTypeName("java.util.HashMap"), "new %s<>()");
    }
    if (type.isRepeated()) {
      return TypedValue.create(typeNameConverter.getTypeName("java.util.ArrayList"), "new %s<>()");
    }
    if (PRIMITIVE_ZERO_VALUE.containsKey(type.getKind())) {
      return TypedValue.create(getTypeName(type), PRIMITIVE_ZERO_VALUE.get(type.getKind()));
    }
    if (type.isMessage()) {
      return TypedValue.create(getTypeName(type), "%s.newBuilder().build()");
    }
    if (type.isEnum()) {
      return getEnumValue(type, type.getEnumType().getValues().get(0));
    }
    return TypedValue.create(getTypeName(type), "null");
  }

  @Override
  public TypedValue getImplZeroValue(TypeRef type) {
    return getSnippetZeroValue(type);
  }


  public TypeName getTypeName(Schema schema) {
    String packageName = schema.location();
    String shortName = getShortName(elem);
    String longName = packageName + "." + shortName;

    return new TypeName(longName, shortName);
  }

  @Override
  public TypedValue getEnumValue(Schema type) {
    // TODO(andrealin) implement this.
    return null;
  }

  @Override
  public TypeName getTypeNameForElementType(Schema type) {
    return null;
  }

  private TypeName getTypeNameForTypedResourceName(
      ResourceNameConfig resourceNameConfig, TypeRef type, String typedResourceShortName) {
    String packageName = getResourceNamePackage(resourceNameConfig);
    String longName = packageName + "." + typedResourceShortName;

    TypeName simpleTypeName = new TypeName(longName, typedResourceShortName);

    if (type.isMap()) {
      throw new IllegalArgumentException("Map type not supported for typed resource name");
    } else if (type.isRepeated()) {
      TypeName listTypeName = typeNameConverter.getTypeName("java.util.List");
      return new TypeName(
          listTypeName.getFullName(), listTypeName.getNickname(), "%s<%i>", simpleTypeName);
    } else {
      return simpleTypeName;
    }
  }

  private static String getResourceNamePackage(ResourceNameConfig resourceNameConfig) {
    ResourceNameType resourceNameType = resourceNameConfig.getResourceNameType();
    switch (resourceNameType) {
      case ANY:
        return "com.google.api.resourcenames";
      case FIXED:
      case SINGLE:
      case ONEOF:
        return getJavaPackage(resourceNameConfig.getAssignedProtoFile());
      case NONE:
      default:
        throw new IllegalArgumentException("Unexpected ResourceNameType: " + resourceNameType);
    }
  }

  @Override
  public TypeName getTypeNameForTypedResourceName(
      FieldConfig fieldConfig, String typedResourceShortName) {
    return getTypeNameForTypedResourceName(
        fieldConfig.getResourceNameConfig(),
        fieldConfig.getField().getType(),
        typedResourceShortName);
  }

  @Override
  public TypeName getTypeNameForResourceNameElementType(
      FieldConfig fieldConfig, String typedResourceShortName) {
    return getTypeNameForTypedResourceName(
        fieldConfig.getResourceNameConfig(),
        fieldConfig.getField().getType().makeOptional(),
        typedResourceShortName);
  }

  private static String getShortName(Schema elem) {
    // TODO(andrealin): escape java keywords
    return elem.id();
  }

  private static String getProtoElementPackage(DiscoveryNode node, Schema elem) {
    node.
    String name = getJavaPackage(elem.getFile());
    if (!elem.getFile().getProto().getOptions().getJavaMultipleFiles()) {
      String outerClassName = elem.getFile().getProto().getOptions().getJavaOuterClassname();
      if (outerClassName.isEmpty()) {
        outerClassName = getFileClassName(elem.getFile());
      }
      name = name + "." + outerClassName;
    }
    return name;
  }

  public static String getJavaPackage(ProtoFile file) {
    String packageName = file.getProto().getOptions().getJavaPackage();
    if (Strings.isNullOrEmpty(packageName)) {
      return DEFAULT_JAVA_PACKAGE_PREFIX + "." + file.getFullName();
    }
    return packageName;
  }

  /** Gets the class name for the given proto file. */
  private static String getFileClassName(ProtoFile file) {
    String baseName = Files.getNameWithoutExtension(new File(file.getSimpleName()).getName());
    return LanguageUtil.lowerUnderscoreToUpperCamel(baseName);
  }
}

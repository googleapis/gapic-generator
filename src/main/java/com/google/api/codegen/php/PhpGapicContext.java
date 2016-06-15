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
package com.google.api.codegen.php;

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.GapicContext;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

/**
 * A GapicContext specialized for PHP.
 */
public class PhpGapicContext extends GapicContext implements PhpContext {
  /**
   * A map from primitive types in proto to PHP counterparts.
   */
  private static final ImmutableMap<Type, String> PRIMITIVE_TYPE_MAP =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "bool")
          .put(Type.TYPE_DOUBLE, "float")
          .put(Type.TYPE_FLOAT, "float")
          .put(Type.TYPE_INT64, "int")
          .put(Type.TYPE_UINT64, "int")
          .put(Type.TYPE_SINT64, "int")
          .put(Type.TYPE_FIXED64, "int")
          .put(Type.TYPE_SFIXED64, "int")
          .put(Type.TYPE_INT32, "int")
          .put(Type.TYPE_UINT32, "int")
          .put(Type.TYPE_SINT32, "int")
          .put(Type.TYPE_FIXED32, "int")
          .put(Type.TYPE_SFIXED32, "int")
          .put(Type.TYPE_STRING, "string")
          .put(Type.TYPE_BYTES, "string")
          .build();

  /**
   * A map from primitive types in proto to zero value in PHP
   */
  private static final ImmutableMap<Type, String> PRIMITIVE_ZERO_VALUE =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "false")
          .put(Type.TYPE_DOUBLE, "0.0")
          .put(Type.TYPE_FLOAT, "0.0")
          .put(Type.TYPE_INT64, "0")
          .put(Type.TYPE_UINT64, "0")
          .put(Type.TYPE_SINT64, "0")
          .put(Type.TYPE_FIXED64, "0")
          .put(Type.TYPE_SFIXED64, "0")
          .put(Type.TYPE_INT32, "0")
          .put(Type.TYPE_UINT32, "0")
          .put(Type.TYPE_SINT32, "0")
          .put(Type.TYPE_FIXED32, "0")
          .put(Type.TYPE_SFIXED32, "0")
          .put(Type.TYPE_STRING, "\"\"")
          .put(Type.TYPE_BYTES, "\"\"")
          .build();

  private PhpContextCommon phpCommon;

  public PhpGapicContext(Model model, ApiConfig apiConfig) {
    super(model, apiConfig);
  }

  @Override
  public void resetState(PhpContextCommon phpCommon) {
    this.phpCommon = phpCommon;
  }

  public PhpContextCommon php() {
    return phpCommon;
  }

  // Snippet Helpers
  // ===============

  /**
   * Returns the PHP filename which holds the gRPC service definition.
   */
  public String getGrpcFilename(Interface service) {
    return service.getFile().getProto().getName().replace(".proto", "_services");
  }

  /**
   * Returns the name of the gRPC client class.
   */
  public String getGrpcClientName(Interface service) {
    String fullyQualifiedClientName = service.getFullName().replaceAll("\\.", "\\\\") + "Client";
    return getTypeName(fullyQualifiedClientName);
  }

  public String getTypeName(String typeName) {
    int lastBackslashIndex = typeName.lastIndexOf('\\');
    if (lastBackslashIndex < 0) {
      throw new IllegalArgumentException("expected fully qualified name");
    }
    String shortTypeName = typeName.substring(lastBackslashIndex + 1);
    return phpCommon.getMinimallyQualifiedName(typeName, shortTypeName);
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
   * Returns the PHP representation of a reference to a type.
   */
  public String typeName(TypeRef type) {
    if (type.isMap()) {
      return "array";
    } else if (type.isRepeated()) {
      return basicTypeName(type) + "[]";
    } else {
      return basicTypeName(type);
    }
  }

  /**
   * Returns the PHP representation of a type, without cardinality. If the type is a primitive,
   * basicTypeName returns it in unboxed form.
   */
  public String basicTypeName(TypeRef type) {
    String result = PRIMITIVE_TYPE_MAP.get(type.getKind());
    if (result != null) {
      if (result.contains("\\")) {
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
   * Returns the PHP representation of a zero value for that type, to be used in code sample doc.
   *
   * Parametric types may use the diamond operator, since the return value will be used only in
   * initialization.
   */
  public String zeroValue(TypeRef type) {
    // Don't call getTypeName; we don't need to import these.
    if (type.isMap()) {
      return "[]";
    }
    if (type.isRepeated()) {
      return "[]";
    }
    if (PRIMITIVE_ZERO_VALUE.containsKey(type.getKind())) {
      return PRIMITIVE_ZERO_VALUE.get(type.getKind());
    }
    if (type.isMessage()) {
      return "new " + typeName(type) + "()";
    }
    return "null";
  }

  public String fullyQualifiedName(TypeRef type) {
    return type.getMessageType().getFullName().replaceAll("\\.", "\\\\");
  }

  /**
   * Gets the full name of the message or enum type in PHP.
   */
  public String getTypeName(ProtoElement elem) {
    // Construct the fully-qualified PHP class name
    int qualifiedPrefixLength = elem.getFile().getFullName().length() + 1;
    String shortName = elem.getFullName().substring(qualifiedPrefixLength);
    String name = getPhpPackage(elem.getFile()) + "\\" + shortName;

    return phpCommon.getMinimallyQualifiedName(name, shortName);
  }

  public String getServiceName(Interface service) {
    return service.getFullName();
  }

  /**
   * Gets the PHP package for the given proto file.
   */
  public String getPhpPackage(ProtoFile file) {
    return file.getProto().getPackage().replaceAll("\\.", "\\\\");
  }

  public Method getFirstMethod(Interface service) {
    ImmutableList<Method> methods = service.getMethods();
    if (methods.size() > 0) {
      return methods.get(0);
    }
    throw new RuntimeException("No methods available.");
  }

  public String returnTypeOrEmpty(TypeRef returnType) {
    return messages().isEmptyType(returnType) ? "" : typeName(returnType);
  }

  public PhpDocConfig.Builder newPhpDocConfigBuilder() {
    return PhpDocConfig.newBuilder();
  }

  // Constants
  // =========

  /**
   * A set of PHP keywords and built-ins. keywords: http://php.net/manual/en/reserved.keywords.php
   */
  private static final ImmutableSet<String> KEYWORD_BUILT_IN_SET =
      ImmutableSet.<String>builder()
          .add(
              "__halt_compiler",
              "abstract",
              "and",
              "array",
              "as",
              "break",
              "callable",
              "case",
              "catch",
              "class",
              "clone",
              "const",
              "continue",
              "declare",
              "default",
              "die",
              "do",
              "echo",
              "else",
              "elseif",
              "empty",
              "enddeclare",
              "endfor",
              "endforeach",
              "endif",
              "endswitch",
              "endwhile",
              "eval",
              "exit",
              "extends",
              "final",
              "finally",
              "for",
              "foreach",
              "function",
              "global",
              "goto",
              "if",
              "implements",
              "include",
              "include_once",
              "instanceof",
              "insteadof",
              "interface",
              "isset",
              "list",
              "namespace",
              "new",
              "or",
              "print",
              "private",
              "protected",
              "public",
              "require",
              "require_once",
              "return",
              "static",
              "switch",
              "throw",
              "trait",
              "try",
              "unset",
              "use",
              "var",
              "while",
              "xor",
              "yield",
              "__CLASS__",
              "__DIR__",
              "__FILE__",
              "__FUNCTION__",
              "__LINE__",
              "__METHOD__",
              "__NAMESPACE__",
              "__TRAIT__")
          .build();
}

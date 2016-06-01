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
package com.google.api.codegen.nodejs;

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.GapicContext;
import com.google.api.codegen.MethodConfig;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.aspects.documentation.model.ElementDocumentationAttribute;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.api.tools.framework.model.TypeRef.Cardinality;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.util.List;

import javax.annotation.Nullable;

/**
 * A GapicContext specialized for Node.JS.
 */
public class NodeJSGapicContext extends GapicContext implements NodeJSContext {

  public NodeJSGapicContext(Model model, ApiConfig apiConfig) {
    super(model, apiConfig);
  }

  @Override
  public String getOutputSubPath(ProtoElement element) {
    return "lib";
  }

  // Snippet Helpers
  // ===============

  public String filePath(ProtoFile file) {
    return file.getSimpleName().replace(".proto", "_pb2.js");
  }

  /**
   * Return comments lines for a given proto element, extracted directly from the proto doc
   */
  public List<String> defaultComments(ProtoElement element) {
    if (!element.hasAttribute(ElementDocumentationAttribute.KEY)) {
      return ImmutableList.<String>of();
    }
    return convertToCommentedBlock(
        JSDocCommentFixer.jsdocify(DocumentationUtil.getScopedDescription(element)));
  }

  /**
   * Returns type information for a field in JSDoc style.
   */
  private String fieldTypeCardinalityComment(Field field) {
    TypeRef type = field.getType();

    String cardinalityComment = "";
    if (type.getCardinality() == Cardinality.REPEATED) {
      if (type.isMap()) {
        return "Object";
      } else {
        cardinalityComment = "[]";
      }
    }
    String typeComment = jsTypeName(field.getType());
    return String.format("%s%s", typeComment, cardinalityComment);
  }

  /**
   * Returns a JSDoc comment string for the field as a parameter to a function.
   */
  private String fieldParamComment(Field field, String paramComment, boolean isOptional) {
    String commentType = fieldTypeCardinalityComment(field);
    String fieldName = wrapIfKeywordOrBuiltIn(lowerUnderscoreToLowerCamel(field.getSimpleName()));
    if (isOptional) {
      fieldName = "otherArgs." + fieldName;
    }
    return fieldComment(
        String.format("@param {%s} %s", commentType, fieldName), paramComment, field);
  }

  /**
   * Returns a JSDoc comment string for the field as an attribute of a message.
   */
  private String fieldPropertyComment(Field field) {
    String commentType = fieldTypeCardinalityComment(field);
    String fieldName = wrapIfKeywordOrBuiltIn(field.getSimpleName());
    return fieldComment(String.format("@property {%s} %s", commentType, fieldName), null, field);
  }

  private String fieldComment(String comment, String paramComment, Field field) {
    if (paramComment == null) {
      paramComment = DocumentationUtil.getScopedDescription(field);
    }
    if (!Strings.isNullOrEmpty(paramComment)) {
      paramComment = JSDocCommentFixer.jsdocify(paramComment);
      comment += "\n  " + paramComment.replaceAll("(\\r?\\n)", "\n  ");
    }
    return comment + "\n";
  }

  /**
   * Return JSDoc callback comment and return type comment for the given method.
   */
  @Nullable
  private String returnTypeComment(Method method, MethodConfig config) {
    MessageType returnMessageType = method.getOutputMessage();
    boolean isEmpty = returnMessageType.getFullName().equals("google.protobuf.Empty");

    String classInfo = jsTypeName(method.getOutputType());

    String callbackType = isEmpty ? "EmptyCallback" : String.format("APICallback<%s>", classInfo);
    String callbackComment =
        "@param {?"
            + callbackType
            + "} callback\n"
            + "  The function which will be called with the result of the API call.";
    if (config.isPageStreaming()) {
      String resourceType = jsTypeName(config.getPageStreaming().getResourcesField().getType());
      return callbackComment
          + "\n@returns {?Stream<"
          + resourceType
          + ">}\n"
          + "  An object stream of "
          + resourceType
          + " instances, unless\n"
          + "  page streaming is disabled through the call options or a callback\n"
          + "  is specified. If page streaming is disabled or a callback is specified,\n"
          + "  this returns null, and the callback will be called with a single instance\n"
          + "  of "
          + classInfo
          + ".";
    } else {
      return callbackComment;
    }
  }

  /**
   * Return comments lines for a given method, consisting of proto doc and parameter type
   * documentation.
   */
  public List<String> methodComments(Method msg) {
    MethodConfig config =
        getApiConfig().getInterfaceConfig((Interface) msg.getParent()).getMethodConfig(msg);

    // Generate parameter types
    StringBuilder paramTypesBuilder = new StringBuilder();
    for (Field field : config.getRequiredFields()) {
      paramTypesBuilder.append(fieldParamComment(field, null, false));
    }
    Iterable<Field> optionalParams = config.getOptionalFields();
    if (optionalParams.iterator().hasNext()) {
      paramTypesBuilder.append("@param {?Object} otherArgs\n");
      for (Field field : optionalParams) {
        if (config.isPageStreaming()
            && field.equals((config.getPageStreaming().getPageSizeField()))) {
          paramTypesBuilder.append(
              fieldParamComment(
                  field,
                  "The maximum number of resources contained in the underlying API\n"
                      + "response. If page streaming is performed per-resource, this\n"
                      + "parameter does not affect the return value. If page streaming is\n"
                      + "performed per-page, this determines the maximum number of\n"
                      + "resources in a page.",
                  true));
        } else {
          paramTypesBuilder.append(fieldParamComment(field, null, true));
        }
      }
    }
    paramTypesBuilder.append(
        "@param {?gax.CallOptions} options\n"
            + "  Overrides the default settings for this call, e.g, timeout,\n"
            + "  retries, etc.");
    String paramTypes = paramTypesBuilder.toString();

    String returnType = returnTypeComment(msg, config);

    // Generate comment contents
    StringBuilder contentBuilder = new StringBuilder();
    if (msg.hasAttribute(ElementDocumentationAttribute.KEY)) {
      contentBuilder.append(
          JSDocCommentFixer.jsdocify(DocumentationUtil.getScopedDescription(msg)));
      if (!Strings.isNullOrEmpty(paramTypes)) {
        contentBuilder.append("\n\n");
      }
    }
    contentBuilder.append(paramTypes);
    if (returnType != null) {
      contentBuilder.append("\n" + returnType);
    }

    contentBuilder.append("\n@throws an error if the RPC is aborted.");
    return convertToCommentedBlock(contentBuilder.toString());
  }

  /**
   * Return the doccomment for the message.
   */
  public List<String> methodDocComment(MessageType msg) {
    StringBuilder attributesBuilder = new StringBuilder();
    attributesBuilder.append("@typedef {Object} " + msg.getFullName() + "\n");
    for (Field field : msg.getFields()) {
      attributesBuilder.append(fieldPropertyComment(field));
    }

    String attributes = attributesBuilder.toString().trim();

    List<String> content = defaultComments(msg);
    return ImmutableList.<String>builder()
        .addAll(content)
        .addAll(convertToCommentedBlock(attributes))
        .build();
  }

  /**
   * Return a non-conflicting safe name if name is a JS reserved word.
   */
  public String wrapIfKeywordOrBuiltIn(String name) {
    if (KEYWORD_BUILT_IN_SET.contains(name)) {
      return name + "_";
    }
    return name;
  }

  /**
   * Return the default value for the given field.
   */
  public String defaultValue(Field field) {
    TypeRef type = field.getType();
    if (type.isMap()) {
      return "{}";
    }
    // Return empty array if the type is repeated.
    if (type.getCardinality() == Cardinality.REPEATED) {
      return "[]";
    }
    switch (type.getKind()) {
      case TYPE_MESSAGE:
        return "{}";
      case TYPE_ENUM:
        Preconditions.checkArgument(
            type.getEnumType().getValues().size() > 0, "enum must have a value");
        return type.getEnumType().getValues().get(0).getFullName();
      default:
        if (type.isPrimitive()) {
          return DEFAULT_VALUE_MAP.get(type.getKind());
        }
        throw new IllegalArgumentException("unknown type kind: " + type.getKind());
    }
  }

  /**
   * Returns the name of JS type for the given typeRef.
   */
  public String jsTypeName(TypeRef typeRef) {
    switch (typeRef.getKind()) {
      case TYPE_MESSAGE:
        return typeRef.getMessageType().getFullName();
      case TYPE_ENUM:
        return typeRef.getEnumType().getFullName();
      default:
        {
          String name = PRIMITIVE_TYPE_NAMES.get(typeRef.getKind());
          if (!Strings.isNullOrEmpty(name)) {
            return name;
          }
          throw new IllegalArgumentException("unknown type kind: " + typeRef.getKind());
        }
    }
  }

  /**
   * Returns the name of the JS type name for arguejs parameter definitions.
   */
  public String getFieldType(Field field) {
    TypeRef typeRef = field.getType();
    if (typeRef.isMap()) {
      return "Object";
    }
    if (typeRef.getCardinality() == Cardinality.REPEATED) {
      return "Array";
    }
    switch (typeRef.getKind()) {
      case TYPE_MESSAGE:
        return "Object";
      case TYPE_BOOL:
        return "Boolean";
      case TYPE_STRING:
      case TYPE_BYTES:
        return "String";
      default:
        // Numeric types and enums.
        return "Number";
    }
  }

  /**
   * Convert the content string into a commented block that can be directly printed out in the
   * generated JS files.
   */
  private List<String> convertToCommentedBlock(String content) {
    if (Strings.isNullOrEmpty(content)) {
      return ImmutableList.<String>of();
    }
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (String comment : Splitter.on("\n").splitToList(content)) {
      builder.add(comment);
    }
    return builder.build();
  }

  // Constants
  // =========

  /**
   * A map from primitive types to its default value.
   */
  private static final ImmutableMap<Type, String> DEFAULT_VALUE_MAP =
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
          .put(Type.TYPE_STRING, "\'\'")
          .put(Type.TYPE_BYTES, "\'\'")
          .build();

  private static final ImmutableMap<Type, String> PRIMITIVE_TYPE_NAMES =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "boolean")
          .put(Type.TYPE_DOUBLE, "number")
          .put(Type.TYPE_FLOAT, "number")
          .put(Type.TYPE_INT64, "number")
          .put(Type.TYPE_UINT64, "number")
          .put(Type.TYPE_SINT64, "number")
          .put(Type.TYPE_FIXED64, "number")
          .put(Type.TYPE_SFIXED64, "number")
          .put(Type.TYPE_INT32, "number")
          .put(Type.TYPE_UINT32, "number")
          .put(Type.TYPE_SINT32, "number")
          .put(Type.TYPE_FIXED32, "number")
          .put(Type.TYPE_SFIXED32, "number")
          .put(Type.TYPE_STRING, "String")
          .put(Type.TYPE_BYTES, "String")
          .build();

  /**
   * A set of ECMAScript 2016 reserved words. See
   * https://tc39.github.io/ecma262/2016/#sec-reserved-words
   */
  private static final ImmutableSet<String> KEYWORD_BUILT_IN_SET =
      ImmutableSet.<String>builder()
          .add(
              "break",
              "do",
              "in",
              "typeof",
              "case",
              "else",
              "instanceof",
              "var",
              "catch",
              "export",
              "new",
              "void",
              "class",
              "extends",
              "return",
              "while",
              "const",
              "finally",
              "super",
              "with",
              "continue",
              "for",
              "switch",
              "yield",
              "debugger",
              "function",
              "this",
              "default",
              "if",
              "throw",
              "delete",
              "import",
              "try",
              "let",
              "static",
              "enum",
              "await",
              "implements",
              "package",
              "protected",
              "interface",
              "private",
              "public",
              "null",
              "true",
              "false",
              // common parameters passed to methods.
              "otherArgs",
              "options",
              "callback")
          .build();
}

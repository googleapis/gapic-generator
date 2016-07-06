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
package com.google.api.codegen.py;

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
import com.google.common.collect.Lists;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/** A GapicContext specialized for Python. */
public class PythonGapicContext extends GapicContext {

  /** A map from primitive types to its default value. */
  private static final ImmutableMap<Type, String> DEFAULT_VALUE_MAP =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "False")
          .put(Type.TYPE_DOUBLE, "0.0")
          .put(Type.TYPE_FLOAT, "0.0")
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
          .put(Type.TYPE_STRING, "\'\'")
          .put(Type.TYPE_BYTES, "\'\'")
          .build();

  /** A map from primitive types to their names in Python. */
  private static final Map<Type, String> PRIMITIVE_TYPE_NAMES =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_DOUBLE, "float")
          .put(Type.TYPE_FLOAT, "float")
          .put(Type.TYPE_INT32, "int")
          .put(Type.TYPE_INT64, "long")
          .put(Type.TYPE_UINT32, "int")
          .put(Type.TYPE_UINT64, "long")
          .put(Type.TYPE_SINT32, "int")
          .put(Type.TYPE_SINT64, "long")
          .put(Type.TYPE_FIXED32, "int")
          .put(Type.TYPE_FIXED64, "long")
          .put(Type.TYPE_SFIXED32, "int")
          .put(Type.TYPE_SFIXED64, "long")
          .put(Type.TYPE_BOOL, "bool")
          .put(Type.TYPE_STRING, "string")
          .put(Type.TYPE_BYTES, "bytes")
          .build();

  private PythonContextCommon pythonCommon;

  public PythonGapicContext(Model model, ApiConfig apiConfig) {
    super(model, apiConfig);
    this.pythonCommon = new PythonContextCommon();
  }

  public PythonContextCommon python() {
    return pythonCommon;
  }

  // Snippet Helpers
  // ===============

  public String filePath(ProtoFile file) {
    return file.getSimpleName().replace(".proto", "_pb2.py");
  }

  /** Return comments lines for a given proto element, extracted directly from the proto doc */
  public List<String> defaultComments(ProtoElement element) {
    if (!element.hasAttribute(ElementDocumentationAttribute.KEY)) {
      return ImmutableList.<String>of("");
    }
    return pythonCommon.convertToCommentedBlock(getSphinxifiedScopedDescription(element));
  }

  /** Returns type information for a field in Sphinx docstring style. */
  private String fieldTypeCardinalityComment(Field field, PythonImportHandler importHandler) {
    return typeCardinalityComment(field.getType(), importHandler);
  }

  private String typeCardinalityComment(TypeRef type, PythonImportHandler importHandler) {
    boolean closingBrace = false;
    String prefix;
    if (type.getCardinality() == Cardinality.REPEATED) {
      if (type.isMap()) {
        prefix =
            String.format("dict[%s -> ", fieldTypeComment(type.getMapKeyField(), importHandler));
      } else {
        prefix = "list[";
      }
      closingBrace = true;
    } else {
      prefix = "";
    }
    String typeComment = typeComment(type, importHandler);
    return String.format("%s%s%s", prefix, typeComment, closingBrace ? "]" : "");
  }

  /** Returns type information for a field in Sphinx docstring style. */
  private String fieldTypeComment(Field field, PythonImportHandler importHandler) {
    return typeComment(field.getType(), importHandler);
  }

  private String typeComment(TypeRef type, PythonImportHandler importHandler) {
    switch (type.getKind()) {
      case TYPE_MESSAGE:
        return ":class:`" + importHandler.elementPath(type.getMessageType(), true) + "`";
      case TYPE_ENUM:
        return ":class:`" + importHandler.elementPath(type.getEnumType(), true) + "`";
      default:
        if (type.isPrimitive()) {
          return PRIMITIVE_TYPE_NAMES.get(type.getKind());
        } else {
          throw new IllegalArgumentException("unknown type kind: " + type.getKind());
        }
    }
  }

  /** Returns a comment string for field, consisting of type information and proto comment. */
  private String fieldComment(Field field, PythonImportHandler importHandler, String paramComment) {
    String comment =
        String.format(
            "  %s (%s)", field.getSimpleName(), fieldTypeCardinalityComment(field, importHandler));
    if (paramComment == null) {
      paramComment = getSphinxifiedScopedDescription(field);
    }
    if (!Strings.isNullOrEmpty(paramComment)) {
      if (paramComment.charAt(paramComment.length() - 1) == '\n') {
        paramComment = paramComment.substring(0, paramComment.length() - 1);
      }
      comment += ": " + paramComment.replaceAll("(\\r?\\n)", "\n    ");
    }
    return comment + "\n";
  }

  /**
   * Return comments lines for a given message, consisting of proto doc and argument type
   * documentation.
   */
  public List<String> messageComments(MessageType msg, PythonImportHandler importHandler) {
    // Generate parameter types
    StringBuilder paramTypesBuilder = new StringBuilder();
    paramTypesBuilder.append("Attributes:\n");
    for (Field field : msg.getFields()) {
      paramTypesBuilder.append(fieldComment(field, importHandler, null));
    }
    String paramTypes = paramTypesBuilder.toString();
    // Generate comment contents
    StringBuilder contentBuilder = new StringBuilder();
    if (msg.hasAttribute(ElementDocumentationAttribute.KEY)) {
      contentBuilder.append(getSphinxifiedScopedDescription(msg));
      if (!Strings.isNullOrEmpty(paramTypes)) {
        contentBuilder.append("\n\n");
      }
    }
    contentBuilder.append(paramTypes);
    return pythonCommon.convertToCommentedBlock(contentBuilder.toString());
  }

  /**
   * Return Sphinx-style return type string for the given method, or null if the return type is
   * None.
   */
  @Nullable
  private String returnTypeComment(
      Method method, MethodConfig config, PythonImportHandler importHandler) {
    MessageType returnMessageType = method.getOutputMessage();
    if (PythonProtoElements.isEmptyMessage(returnMessageType)) {
      return null;
    }

    String path = importHandler.elementPath(returnMessageType, true);
    String classInfo = ":class:`" + path + "` instance";

    if (config.isPageStreaming()) {
      return "Returns:"
          + "\n  A :class:`google.gax.PageIterator` instance. By default, this"
          + "\n  is an iterable of "
          + fieldTypeComment(config.getPageStreaming().getResourcesField(), importHandler)
          + " instances."
          + "\n  This object can also be configured to iterate over the pages"
          + "\n  of the response through the `CallOptions` parameter.";

    } else {
      return "Returns:\n  A " + classInfo + ".";
    }
  }

  public PythonDocConfig.Builder newDocConfigBuilder() {
    return PythonDocConfig.newBuilder();
  }

  /** Generate comments lines for a given method's description. */
  public List<String> methodDescriptionComments(Method method) {
    String description = "";
    if (method.hasAttribute(ElementDocumentationAttribute.KEY)) {
      String sphinxified = getSphinxifiedScopedDescription(method);
      sphinxified = sphinxified.trim();
      description = sphinxified.replaceAll("\\s*\\n\\s*", "\n");
    }
    return Splitter.on("\n").splitToList(description);
  }

  /**
   * Generate comments lines for a given method's signature, consisting of proto doc and parameter
   * type documentation.
   */
  public List<String> methodSignatureComments(Method method, PythonImportHandler importHandler) {
    MethodConfig config =
        getApiConfig().getInterfaceConfig((Interface) method.getParent()).getMethodConfig(method);

    StringBuilder contentBuilder = new StringBuilder();

    // parameter types
    contentBuilder.append("Args:\n");
    for (Field field : this.messages().flattenedFields(method.getInputType())) {
      if (config.isPageStreaming()
          && field.equals((config.getPageStreaming().getPageSizeField()))) {
        contentBuilder.append(
            fieldComment(
                field,
                importHandler,
                "The maximum number of resources contained in the\n"
                    + "underlying API response. If page streaming is performed per-\n"
                    + "resource, this parameter does not affect the return value. If page\n"
                    + "streaming is performed per-page, this determines the maximum number\n"
                    + "of resources in a page."));
      } else {
        contentBuilder.append(fieldComment(field, importHandler, null));
      }
    }
    contentBuilder.append(
        "  options (:class:`google.gax.CallOptions`): "
            + "Overrides the default\n    settings for this call, e.g, timeout, retries etc.");

    // return type
    String returnType = returnTypeComment(method, config, importHandler);
    if (returnType != null) {
      contentBuilder.append("\n\n" + returnType);
    }

    // exception types
    contentBuilder.append(
        "\n\nRaises:\n  :exc:`google.gax.errors.GaxError` if the RPC is aborted.");

    return Splitter.on("\n").splitToList(contentBuilder.toString());
  }

  /** Get required (non-optional) fields. */
  public List<Field> getRequiredFields(Method method) {
    Interface service = (Interface) method.getParent();
    MethodConfig methodConfig = getApiConfig().getInterfaceConfig(service).getMethodConfig(method);
    return Lists.newArrayList(methodConfig.getRequiredFields());
  }

  /** Get return type string, or an empty string if there is no return type. */
  public String returnTypeOrEmpty(Method method, PythonImportHandler importHandler) {
    TypeRef returnType = method.getOutputType();
    return messages().isEmptyType(returnType)
        ? ""
        : typeCardinalityComment(returnType, importHandler);
  }

  /** Return the default value for the given field. Return null if there is no default value. */
  public String defaultValue(Field field, PythonImportHandler importHandler) {
    return defaultValue(field.getType(), importHandler);
  }

  /** Return the default value for the given type. Return null if there is no default value. */
  public String defaultValue(TypeRef type, PythonImportHandler importHandler) {
    // Return empty array if the type is repeated.
    if (type.getCardinality() == Cardinality.REPEATED) {
      return "[]";
    }
    switch (type.getKind()) {
      case TYPE_MESSAGE:
        return importHandler.elementPath(type.getMessageType(), false) + "()";
      case TYPE_ENUM:
        Preconditions.checkArgument(
            type.getEnumType().getValues().size() > 0, "enum must have a value");
        return importHandler.elementPath(type.getEnumType().getValues().get(0), false);
      default:
        if (type.isPrimitive()) {
          return DEFAULT_VALUE_MAP.get(type.getKind());
        }
        throw new IllegalArgumentException("unknown type kind: " + type.getKind());
    }
  }

  /** Return whether the given field's default value is mutable in python. */
  public boolean isDefaultValueMutable(Field field) {
    TypeRef type = field.getType();
    if (type.getCardinality() == Cardinality.REPEATED) {
      return true;
    }
    switch (type.getKind()) {
      case TYPE_MESSAGE: // Fall-through.
      case TYPE_ENUM:
        return true;
      default:
        return false;
    }
  }

  private String getSphinxifiedScopedDescription(ProtoElement element) {
    return PythonSphinxCommentFixer.sphinxify(DocumentationUtil.getScopedDescription(element));
  }

  /**
   * Get a Python formatted primitive value, given a primitive type and a string representation of
   * that value. The value must be a valid example of that type. Values of type Bool must be either
   * the string 'true' or 'false' (other capitalizations are permitted).
   */
  public String renderPrimitiveValue(TypeRef type, String value) {
    Type primitiveType = type.getKind();
    if (!PRIMITIVE_TYPE_NAMES.containsKey(primitiveType)) {
      throw new IllegalArgumentException(
          "Initial values are only supported for primitive types, got type "
              + type
              + ", with value "
              + value);
    }
    switch (primitiveType) {
      case TYPE_BOOL:
        // Capitalize first letter
        return lowerCamelToUpperCamel(value.toLowerCase());
      case TYPE_STRING:
      case TYPE_BYTES:
        return "'" + value + "'";
      default:
        // Types that do not need to be modified (e.g. TYPE_INT32) are handled here
        return value;
    }
  }
}

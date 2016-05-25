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
import com.google.api.codegen.py.PythonImport.ImportType;
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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * A GapicContext specialized for Python.
 */
public class PythonGapicContext extends GapicContext implements PythonContext {

  /**
   * A map from primitive types to its default value.
   */
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

  /**
   * A map from primitive types to their names in Python.
   */
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
  private PythonSnippetSet<?> pythonSnippetSet;

  public PythonGapicContext(Model model, ApiConfig apiConfig) {
    super(model, apiConfig);
    this.pythonCommon = new PythonContextCommon();
  }

  @Override
  public void setPythonSnippetSet(PythonSnippetSet<?> pythonSnippetSet) {
    this.pythonSnippetSet = pythonSnippetSet;
  }

  public PythonContextCommon python() {
    return pythonCommon;
  }

  // Snippet Helpers
  // ===============

  public String filePath(ProtoFile file) {
    return file.getSimpleName().replace(".proto", "_pb2.py");
  }

  /**
   * Return a Python docstring to be associated with the given ProtoElement.
   */
  public List<String> comments(ProtoElement element, PythonImportHandler importHandler) {
    if (element instanceof Method) {
      return methodComments((Method) element, importHandler);
    } else if (element instanceof MessageType) {
      return messageComments((MessageType) element, importHandler);
    } else {
      return defaultComments(element);
    }
  }

  /**
   * Return comments lines for a given proto element, extracted directly from the proto doc
   */
  private List<String> defaultComments(ProtoElement element) {
    if (!element.hasAttribute(ElementDocumentationAttribute.KEY)) {
      return ImmutableList.<String>of("");
    }
    return pythonCommon.convertToCommentedBlock(getSphinxifiedScopedDescription(element));
  }

  /**
   * Returns type information for a field in Sphinx docstring style.
   */
  private String fieldTypeCardinalityComment(Field field, PythonImportHandler importHandler) {
    return typeCardinalityComment(field.getType(), importHandler);
  }

  private String typeCardinalityComment(TypeRef type, PythonImportHandler importHandler) {
    boolean closingBrace = false;
    String prefix;
    if (type.getCardinality() == Cardinality.REPEATED) {
      if (type.isMap()) {
        prefix = String.format("dict[%s, ", fieldTypeComment(type.getMapKeyField(), importHandler));
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

  /**
   * Returns type information for a field in Sphinx docstring style.
   */
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

  /**
   * Returns a comment string for field, consisting of type information and proto comment.
   */
  private String fieldComment(Field field, PythonImportHandler importHandler) {
    String comment =
        String.format(
            "  %s (%s)", field.getSimpleName(), fieldTypeCardinalityComment(field, importHandler));
    String paramComment = getSphinxifiedScopedDescription(field);
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
  private List<String> messageComments(MessageType msg, PythonImportHandler importHandler) {
    // Generate parameter types
    StringBuilder paramTypesBuilder = new StringBuilder();
    paramTypesBuilder.append("Attributes:\n");
    for (Field field : msg.getFields()) {
      paramTypesBuilder.append(fieldComment(field, importHandler));
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
  private String returnTypeComment(Method method, PythonImportHandler importHandler) {
    MessageType returnMessageType = method.getOutputMessage();
    if (PythonProtoElements.isEmptyMessage(returnMessageType)) {
      return null;
    }

    String path = importHandler.elementPath(returnMessageType, true);
    String classInfo = ":class:`" + path + "` instance";
    MethodConfig config =
        getApiConfig().getInterfaceConfig((Interface) method.getParent()).getMethodConfig(method);

    if (config.isPageStreaming()) {
      return "Yields:\n  Instances of "
          + fieldTypeComment(config.getPageStreaming().getResourcesField(), importHandler)
          + "\n  unless page streaming is disabled through the call options. If"
          + "\n  page streaming is disabled, a single \n  "
          + classInfo
          + "\n  is returned.";

    } else {
      return "Returns:\n  A " + classInfo + ".";
    }
  }

  /**
   * Return comments lines for a given method, consisting of proto doc and parameter type
   * documentation.
   */
  private List<String> methodComments(Method method, PythonImportHandler importHandler) {
    String sampleCode = methodSnippet(method, importHandler);

    // Generate parameter types
    StringBuilder paramTypesBuilder = new StringBuilder();
    paramTypesBuilder.append("Args:\n");
    for (Field field : this.messages().flattenedFields(method.getInputType())) {
      paramTypesBuilder.append(fieldComment(field, importHandler));
    }
    paramTypesBuilder.append(
        "  options (:class:`google.gax.CallOptions`): "
            + "Overrides the default\n    settings for this call, e.g, timeout, retries etc.");
    String paramTypes = paramTypesBuilder.toString();

    String returnType = returnTypeComment(method, importHandler);

    // Generate comment contents
    StringBuilder contentBuilder = new StringBuilder();
    if (method.hasAttribute(ElementDocumentationAttribute.KEY)) {
      String sphinxified = getSphinxifiedScopedDescription(method);
      sphinxified = sphinxified.trim();
      contentBuilder.append(sphinxified.replaceAll("\\s*\\n\\s*", "\n"));
      if (!Strings.isNullOrEmpty(paramTypes)) {
        contentBuilder.append("\n\n");
      }
    }
    contentBuilder.append(sampleCode);
    contentBuilder.append("\n\n");
    contentBuilder.append(paramTypes);
    if (returnType != null) {
      contentBuilder.append("\n\n" + returnType);
    }

    contentBuilder.append(
        "\n\nRaises:\n  :exc:`google.gax.errors.GaxError` if the RPC is aborted.");
    return pythonCommon.convertToCommentedBlock(contentBuilder.toString());
  }
  
  private String methodSnippet(Method method, PythonImportHandler importHandler) {
    if (!getApiConfig().generateSamples()) {
      return "";
    }

    Interface service = (Interface) method.getParent();
    List<String> importStrings = new ArrayList<>();
    importStrings.add(
        PythonImport.create(
                ImportType.APP,
                method.getFile().getProto().getPackage()
                    + "."
                    + PythonProtoElements.getPbFileName(method.getInputMessage()),
                getApiWrapperName(service))
            .importString());

    MethodConfig methodConfig = getApiConfig().getInterfaceConfig(service).getMethodConfig(method);
    String methodName = upperCamelToLowerCamel(method.getSimpleName());

    Iterable<Field> requiredFields = methodConfig.getRequiredFields();
    Iterable<Field> optionalFields = methodConfig.getOptionalFields();
    List<Field> fields = new ArrayList<Field>();
    for (Field field : requiredFields) {
      fields.add(field);
    }
    for (Field field : optionalFields) {
      fields.add(field);
    }

    PythonDocConfig docConfig =
        PythonDocConfig.newBuilder()
            .setAppImports(importStrings)
            .setApiName(getApiWrapperName((Interface) method.getParent()))
            .setMethodName(methodName)
            .setReturnType(returnTypeOrEmpty(method.getOutputType(), importHandler))
            .setFieldInitCode(this, service, method, fields)
            .setFieldParams(this, fields)
            // FIXME: this should not be hard-coded
            .setPagedVariant(false)
            .build();

    return generateMethodSampleCode(docConfig);
  }

  private String returnTypeOrEmpty(TypeRef returnType, PythonImportHandler importHandler) {
    return messages().isEmptyType(returnType)
        ? ""
        : typeCardinalityComment(returnType, importHandler);
  }

  /**
   * Generate the example snippet for a method.
   */
  public String generateMethodSampleCode(PythonDocConfig config) {
    return pythonSnippetSet.generateMethodSampleCode(config).prettyPrint();
  }

  /**
   * Return the default value for the given field. Return null if there is no default value.
   */
  public String defaultValue(Field field, PythonImportHandler importHandler) {
    TypeRef type = field.getType();
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

  /**
   * Return whether the given field's default value is mutable in python.
   */
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
}

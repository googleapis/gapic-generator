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

import com.google.api.codegen.GapicContext;
import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.transformer.DefaultFeatureConfig;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.transformer.py.PythonApiMethodParamTransformer;
import com.google.api.codegen.transformer.py.PythonImportSectionTransformer;
import com.google.api.codegen.transformer.py.PythonModelTypeNameConverter;
import com.google.api.codegen.transformer.py.PythonSurfaceNamer;
import com.google.api.codegen.util.py.PythonCommentReformatter;
import com.google.api.codegen.util.py.PythonTypeTable;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.aspects.documentation.model.ElementDocumentationAttribute;
import com.google.api.tools.framework.model.EnumType;
import com.google.api.tools.framework.model.EnumValue;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.api.tools.framework.model.TypeRef.Cardinality;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** A GapicContext specialized for Python. */
public class PythonGapicContext extends GapicContext {

  /** A map from primitive types to its default value. */
  private static final ImmutableMap<Type, String> DEFAULT_VALUE_MAP =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "False")
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
          .put(Type.TYPE_BYTES, "b\'\'")
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

  private PackageMetadataConfig packageConfig;

  public PythonGapicContext(Model model, ApiConfig apiConfig, PackageMetadataConfig packageConfig) {
    super(model, apiConfig);
    this.packageConfig = packageConfig;
    this.pythonCommon = new PythonContextCommon();
  }

  public PythonContextCommon python() {
    return pythonCommon;
  }

  @Override
  protected boolean isSupported(Method method) {
    return true;
  }

  // Snippet Helpers
  // ===============

  /**
   * Return ApiMethodView for sample gen.
   *
   * <p>TODO(eoogbe): Temporary solution to use MVVM with just sample gen. This class will
   * eventually go away when code gen also converts to MVVM.
   */
  public ApiMethodView getApiMethodView(Interface service, Method method) {
    SurfaceTransformerContext context = getSurfaceTransformerContextFromService(service);
    MethodTransformerContext methodContext = context.asDynamicMethodContext(method);
    DynamicLangApiMethodTransformer apiMethodTransformer =
        new DynamicLangApiMethodTransformer(
            new PythonApiMethodParamTransformer(),
            new InitCodeTransformer(new PythonImportSectionTransformer()));

    return apiMethodTransformer.generateMethod(methodContext);
  }

  private SurfaceTransformerContext getSurfaceTransformerContextFromService(Interface service) {
    ModelTypeTable modelTypeTable =
        new ModelTypeTable(
            new PythonTypeTable(getApiConfig().getPackageName()),
            new PythonModelTypeNameConverter(getApiConfig().getPackageName()));
    return SurfaceTransformerContext.create(
        service,
        getApiConfig(),
        modelTypeTable,
        new PythonSurfaceNamer(getApiConfig().getPackageName()),
        new DefaultFeatureConfig());
  }

  public String filePath(ProtoFile file, PythonImportHandler importHandler) {
    return importHandler
        .protoPackageToPythonPackage(file.getSimpleName(), "/")
        .replace(".proto", "_pb2.py");
  }

  /** Return the package name for the GAPIC package. * */
  public String gapicPackageName() {
    return "gapic-" + packageConfig.packageName(TargetLanguage.PYTHON);
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

  private String enumClassName(EnumType enumType) {
    return "enums." + pythonCommon.wrapIfKeywordOrBuiltIn(pathInEnumFile(enumType));
  }

  private String typeComment(TypeRef type, PythonImportHandler importHandler) {
    switch (type.getKind()) {
      case TYPE_MESSAGE:
        return ":class:`" + importHandler.elementPath(type.getMessageType(), true) + "`";
      case TYPE_ENUM:
        return "enum :class:`"
            + getApiConfig().getPackageName()
            + "."
            + enumClassName(type.getEnumType())
            + "`";
      default:
        if (type.isPrimitive()) {
          return PRIMITIVE_TYPE_NAMES.get(type.getKind());
        } else {
          throw new IllegalArgumentException("unknown type kind: " + type.getKind());
        }
    }
  }

  /** Returns a comment string for field, consisting of type information and proto comment. */
  private String fieldComment(String name, String type, String paramComment) {
    String comment = String.format("  %s (%s)", name, type);
    if (!Strings.isNullOrEmpty(paramComment)) {
      if (paramComment.charAt(paramComment.length() - 1) == '\n') {
        paramComment = paramComment.substring(0, paramComment.length() - 1);
      }
      comment += ": " + paramComment.replaceAll("(\\r?\\n)", "\n    ");
    }
    return comment + "\n";
  }

  /** Alternative way of calling fieldComment for proto fields. */
  private String fieldComment(
      String name, Field field, PythonImportHandler importHandler, String paramComment) {
    if (paramComment == null) {
      paramComment = getSphinxifiedScopedDescription(field);
    }
    return fieldComment(name, fieldTypeCardinalityComment(field, importHandler), paramComment);
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
      paramTypesBuilder.append(fieldComment(field.getSimpleName(), field, importHandler, null));
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
  public String returnTypeComment(
      Interface service, Method method, PythonImportHandler importHandler) {
    MethodConfig config = getApiConfig().getInterfaceConfig(service).getMethodConfig(method);
    if (MethodConfig.isReturnEmptyMessageMethod(method)) {
      return "";
    }

    String path = returnTypeCommentPath(method, config, importHandler);
    String classInfo = ":class:`" + path + "`";

    if (method.getResponseStreaming()) {
      return "Returns:\n" + "  iterator[" + classInfo + "].";
    } else if (config.isPageStreaming()) {
      return "Returns:"
          + "\n  A :class:`google.gax.PageIterator` instance. By default, this"
          + "\n  is an iterable of "
          + fieldTypeComment(config.getPageStreaming().getResourcesField(), importHandler)
          + " instances."
          + "\n  This object can also be configured to iterate over the pages"
          + "\n  of the response through the `CallOptions` parameter.";
    } else {
      return "Returns:\n  A " + classInfo + " instance.";
    }
  }

  private String returnTypeCommentPath(
      Method method, MethodConfig config, PythonImportHandler importHandler) {
    if (config.isLongRunningOperation()) {
      return "google.gax._OperationFuture";
    }

    MessageType returnMessageType = method.getOutputMessage();
    return importHandler.elementPath(returnMessageType, true);
  }

  public List<String> splitToLines(String s) {
    return Splitter.on("\n").splitToList(s);
  }

  private String getTrimmedDocs(ProtoElement elt) {
    String description = "";
    if (elt.hasAttribute(ElementDocumentationAttribute.KEY)) {
      // TODO (geigerj): "trimming" the docs means we don't support code blocks. Investigate
      // supporting code blocks here.
      description =
          getSphinxifiedScopedDescription(elt)
              .replaceAll("\\s*\\n\\s*", "\n")
              .replaceAll("::\n", "");
    }
    return description;
  }

  /** Generate comments lines for a given method's description. */
  public List<String> methodDescriptionComments(Method method) {
    return splitToLines(getTrimmedDocs(method));
  }

  public String throwsComment(Interface service, Method method) {
    MethodConfig config = getApiConfig().getInterfaceConfig(service).getMethodConfig(method);
    StringBuilder contentBuilder = new StringBuilder();
    contentBuilder.append("\nRaises:\n  :exc:`google.gax.errors.GaxError` if the RPC is aborted.");
    if (Iterables.size(config.getRequiredFields()) > 0
        || Iterables.size(removePageTokenFromFields(config.getOptionalFields(), config)) > 0) {
      contentBuilder.append("\n  :exc:`ValueError` if the parameters are invalid.");
    }
    return contentBuilder.toString();
  }

  public List<String> enumValueComment(EnumValue value) {
    String description =
        fieldComment(
            pythonCommon.wrapIfKeywordOrBuiltIn(value.getSimpleName()),
            "int",
            getTrimmedDocs(value));
    return splitToLines(description);
  }

  /** Get required (non-optional) fields. */
  public List<Field> getRequiredFields(Interface service, Method method) {
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

  /** Gives the path to a generated enum alias in the enums.py file */
  private String pathInEnumFile(EnumType enumType) {
    List<String> path = new LinkedList<>();
    path.add(enumType.getSimpleName());
    ProtoElement elt = enumType.getParent();
    while (elt.getParent() != null) {
      path.add(0, elt.getSimpleName());
      elt = elt.getParent();
    }
    return Joiner.on(".").join(path);
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
        return "enums."
            + pathInEnumFile(type.getEnumType())
            + "."
            + type.getEnumType().getValues().get(0).getSimpleName();
      default:
        if (type.isPrimitive()) {
          return DEFAULT_VALUE_MAP.get(type.getKind());
        }
        throw new IllegalArgumentException("unknown type kind: " + type.getKind());
    }
  }

  /** Returns the name of Python class for the given type. */
  public String pythonTypeName(TypeRef type, PythonImportHandler importHandler) {
    switch (type.getKind()) {
      case TYPE_MESSAGE:
        return importHandler.elementPath(type.getMessageType(), false);
      case TYPE_ENUM:
        return enumClassName(type.getEnumType());
      default:
        {
          String name = PRIMITIVE_TYPE_NAMES.get(type.getKind());
          if (!Strings.isNullOrEmpty(name)) {
            return name;
          }
          throw new IllegalArgumentException("unknown type kind: " + type.getKind());
        }
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

  public String getSphinxifiedScopedDescription(ProtoElement element) {
    return new PythonCommentReformatter().reformat(DocumentationUtil.getScopedDescription(element));
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

  public List<Interface> getStubInterfaces(Interface service) {
    Map<String, Interface> interfaces = new TreeMap<>();
    for (MethodConfig methodConfig :
        getApiConfig().getInterfaceConfig(service).getMethodConfigs()) {
      String rerouteToGrpcInterface = methodConfig.getRerouteToGrpcInterface();
      Interface target = InterfaceConfig.getTargetInterface(service, rerouteToGrpcInterface);
      interfaces.put(target.getFullName(), target);
    }
    return new ArrayList<>(interfaces.values());
  }

  public String stubName(Interface service) {
    return upperCamelToLowerUnderscore(service.getSimpleName()) + "_stub";
  }

  public String stubNameForMethod(Interface service, Method method) {
    MethodConfig methodConfig = getApiConfig().getInterfaceConfig(service).getMethodConfig(method);
    String rerouteToGrpcInterface = methodConfig.getRerouteToGrpcInterface();
    Interface target = InterfaceConfig.getTargetInterface(service, rerouteToGrpcInterface);
    return stubName(target);
  }

  public boolean isLongrunning(Method method, Interface service) {
    return getApiConfig()
        .getInterfaceConfig(service)
        .getMethodConfig(method)
        .isLongRunningOperation();
  }

  public boolean hasLongrunningMethod(Interface service) {
    for (Method method : getSupportedMethodsV2(service)) {
      if (isLongrunning(method, service)) {
        return true;
      }
    }
    return false;
  }
}

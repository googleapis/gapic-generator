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
package com.google.api.codegen.transformer.nodejs;

import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.metacode.InitFieldConfig;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.Synchronicity;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.js.JSCommentReformatter;
import com.google.api.codegen.util.js.JSNameFormatter;
import com.google.api.codegen.util.js.JSTypeTable;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.EnumType;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The SurfaceNamer for NodeJS. */
public class NodeJSSurfaceNamer extends SurfaceNamer {
  private static final JSCommentReformatter commentReformatter = new JSCommentReformatter();

  private final boolean isGcloud;
  private final String packageName;

  public NodeJSSurfaceNamer(String packageName, boolean isGcloud) {
    super(
        new JSNameFormatter(),
        new ModelTypeFormatterImpl(new NodeJSModelTypeNameConverter(packageName)),
        new JSTypeTable(packageName),
        new JSCommentReformatter(),
        packageName,
        packageName);
    this.packageName = packageName;
    this.isGcloud = isGcloud;
  }

  @Override
  public SurfaceNamer cloneWithPackageName(String packageName) {
    return new NodeJSSurfaceNamer(packageName, isGcloud);
  }

  /**
   * NodeJS uses a special format for ApiWrapperModuleName.
   *
   * <p>The name for the module for this vkit module. This assumes that the package_name in the API
   * config will be in the format of 'apiname.version', and extracts the 'apiname'.
   */
  @Override
  public String getApiWrapperModuleName() {
    List<String> names = Splitter.on(".").splitToList(packageName);
    return names.get(0);
  }

  @Override
  public String getApiWrapperModuleVersion() {
    List<String> names = Splitter.on(".").splitToList(packageName);
    if (names.size() < 2) {
      return null;
    }
    return names.get(names.size() - 1);
  }

  @Override
  public String getPackageServiceName(Interface apiInterface) {
    return getReducedServiceName(apiInterface.getSimpleName()).toLowerCamel();
  }

  @Override
  public String getApiWrapperClassConstructorName(Interface apiInterface) {
    return publicFieldName(Name.upperCamel(apiInterface.getSimpleName(), "Client"));
  }

  @Override
  public String getFieldSetFunctionName(TypeRef type, Name identifier) {
    if (type.isMap() || type.isRepeated()) {
      return publicMethodName(Name.from("add").join(identifier));
    } else {
      return publicMethodName(Name.from("set").join(identifier));
    }
  }

  @Override
  public String getPathTemplateName(
      Interface apiInterface, SingleResourceNameConfig resourceNameConfig) {
    return inittedConstantName(Name.from(resourceNameConfig.getEntityName(), "path", "template"));
  }

  @Override
  public String getParseFunctionName(String var, SingleResourceNameConfig resourceNameConfig) {
    return staticFunctionName(
        Name.from("match", var, "from", resourceNameConfig.getEntityName(), "name"));
  }

  @Override
  public String getFormatFunctionName(
      Interface apiInterface, SingleResourceNameConfig resourceNameConfig) {
    return staticFunctionName(Name.from(resourceNameConfig.getEntityName(), "path"));
  }

  @Override
  public String getClientConfigPath(Interface apiInterface) {
    return Name.upperCamel(apiInterface.getSimpleName()).join("client_config").toLowerUnderscore();
  }

  public String getClientFileName(Interface apiInterface) {
    return Name.upperCamel(apiInterface.getSimpleName()).join("client").toLowerUnderscore();
  }

  @Override
  public boolean shouldImportRequestObjectParamType(Field field) {
    return field.getType().isMap();
  }

  @Override
  public String getOptionalArrayTypeName() {
    return "gax.CallOptions";
  }

  @Override
  public String getDynamicLangReturnTypeName(Method method, GapicMethodConfig methodConfig) {
    if (new ServiceMessages().isEmptyType(method.getOutputType())) {
      return "";
    }

    return getModelTypeFormatter().getFullNameFor(method.getOutputType());
  }

  @Override
  public String getFullyQualifiedStubType(Interface apiInterface) {
    return getModelTypeFormatter().getFullNameFor(apiInterface);
  }

  @Override
  public String getGrpcClientImportName(Interface apiInterface) {
    return "grpc-" + NamePath.dotted(apiInterface.getFile().getFullName()).toDashed();
  }

  @Override
  public String getFieldGetFunctionName(FeatureConfig featureConfig, FieldConfig fieldConfig) {
    Field field = fieldConfig.getField();
    return Name.from(field.getSimpleName()).toLowerCamel();
  }

  @Override
  public String getFieldGetFunctionName(TypeRef type, Name identifier) {
    return identifier.toLowerCamel();
  }

  @Override
  public String getAsyncApiMethodName(Method method, VisibilityConfig visibility) {
    return getApiMethodName(Name.upperCamel(method.getSimpleName()), visibility);
  }

  /** Return JSDoc callback comment and return type comment for the given method. */
  @Override
  public List<String> getReturnDocLines(
      GapicInterfaceContext context, GapicMethodConfig methodConfig, Synchronicity synchronicity) {
    Method method = methodConfig.getMethod();
    if (method.getRequestStreaming() && method.getResponseStreaming()) {
      return bidiStreamingReturnDocLines(method);
    } else if (method.getResponseStreaming()) {
      return responseStreamingReturnDocLines(method);
    }

    List<String> callbackLines = returnCallbackDocLines(context.getModelTypeTable(), methodConfig);
    List<String> returnObjectLines =
        returnObjectDocLines(context.getModelTypeTable(), methodConfig);
    return ImmutableList.<String>builder().addAll(callbackLines).addAll(returnObjectLines).build();
  }

  private List<String> bidiStreamingReturnDocLines(Method method) {
    return ImmutableList.<String>builder()
        .add(
            "@returns {Stream}",
            "  An object stream which is both readable and writable. It accepts objects",
            "  representing "
                + commentReformatter.getLinkedElementName(method.getInputType().getMessageType())
                + " for write() method, and",
            "  will emit objects representing "
                + commentReformatter.getLinkedElementName(method.getOutputType().getMessageType())
                + " on 'data' event asynchronously.")
        .build();
  }

  private List<String> responseStreamingReturnDocLines(Method method) {
    return ImmutableList.<String>builder()
        .add(
            "@returns {Stream}",
            "  An object stream which emits "
                + commentReformatter.getLinkedElementName(method.getOutputType().getMessageType())
                + " on 'data' event.")
        .build();
  }

  @Override
  public String getTypeNameDoc(ModelTypeTable typeTable, TypeRef typeRef) {
    if (typeRef.isMessage()) {
      return "an object representing "
          + commentReformatter.getLinkedElementName(typeRef.getMessageType());
    } else if (typeRef.isEnum()) {
      return "a number of " + commentReformatter.getLinkedElementName(typeRef.getEnumType());
    }
    // Converting to lowercase because "String" is capitalized in NodeJSModelTypeNameConverter.
    return "a " + getParamTypeNoCardinality(typeTable, typeRef).toLowerCase();
  }

  private List<String> returnCallbackDocLines(
      ModelTypeTable typeTable, GapicMethodConfig methodConfig) {
    String returnTypeDoc = returnTypeDoc(typeTable, methodConfig);
    Method method = methodConfig.getMethod();
    String classInfo = getParamTypeName(typeTable, method.getOutputType());
    String callbackType;
    if (isProtobufEmpty(method.getOutputMessage())) {
      callbackType = "function(?Error)";
    } else if (methodConfig.isPageStreaming()) {
      callbackType = String.format("function(?Error, ?Array, ?Object, ?%s)", classInfo);
    } else {
      callbackType = String.format("function(?Error, ?%s)", classInfo);
    }
    ImmutableList.Builder<String> callbackLines = ImmutableList.builder();
    callbackLines.add(
        "@param {" + callbackType + "=} callback",
        "  The function which will be called with the result of the API call.");
    if (!isProtobufEmpty(method.getOutputMessage())) {
      callbackLines.add("", "  The second parameter to the callback is " + returnTypeDoc + ".");
      if (methodConfig.isPageStreaming()) {
        callbackLines.add(
            "",
            "  When autoPaginate: false is specified through options, it contains the result",
            "  in a single response. If the response indicates the next page exists, the third",
            "  parameter is set to be used for the next request object. The fourth parameter keeps",
            "  the raw response object of "
                + getTypeNameDoc(typeTable, method.getOutputType())
                + ".");
      }
    }
    return callbackLines.build();
  }

  private List<String> returnObjectDocLines(
      ModelTypeTable typeTable, GapicMethodConfig methodConfig) {
    String returnTypeDoc = returnTypeDoc(typeTable, methodConfig);
    Method method = methodConfig.getMethod();
    ImmutableList.Builder<String> returnMessageLines = ImmutableList.builder();
    if (method.getRequestStreaming()) {
      returnMessageLines.add(
          "@return {Stream} - A writable stream which accepts objects representing",
          "  "
              + commentReformatter.getLinkedElementName(method.getInputType().getMessageType())
              + " for write() method.");
    } else {
      if (isProtobufEmpty(method.getOutputMessage())) {
        returnMessageLines.add(
            "@return {Promise} - The promise which resolves when API call finishes.");
      } else {
        returnMessageLines.add(
            "@return {Promise} - The promise which resolves to an array.",
            "  The first element of the array is " + returnTypeDoc + ".");
        if (methodConfig.isPageStreaming()) {
          returnMessageLines.add(
              "",
              "  When autoPaginate: false is specified through options, the array has three "
                  + "elements.",
              "  The first element is " + returnTypeDoc + " in a single response.",
              "  The second element is the next request object if the response",
              "  indicates the next page exists, or null. The third element is ",
              "  " + getTypeNameDoc(typeTable, method.getOutputType()) + ".",
              "");
        }
      }
      returnMessageLines.add(
          "  The promise has a method named \"cancel\" which cancels the ongoing API call.");
    }
    return returnMessageLines.build();
  }

  @Override
  public String getParamTypeName(ModelTypeTable typeTable, TypeRef type) {
    String cardinalityComment = "";
    if (type.getCardinality() == TypeRef.Cardinality.REPEATED) {
      if (type.isMap()) {
        String keyType = getParamTypeName(typeTable, type.getMapKeyField().getType());
        String valueType = getParamTypeName(typeTable, type.getMapValueField().getType());
        return String.format("Object.<%s, %s>", keyType, valueType);
      } else {
        cardinalityComment = "[]";
      }
    }

    return String.format("%s%s", getParamTypeNoCardinality(typeTable, type), cardinalityComment);
  }

  private boolean isProtobufEmpty(MessageType message) {
    return message.getFullName().equals("google.protobuf.Empty");
  }

  private String returnTypeDoc(ModelTypeTable typeTable, GapicMethodConfig methodConfig) {
    String returnTypeDoc = "";
    if (methodConfig.isPageStreaming()) {
      returnTypeDoc = "Array of ";
      TypeRef resourcesType = methodConfig.getPageStreaming().getResourcesField().getType();
      if (resourcesType.isMessage()) {
        returnTypeDoc += commentReformatter.getLinkedElementName(resourcesType.getMessageType());
      } else if (resourcesType.isEnum()) {
        returnTypeDoc += commentReformatter.getLinkedElementName(resourcesType.getEnumType());
      } else {
        // Converting to lowercase because "String" is capitalized in NodeJSModelTypeNameConverter.
        returnTypeDoc += getParamTypeNoCardinality(typeTable, resourcesType).toLowerCase();
      }
    } else if (methodConfig.isLongRunningOperation()) {
      returnTypeDoc =
          "a [gax.Operation]{@link https://googleapis.github.io/gax-nodejs/Operation} object";
    } else {
      returnTypeDoc = getTypeNameDoc(typeTable, methodConfig.getMethod().getOutputType());
    }
    return returnTypeDoc;
  }

  private String getParamTypeNoCardinality(ModelTypeTable typeTable, TypeRef type) {
    if (type.isMessage()) {
      return "Object";
    } else if (type.isEnum()) {
      return "number";
    } else {
      return typeTable.getFullNameForElementType(type);
    }
  }

  @Override
  public String getProtoFileName(ProtoFile file) {
    String filePath = file.getSimpleName().replace(".proto", ".js");
    if (commentReformatter.isExternalFile(file)) {
      filePath = filePath.replaceAll("/", "_");
    } else {
      int lastSlash = filePath.lastIndexOf('/');
      if (lastSlash >= 0) {
        filePath = filePath.substring(lastSlash + 1);
      }
    }
    return filePath;
  }

  @Override
  public List<String> getDocLines(Field field) {
    ImmutableList.Builder<String> lines = ImmutableList.builder();
    List<String> fieldDocLines = getDocLines(DocumentationUtil.getScopedDescription(field));
    String extraFieldDescription = getExtraFieldDescription(field);

    lines.addAll(fieldDocLines);
    if (!Strings.isNullOrEmpty(extraFieldDescription)) {
      if (!fieldDocLines.isEmpty()) {
        // Add a break if there was field docs and an extra description.
        lines.add("");
      }
      lines.add(extraFieldDescription);
    }
    return lines.build();
  }

  private String getExtraFieldDescription(Field field) {
    boolean fieldIsMessage = field.getType().isMessage() && !field.getType().isMap();
    boolean fieldIsEnum = field.getType().isEnum();
    if (fieldIsMessage) {
      return "This object should have the same structure as "
          + commentReformatter.getLinkedElementName(field.getType().getMessageType());
    } else if (fieldIsEnum) {
      return "The number should be among the values of "
          + commentReformatter.getLinkedElementName(field.getType().getEnumType());
    }
    return "";
  }

  @Override
  public String getMessageTypeName(ModelTypeTable typeTable, MessageType message) {
    // JSTypeTable produces nicknames which are qualified with the a packagePrefix which needs
    // to be stripped from the message type name.
    List<String> messageNames =
        Arrays.asList(typeTable.getNicknameFor(TypeRef.of(message)).split("\\."));
    return messageNames.get(messageNames.size() - 1);
  }

  @Override
  public String getEnumTypeName(ModelTypeTable typeTable, EnumType enumType) {
    // JSTypeTable produces nicknames which are qualified with the a packagePrefix which needs
    // to be stripped from the enum type name.
    List<String> enumNames =
        Arrays.asList(typeTable.getNicknameFor(TypeRef.of(enumType)).split("\\."));
    return enumNames.get(enumNames.size() - 1);
  }

  @Override
  public String getServiceFileName(GapicInterfaceConfig interfaceConfig) {
    return Name.upperCamel(interfaceConfig.getInterface().getSimpleName())
            .join("client")
            .toLowerUnderscore()
        + ".js";
  }

  @Override
  public String getSourceFilePath(String path, String publicClassName) {
    return path + File.separator + Name.upperCamel(publicClassName).toLowerUnderscore() + ".js";
  }

  @Override
  public String getPackageName() {
    if (isGcloud) {
      return "@google-cloud/" + packageName.split("\\.")[0];
    }
    return packageName;
  }

  @Override
  public String getByteLengthFunctionName(TypeRef typeRef) {
    switch (typeRef.getKind()) {
      case TYPE_MESSAGE:
        return "gax.createByteLengthFunction(loadedProtos."
            + typeRef.getMessageType().getFullName()
            + ")";
      case TYPE_STRING:
      case TYPE_BYTES:
        return "function(s) { return s.length; }";
      default:
        // There is no easy way to say the actual length of the numeric fields.
        // For now throwing an exception.
        throw new IllegalArgumentException(
            "Can't determine the byte length function for " + typeRef.getKind());
    }
  }

  @Override
  public String getLocalPackageName() {
    // NodeJS module names can be hyphen separated.
    return Name.from(getApiWrapperModuleName().split("[^a-zA-Z0-9']+")).toLowerCamel();
  }

  @Override
  public String getStreamTypeName(GrpcStreamingType type) {
    switch (type) {
      case BidiStreaming:
        return "gax.StreamType.BIDI_STREAMING";
      case ClientStreaming:
        return "gax.StreamType.CLIENT_STREAMING";
      case ServerStreaming:
        return "gax.StreamType.SERVER_STREAMING";
      default:
        return getNotImplementedString(
            "SurfaceNamer.getStreamTypeName(GrpcStreamingType." + type.toString() + ")");
    }
  }

  @Override
  public String injectRandomStringGeneratorCode(String randomString) {
    String delimiter = ",";
    String[] split =
        CommonRenderingUtil.stripQuotes(randomString)
            .replace(
                InitFieldConfig.RANDOM_TOKEN, delimiter + InitFieldConfig.RANDOM_TOKEN + delimiter)
            .split(delimiter);
    ArrayList<String> stringParts = new ArrayList<>();
    for (String token : split) {
      if (token.length() > 0) {
        if (token.equals(InitFieldConfig.RANDOM_TOKEN)) {
          stringParts.add("Date.now().toString()");
        } else {
          stringParts.add("\"" + token + "\"");
        }
      }
    }
    return Joiner.on(" + ").join(stringParts);
  }
}

/* Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.transformer.nodejs;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProtoTypeRef;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.metacode.InitFieldConfig;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.Synchronicity;
import com.google.api.codegen.transformer.TransformationContext;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.VersionMatcher;
import com.google.api.codegen.util.js.JSCommentReformatter;
import com.google.api.codegen.util.js.JSNameFormatter;
import com.google.api.codegen.util.js.JSTypeTable;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.tools.framework.model.EnumType;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    for (String n : names) {
      if (VersionMatcher.isVersion(n)) {
        return n;
      }
    }
    return null;
  }

  @Override
  public String getPackageServiceName(InterfaceConfig interfaceConfig) {
    return getReducedServiceName(interfaceConfig.getInterfaceModel().getSimpleName())
        .toLowerCamel();
  }

  @Override
  public String getApiWrapperClassConstructorName(InterfaceConfig interfaceConfig) {
    return publicFieldName(
        Name.upperCamel(interfaceConfig.getInterfaceModel().getSimpleName(), "Client"));
  }

  @Override
  public String getApiSampleFileName(String className) {
    return Name.anyCamel(className).toLowerUnderscore() + ".js";
  }

  @Override
  public String getFieldSetFunctionName(TypeModel type, Name identifier) {
    if (type.isMap() || type.isRepeated()) {
      return publicMethodName(Name.from("add").join(identifier));
    } else {
      return publicMethodName(Name.from("set").join(identifier));
    }
  }

  @Override
  public String getPathTemplateName(
      InterfaceConfig interfaceConfig, SingleResourceNameConfig resourceNameConfig) {
    return publicFieldName(Name.from(resourceNameConfig.getEntityName(), "path", "template"));
  }

  @Override
  public String getParseFunctionName(String var, SingleResourceNameConfig resourceNameConfig) {
    return staticFunctionName(
        Name.from("match", var, "from", resourceNameConfig.getEntityName(), "name"));
  }

  @Override
  public String getFormatFunctionName(
      InterfaceConfig interfaceConfig, SingleResourceNameConfig resourceNameConfig) {
    return staticFunctionName(Name.from(resourceNameConfig.getEntityName(), "path"));
  }

  @Override
  public String getClientConfigPath(InterfaceConfig interfaceConfig) {
    return Name.upperCamel(interfaceConfig.getInterfaceModel().getSimpleName())
        .join("client_config")
        .toLowerUnderscore();
  }

  public String getClientFileName(InterfaceConfig interfaceConfig) {
    return Name.upperCamel(interfaceConfig.getInterfaceModel().getSimpleName())
        .join("client")
        .toLowerUnderscore();
  }

  @Override
  public boolean shouldImportRequestObjectParamType(FieldModel field) {
    return field.isMap();
  }

  @Override
  public String getOptionalArrayTypeName() {
    return "gax.CallOptions";
  }

  @Override
  public String getDynamicLangReturnTypeName(MethodContext methodContext) {
    MethodModel method = methodContext.getMethodModel();
    if (method.isOutputTypeEmpty()) {
      return "";
    }

    return method.getOutputTypeName(methodContext.getTypeTable()).getFullName();
  }

  @Override
  public String getFullyQualifiedStubType(InterfaceModel apiInterface) {
    return getModelTypeFormatter().getFullNameFor(apiInterface);
  }

  @Override
  public String getFieldGetFunctionName(FeatureConfig featureConfig, FieldConfig fieldConfig) {
    FieldModel field = fieldConfig.getField();
    return Name.from(field.getSimpleName()).toLowerCamel();
  }

  @Override
  public String getFieldGetFunctionName(FieldModel field) {
    return Name.from(field.getSimpleName()).toLowerCamel();
  }

  @Override
  public String getFieldGetFunctionName(FieldModel type, Name identifier) {
    return identifier.toLowerCamel();
  }

  @Override
  public String getAsyncApiMethodName(MethodModel method, VisibilityConfig visibility) {
    return getApiMethodName(Name.upperCamel(method.getSimpleName()), visibility);
  }

  /** Return JSDoc callback comment and return type comment for the given method. */
  @Override
  public List<String> getReturnDocLines(
      TransformationContext context, MethodContext methodContext, Synchronicity synchronicity) {
    GapicMethodConfig methodConfig = (GapicMethodConfig) methodContext.getMethodConfig();
    Method method = methodConfig.getMethod();
    if (method.getRequestStreaming() && method.getResponseStreaming()) {
      return bidiStreamingReturnDocLines(method);
    } else if (method.getResponseStreaming()) {
      return responseStreamingReturnDocLines(method);
    }

    List<String> callbackLines = returnCallbackDocLines(context.getImportTypeTable(), methodConfig);
    List<String> returnObjectLines =
        returnObjectDocLines(context.getImportTypeTable(), methodConfig);
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
  public String getTypeNameDoc(ImportTypeTable typeTable, TypeModel typeModel) {
    TypeRef typeRef = ((ProtoTypeRef) typeModel).getProtoType();
    if (typeRef.isMessage()) {
      return "an object representing "
          + commentReformatter.getLinkedElementName(typeRef.getMessageType());
    } else if (typeRef.isEnum()) {
      return "a number of " + commentReformatter.getLinkedElementName(typeRef.getEnumType());
    }
    // Converting to lowercase because "String" is capitalized in NodeJSModelTypeNameConverter.
    return "a " + getParamTypeNoCardinality(typeTable, typeModel).toLowerCase();
  }

  private List<String> returnCallbackDocLines(
      ImportTypeTable typeTable, GapicMethodConfig methodConfig) {
    String returnTypeDoc = returnTypeDoc(typeTable, methodConfig);
    Method method = methodConfig.getMethod();
    MethodModel methodModel = methodConfig.getMethodModel();
    String classInfo = getParamTypeName(typeTable, methodModel.getOutputType());
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
        "@param {" + callbackType + "} [callback]",
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
                + getTypeNameDoc(typeTable, methodModel.getOutputType())
                + ".");
      }
    }
    return callbackLines.build();
  }

  private List<String> returnObjectDocLines(
      ImportTypeTable typeTable, GapicMethodConfig methodConfig) {
    String returnTypeDoc = returnTypeDoc(typeTable, methodConfig);
    Method method = methodConfig.getMethod();
    ImmutableList.Builder<String> returnMessageLines = ImmutableList.builder();
    if (method.getRequestStreaming()) {
      returnMessageLines.add(
          "@returns {Stream} - A writable stream which accepts objects representing",
          "  "
              + commentReformatter.getLinkedElementName(method.getInputType().getMessageType())
              + " for write() method.");
    } else {
      if (isProtobufEmpty(method.getOutputMessage())) {
        returnMessageLines.add(
            "@returns {Promise} - The promise which resolves when API call finishes.");
      } else {
        returnMessageLines.add(
            "@returns {Promise} - The promise which resolves to an array.",
            "  The first element of the array is " + returnTypeDoc + ".");
        if (methodConfig.isPageStreaming()) {
          returnMessageLines.add(
              "",
              "  When autoPaginate: false is specified through options, the array has three "
                  + "elements.",
              "  The first element is " + returnTypeDoc + " in a single response.",
              "  The second element is the next request object if the response",
              "  indicates the next page exists, or null. The third element is ",
              "  " + getTypeNameDoc(typeTable, methodConfig.getMethodModel().getOutputType()) + ".",
              "");
        }
      }
      returnMessageLines.add(
          "  The promise has a method named \"cancel\" which cancels the ongoing API call.");
    }
    return returnMessageLines.build();
  }

  @Override
  public String getParamTypeName(ImportTypeTable typeTable, TypeModel type) {
    String cardinalityComment = "";
    if (type.isRepeated()) {
      if (type.isMap()) {
        String keyType = getParamTypeName(typeTable, type.getMapKeyType());
        String valueType = getParamTypeName(typeTable, type.getMapValueType());
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

  private String returnTypeDoc(ImportTypeTable typeTable, GapicMethodConfig methodConfig) {
    String returnTypeDoc = "";
    if (methodConfig.isPageStreaming()) {
      returnTypeDoc = "Array of ";
      FieldModel resourcesType = methodConfig.getPageStreaming().getResourcesField();
      if (resourcesType.isMessage()) {
        returnTypeDoc +=
            commentReformatter.getLinkedElementName(
                ((ProtoTypeRef) resourcesType.getType()).getProtoType().getMessageType());
      } else if (resourcesType.isEnum()) {
        returnTypeDoc +=
            commentReformatter.getLinkedElementName(
                ((ProtoTypeRef) resourcesType.getType()).getProtoType().getEnumType());
      } else {
        // Converting to lowercase because "String" is capitalized in NodeJSModelTypeNameConverter.
        returnTypeDoc +=
            getParamTypeNoCardinality(typeTable, resourcesType.getType()).toLowerCase();
      }
    } else if (methodConfig.isLongRunningOperation()) {
      returnTypeDoc =
          "a [gax.Operation]{@link https://googleapis.github.io/gax-nodejs/Operation} object";
    } else {
      returnTypeDoc =
          getTypeNameDoc(typeTable, ProtoTypeRef.create(methodConfig.getMethod().getOutputType()));
    }
    return returnTypeDoc;
  }

  private String getParamTypeNoCardinality(ImportTypeTable typeTable, TypeModel type) {
    if (type.isMessage()) {
      return "Object";
    } else if (type.isEnum()) {
      return "number";
    } else {
      return typeTable.getFullNameForElementType(type);
    }
  }

  @Override
  public String getProtoFileName(String fileSimpleName) {
    String filePath = fileSimpleName.replace(".proto", ".js");
    int lastSlash = filePath.lastIndexOf('/');
    if (lastSlash >= 0) {
      filePath = filePath.substring(lastSlash + 1);
    }
    return filePath;
  }

  @Override
  public List<String> getDocLines(FieldModel field) {
    ImmutableList.Builder<String> lines = ImmutableList.builder();
    List<String> fieldDocLines = getDocLines(field.getScopedDocumentation());
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

  private String getExtraFieldDescription(FieldModel field) {
    boolean fieldIsMessage = field.isMessage() && !field.isMap();
    boolean fieldIsEnum = field.isEnum();
    if (fieldIsMessage) {
      return "This object should have the same structure as "
          + commentReformatter.getLinkedElementName(
              ((ProtoTypeRef) field.getType()).getProtoType().getMessageType());
    } else if (fieldIsEnum) {
      return "The number should be among the values of "
          + commentReformatter.getLinkedElementName(
              ((ProtoTypeRef) field.getType()).getProtoType().getEnumType());
    }
    return "";
  }

  @Override
  public String getMessageTypeName(ImportTypeTable typeTable, MessageType message) {
    // JSTypeTable produces nicknames which are qualified with the a packagePrefix which needs
    // to be stripped from the message type name.
    List<String> messageNames =
        Arrays.asList(
            ((ModelTypeTable) typeTable).getNicknameFor(TypeRef.of(message)).split("\\."));
    return messageNames.get(messageNames.size() - 1);
  }

  @Override
  public String getEnumTypeName(ImportTypeTable typeTable, EnumType enumType) {
    // JSTypeTable produces nicknames which are qualified with the a packagePrefix which needs
    // to be stripped from the enum type name.
    List<String> enumNames =
        Arrays.asList(
            ((ModelTypeTable) typeTable).getNicknameFor(TypeRef.of(enumType)).split("\\."));
    return enumNames.get(enumNames.size() - 1);
  }

  @Override
  public String getServiceFileName(InterfaceConfig interfaceConfig) {
    return Name.upperCamel(interfaceConfig.getRawName()).join("client").toLowerUnderscore() + ".js";
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
  public String getByteLengthFunctionName(FieldModel typeRef) {
    if (typeRef.isMessage()) {
      return "gax.createByteLengthFunction(protoFilesRoot.lookup('"
          + typeRef.getTypeFullName()
          + "'))";
    } else if (typeRef.isString() || typeRef.isBytes()) {
      return "s => s.length";
    } else {
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

  @Override
  public String getIndexAccessorName(int index) {
    return String.format("[%d]", index);
  }

  @Override
  public String getFieldAccessorName(FieldModel field) {
    return "." + getFieldGetFunctionName(field);
  }

  @Override
  public List<String> getPrintSpecs(String spec, List<String> args) {
    if (args.isEmpty()) {
      return Collections.singletonList(spec);
    }
    if (args.size() == 1 && "%s".equals(spec)) {
      return ImmutableList.of(spec, args.get(0));
    }
    String format = spec;
    int cursor = 0;
    int argIndex = 0;
    StringBuilder sb = new StringBuilder();
    while (true) {
      int p = format.indexOf("%", cursor);
      if (p < 0) {
        return Collections.singletonList(
            sb.append(format, cursor, format.length()).toString().replace("'", "\\'"));
      }
      sb.append(format, cursor, p);
      if (format.startsWith("%%", p)) {
        sb.append('%');
      } else if (format.startsWith("%s", p)) {
        Preconditions.checkArgument(
            argIndex < args.size(), "Insufficient arguments for print spec %s", format);
        sb.append(String.format("{$%s}", args.get(argIndex++)));
      } else {
        throw new IllegalArgumentException(String.format("bad format verb: %s", format));
      }
      cursor = p + 2;
    }
  }

  @Override
  public String getSampleResponseVarName(MethodContext context, CallingForm form) {
    switch (form) {
      case Request:
      case RequestStreamingBidi:
      case RequestStreamingClient:
      case RequestStreamingServer:
        return "response";
      case RequestAsyncPaged:
      case RequestAsyncPagedAll:
        return "resource";
      case LongRunningEventEmitter:
      case LongRunningPromise:
        return "finalApiResponse";
      default:
        throw new IllegalArgumentException("illegal calling form for Node.js: " + form);
    }
  }
}

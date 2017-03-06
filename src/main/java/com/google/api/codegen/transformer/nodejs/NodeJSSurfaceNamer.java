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
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.transformer.Synchronicity;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.js.JSCommentReformatter;
import com.google.api.codegen.util.js.JSNameFormatter;
import com.google.api.codegen.util.js.JSTypeTable;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import java.util.List;

/** The SurfaceNamer for NodeJS. */
public class NodeJSSurfaceNamer extends SurfaceNamer {
  public NodeJSSurfaceNamer(String packageName) {
    super(
        new JSNameFormatter(),
        new ModelTypeFormatterImpl(new NodeJSModelTypeNameConverter(packageName)),
        new JSTypeTable(packageName),
        new JSCommentReformatter(),
        packageName);
  }

  /**
   * NodeJS uses a special format for ApiWrapperModuleName.
   *
   * <p>The name for the module for this vkit module. This assumes that the package_name in the API
   * config will be in the format of 'apiname.version', and extracts the 'apiname' and 'version'
   * part and combine them to lower-camelcased style (like pubsubV1).
   *
   * <p>Based on {@link com.google.api.codegen.nodejs.NodeJSGapicContext#getModuleName}.
   */
  @Override
  public String getApiWrapperModuleName() {
    List<String> names = Splitter.on(".").splitToList(getPackageName());
    if (names.size() < 2) {
      return getPackageName();
    }
    return names.get(0) + Name.from(names.get(1)).toUpperCamel();
  }

  @Override
  public String getApiWrapperModuleVersion() {
    List<String> names = Splitter.on(".").splitToList(getPackageName());
    if (names.size() < 2) {
      return null;
    }
    return names.get(names.size() - 1);
  }

  @Override
  public String getApiWrapperClassConstructorName(Interface interfaze) {
    return publicFieldName(Name.upperCamel(interfaze.getSimpleName(), "Client"));
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
      Interface service, SingleResourceNameConfig resourceNameConfig) {
    return inittedConstantName(Name.from(resourceNameConfig.getEntityName(), "name", "template"));
  }

  @Override
  public String getFormatFunctionName(
      Interface service, SingleResourceNameConfig resourceNameConfig) {
    return staticFunctionName(Name.from(resourceNameConfig.getEntityName(), "path"));
  }

  @Override
  public String getClientConfigPath(Interface service) {
    return "./resources/"
        + Name.upperCamel(service.getSimpleName()).join("client_config").toLowerUnderscore()
        + ".json";
  }

  public String getClientFileName(Interface service) {
    return Name.upperCamel(service.getSimpleName()).join("client").toLowerUnderscore();
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
  public String getDynamicLangReturnTypeName(Method method, MethodConfig methodConfig) {
    if (new ServiceMessages().isEmptyType(method.getOutputType())) {
      return "";
    }

    return getModelTypeFormatter().getFullNameFor(method.getOutputType());
  }

  @Override
  public String getFullyQualifiedStubType(Interface service) {
    return getModelTypeFormatter().getFullNameFor(service);
  }

  @Override
  public String getGrpcClientImportName(Interface service) {
    return "grpc-" + NamePath.dotted(service.getFile().getFullName()).toDashed();
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
  // TODO(landrito): Move this to a docTransformer.
  @Override
  public List<String> getReturnDocLines(
      SurfaceTransformerContext context, MethodConfig methodConfig, Synchronicity synchronicity) {
    Method method = methodConfig.getMethod();
    if (method.getRequestStreaming() && method.getResponseStreaming()) {
      return bidiStreamingReturnDocLines(method);
    } else if (method.getResponseStreaming()) {
      return responseStreamingReturnDocLines(method);
    }

    List<String> callbackLines = returnCallbackDocLines(context.getTypeTable(), methodConfig);
    List<String> returnObjectLines = returnObjectDocLines(context.getTypeTable(), methodConfig);
    return ImmutableList.<String>builder().addAll(callbackLines).addAll(returnObjectLines).build();
  }

  private List<String> bidiStreamingReturnDocLines(Method method) {
    return ImmutableList.<String>builder()
        .add(
            "@returns {Stream}",
            "  An object stream which is both readable and writable. It accepts objects",
            "  representing "
                + getLinkedElementName(method.getInputType().getMessageType())
                + " for write() method, and",
            "  will emit objects representing "
                + getLinkedElementName(method.getOutputType().getMessageType())
                + " on 'data' event asynchronously.")
        .build();
  }

  private List<String> responseStreamingReturnDocLines(Method method) {
    return ImmutableList.<String>builder()
        .add(
            "@returns {Stream}",
            "  An object stream which emits "
                + getLinkedElementName(method.getOutputType().getMessageType())
                + " on 'data' event.")
        .build();
  }

  @Override
  public String getLinkedElementName(ProtoElement element) {
    if (isExternalFile(element.getFile())) {
      String fullName = element.getFullName();
      return String.format("[%s]{@link external:\"%s\"}", fullName, fullName);
    } else {
      String simpleName = element.getSimpleName();
      return String.format("[%s]{@link %s}", simpleName, simpleName);
    }
  }

  private boolean isExternalFile(ProtoFile file) {
    String filePath = file.getSimpleName();
    for (String commonPath : COMMON_PROTO_PATHS) {
      if (filePath.startsWith(commonPath)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String getTypeNameDoc(ModelTypeTable typeTable, TypeRef typeRef) {
    if (typeRef.isMessage()) {
      return "an object representing " + getLinkedElementName(typeRef.getMessageType());
    } else if (typeRef.isEnum()) {
      return "a number of " + getLinkedElementName(typeRef.getEnumType());
    }
    return "a " + getParamTypeName(typeTable, typeRef);
  }

  private List<String> returnCallbackDocLines(ModelTypeTable typeTable, MethodConfig methodConfig) {
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

  private List<String> returnObjectDocLines(ModelTypeTable typeTable, MethodConfig methodConfig) {
    String returnTypeDoc = returnTypeDoc(typeTable, methodConfig);
    Method method = methodConfig.getMethod();
    ImmutableList.Builder<String> returnMessageLines = ImmutableList.builder();
    if (method.getRequestStreaming()) {
      returnMessageLines.add(
          "@return {Stream} - A writable stream which accepts objects representing",
          "  "
              + getLinkedElementName(method.getInputType().getMessageType())
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
    switch (type.getKind()) {
      case TYPE_MESSAGE:
        return "Object";
      case TYPE_ENUM:
        return "number";
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

  private boolean isProtobufEmpty(MessageType message) {
    return message.getFullName().equals("google.protobuf.Empty");
  }

  private String returnTypeDoc(ModelTypeTable typeTable, MethodConfig methodConfig) {
    String returnTypeDoc = "";
    if (methodConfig.isPageStreaming()) {
      returnTypeDoc = "Array of ";
      TypeRef resourcesType = methodConfig.getPageStreaming().getResourcesField().getType();
      if (resourcesType.isMessage()) {
        returnTypeDoc += getLinkedElementName(resourcesType.getMessageType());
      } else if (resourcesType.isEnum()) {
        returnTypeDoc += getLinkedElementName(resourcesType.getEnumType());
      } else {
        returnTypeDoc += getParamTypeName(typeTable, resourcesType);
      }
    } else if (methodConfig.isLongRunningOperation()) {
      returnTypeDoc =
          "a [gax.Operation]{@link " + "https://googleapis.github.io/gax-nodejs/Operation} object";
    } else {
      returnTypeDoc = getTypeNameDoc(typeTable, methodConfig.getMethod().getOutputType());
    }
    return returnTypeDoc;
  }

  private static final ImmutableSet<String> COMMON_PROTO_PATHS =
      ImmutableSet.of(
          "google/api",
          "google/bytestream",
          "google/logging/type",
          "google/longrunning",
          "google/protobuf",
          "google/rpc",
          "google/type");

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
          .put(Type.TYPE_STRING, "string")
          .put(Type.TYPE_BYTES, "string")
          .build();
}

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
package com.google.api.codegen.go;

import com.google.api.Documentation;
import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.CollectionConfig;
import com.google.api.codegen.GapicContext;
import com.google.api.codegen.LanguageUtil;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.PageStreamingConfig;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.aspects.documentation.model.ElementDocumentationAttribute;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A GapicContext specialized for Go.
 */
public class GoGapicContext extends GapicContext implements GoContext {

  /**
   * A map from primitive types in proto to Java counterparts.
   */
  private static final ImmutableMap<Type, String> PRIMITIVE_TYPE_MAP =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "bool")
          .put(Type.TYPE_DOUBLE, "float64")
          .put(Type.TYPE_FLOAT, "float32")
          .put(Type.TYPE_INT64, "int64")
          .put(Type.TYPE_UINT64, "uint64")
          .put(Type.TYPE_SINT64, "int64")
          .put(Type.TYPE_FIXED64, "int64")
          .put(Type.TYPE_SFIXED64, "int64")
          .put(Type.TYPE_INT32, "int32")
          .put(Type.TYPE_UINT32, "uint32")
          .put(Type.TYPE_SINT32, "int32")
          .put(Type.TYPE_FIXED32, "int32")
          .put(Type.TYPE_SFIXED32, "int32")
          .put(Type.TYPE_STRING, "string")
          .put(Type.TYPE_BYTES, "[]byte")
          .build();

  /**
   * The import path for GAX.
   */
  private static final String GAX_PACKAGE_BASE = "github.com/googleapis/gax-go";

  /**
   * The import path for generated pb.go files for core-proto files.
   */
  private static final String CORE_PROTO_BASE = "github.com/googleapis/proto-client-go";

  /**
   * The set of the core protobuf packages.
   *
   * See the comment above, this is also not used right now.
   */
  private static final ImmutableSet<String> CORE_PROTO_PACKAGES =
      ImmutableSet.<String>builder()
          .add("google/api")
          .add("google/longrunning")
          .add("google/protobuf")
          .add("google/rpc")
          .add("google/type")
          .build();

  private final GoContextCommon goCommon;

  public GoGapicContext(Model model, ApiConfig apiConfig) {
    super(model, apiConfig);
    this.goCommon = new GoContextCommon();
  }

  public String getReducedServiceName(Interface service) {
    String name = service.getSimpleName().replaceAll("V[0-9]+$", "");
    name = name.replaceAll("Service$", "");
    return LanguageUtil.upperCamelToLowerUnderscore(name);
  }

  /**
   * Returns the type name of the gRPC-generated service client.
   */
  public String getServiceClientName(Interface service) {
    return localPackageName(service) + "." + service.getSimpleName() + "Client";
  }

  /**
   * Returns the function name which creates the gRPC-generated service client.
   */
  public String getServiceClientConstructorName(Interface service) {
    return localPackageName(service) + ".New" + service.getSimpleName() + "Client";
  }

  /**
   * Returns the local name used in Go files for the package of the proto.
   */
  private static String localPackageName(ProtoElement proto) {
    return proto.getFile().getProto().getPackage().replace(".", "_");
  }

  /**
   * Returns the Go type name for the specified TypeRef.
   */
  public String typeName(TypeRef type) {
    if (type.isMap()) {
      String keyName = typeName(type.getMapKeyField().getType());
      String valueName = typeName(type.getMapValueField().getType());
      return "map[" + keyName + "]" + valueName;
    } else {
      String name;
      if (type.isMessage()) {
        MessageType messageType = type.getMessageType();
        name = "*" + localPackageName(messageType) + "." + messageType.getProto().getName();
      } else if (type.isPrimitive()) {
        name = PRIMITIVE_TYPE_MAP.get(type.getKind());
      } else {
        throw new IllegalArgumentException("Unknown type: " + type.toString());
      }
      if (type.isRepeated()) {
        return "[]" + name;
      }
      return name;
    }
  }

  /**
   * Returns the (dereferenced) Go type name for the specificed message TypeRef.
   */
  public String messageTypeName(TypeRef type) {
      if (!type.isMessage()) {
        throw new IllegalArgumentException("Expected message type, got: " + type.toString());
      }
      MessageType messageType = type.getMessageType();
      return localPackageName(messageType) + "." + messageType.getProto().getName();
  }

  /**
   * Returns the Go type for the resources field.
   *
   * Note that this returns the individual element type of the resources in the message. For
   * example, if SomeResponse has 'repeated string contents' field, the return value should be
   * 'string', not '[]string', and that's why the snippet can't use typeName() directly.
   */
  public TypeRef getResourceType(Field field) {
    // Creating a copy with 'required' to extract the type name but isn't affected by
    // repeated cardinality.
    return field.getType().makeRequired();
  }

  /**
   * Returns the Go type name for the resources field.
   */
  public String getResourceTypeName(Field field) {
    return typeName(getResourceType(field));
  }

  /**
   * The Go expression to build a value for the specified type.
   */
  public String constructionExpr(TypeRef type) {
    MessageType messageType = type.getMessageType();
    return "&" + localPackageName(messageType) + "." + messageType.getProto().getName();
  }

  /**
   * Returns the list of page streaming configs, grouped by the element type. In Go, the iterator
   * structs are defined per element type.
   */
  public Iterable<PageStreamingConfig> getPageStreamingConfigs(Interface service) {
    Map<String, PageStreamingConfig> streamingConfigs = new LinkedHashMap<>();
    for (Method method : service.getMethods()) {
      MethodConfig methodConfig =
          getApiConfig().getInterfaceConfig(service).getMethodConfig(method);

      if (!methodConfig.isPageStreaming()) {
        continue;
      }
      final PageStreamingConfig config = methodConfig.getPageStreaming();
      String typeName = getIteratorTypeName(config);
      if (!streamingConfigs.containsKey(typeName)) {
        streamingConfigs.put(typeName, config);
      }
    }
    return streamingConfigs.values();
  }

  public String getNextPageTokenType(Interface service, PageStreamingConfig config) {
    return typeName(config.getRequestTokenField().getType());
  }

  /**
   * Returns the upper camel prefix name for the resource field.
   *
   * Specifically:
   * `repeated float hellos`: `Float`
   * `repeated World worlds`: `World`
   */
  private String getSimpleResourcesTypeName(PageStreamingConfig config) {
    TypeRef type = config.getResourcesField().getType();
    if (type.isMessage()) {
      return type.getMessageType().getSimpleName();
    } else {
      return LanguageUtil.lowerCamelToUpperCamel(type.getPrimitiveTypeName());
    }
  }

  /**
   * Returns the Go type name for the page-streaming iterator implementation of the method.
   */
  public String getIteratorTypeName(PageStreamingConfig config) {
    return getSimpleResourcesTypeName(config) + "Iterator";
  }

  /**
   * Returns the zero-value representation for the specified type in Go. See:
   * https://golang.org/ref/spec#The_zero_value
   */
  public String zeroValue(TypeRef type) {
    if (type.isRepeated() || type.isMap() || type.isMessage()) {
      return "nil";
    }

    switch (type.getKind()) {
      case TYPE_BOOL:
        return "false";

      case TYPE_STRING:
        return "\"\"";

      case TYPE_BYTES:
        return "nil";

      default:
        // Anything else -- numeric values.
        return "0";
    }
  }

  /**
   * Returns the Go type name of (gRPC's) response-streaming methods which receives the element
   * objects.
   */
  public String getStreamReceiver(Interface service, Method method) {
    return localPackageName(service)
        + "."
        + service.getSimpleName()
        + "_"
        + method.getSimpleName()
        + "Client";
  }

  /**
   * Returns the doc comments of Go for the proto elements.
   */
  public Iterable<String> comments(ProtoElement element) {
    if (!element.hasAttribute(ElementDocumentationAttribute.KEY)) {
      return ImmutableList.<String>of("");
    }
    return goCommon.getCommentLines(DocumentationUtil.getScopedDescription(element));
  }

  public Iterable<String> getFieldComments(Field field) {
    return goCommon.getCommentLines(
        "\n"
            + lowerUnderscoreToLowerCamel(field.getSimpleName())
            + ": "
            + DocumentationUtil.getScopedDescription(field));
  }

  /**
   * Returns the doc comments of a method in Go style. Right now this assumes comment of gRPC
   * methods starts with a verb.
   */
  public Iterable<String> getMethodComments(Method method, String methodName) {
    if (!method.hasAttribute(ElementDocumentationAttribute.KEY)) {
      return ImmutableList.<String>of("");
    }

    String comment = DocumentationUtil.getScopedDescription(method);
    int firstChar = comment.codePointAt(0);
    if (Character.isUpperCase(firstChar)) {
      String lower = new String(Character.toChars(Character.toLowerCase(firstChar)));
      comment = methodName + " " + lower + comment.substring(Character.charCount(firstChar));
    }
    return goCommon.getCommentLines(comment);
  }

  /**
   * Returns the package doc comments for the entire config.
   */
  public Iterable<String> getPackageDocument(Interface service) {
    Documentation doc = service.getModel().getServiceConfig().getDocumentation();
    return goCommon.getCommentLines(doc.getSummary());
  }

  /**
   * Returns the unique list of path component variable names in the path template.
   */
  public Iterable<String> getCollectionParams(Interface service) {
    TreeSet<String> params = new TreeSet<>();
    for (CollectionConfig config :
        getApiConfig().getInterfaceConfig(service).getCollectionConfigs()) {
      for (String param : config.getNameTemplate().vars()) {
        params.add(param);
      }
    }
    return params;
  }

  /**
   * Returns the package path for the client.
   */
  public String getClientPackagePath(Interface service) {
    return getApiConfig().getPackageName() + "/" + getReducedServiceName(service);
  }

  /**
   * Creates a Go import from the message import.
   */
  private GoImport createMessageImport(MessageType messageType) {
    String pkgName = messageType.getFile().getProto().getOptions().getGoPackage();
    // If there's no `go_package` specified, we guess an import path based on the core proto base
    // repo and the proto package.
    if (Strings.isNullOrEmpty(pkgName)) {
      pkgName = CORE_PROTO_BASE + "/" +
          messageType.getFile().getProto().getPackage().replace(".", "/");
    }
    String localName = localPackageName(messageType);
    return GoImport.create(pkgName, localName);
  }

  private Set<GoImport> getStandardImports(Interface service) {
    TreeSet<GoImport> standardImports = new TreeSet<>();

    standardImports.add(GoImport.create("fmt"));
    standardImports.add(GoImport.create("runtime"));

    if (!getApiConfig().getInterfaceConfig(service).getRetrySettingsDefinition().isEmpty()) {
      standardImports.add(GoImport.create("time"));
    }
    if (hasPageStreamingMethod(service)) {
      standardImports.add(GoImport.create("errors"));
    }
    return standardImports;
  }

  private TreeSet<GoImport> getMessageImports(Interface service) {
    TreeSet<GoImport> messageImports = new TreeSet<>();

    // Add method request-type imports
    for (Method method : service.getMethods()) {
      MessageType inputMessage = method.getInputMessage();
      MessageType outputMessage = method.getOutputMessage();
      MethodConfig methodConfig =
          getApiConfig().getInterfaceConfig(service).getMethodConfig(method);
      messageImports.add(createMessageImport(inputMessage));
      if (!isEmpty(method.getOutputType())) {
        messageImports.add(createMessageImport(outputMessage));
      }
      if (methodConfig.isPageStreaming()) {
        TypeRef resourceType = methodConfig.getPageStreaming().getResourcesField().getType();
        if (resourceType.isMessage()) {
          messageImports.add(createMessageImport(resourceType.getMessageType()));
        }
      }
    }

    return messageImports;
  }

  private List<String> formatImports(Set<GoImport> standard, Set<GoImport> thirdParty) {
    List<String> result = new ArrayList<String>();
    if (standard != null) {
      for (GoImport goImport : standard) {
        result.add("    " + goImport.importString());
      }
      result.add("");
    }
    if (thirdParty != null) {
      for (GoImport goImport : thirdParty) {
        result.add("    " + goImport.importString());
      }
    }
    return result;
  }

  /**
   * Calculates the set of imports and returns a sorted set of Go import output strings. This
   * imitates the same order which gofmt does, which means: - standard imports (Go standard libraries)
   * in alphabetical order - a blank line (so an empty string) - other imports, alphabetical order
   *
   * Each of the lines (except for the blank line) starts with a tab character '\t' for the
   * indentation within the 'import' section in Go file.
   */
  public Iterable<String> getImports(Interface service) {
    TreeSet<GoImport> thirdParty = new TreeSet<>();

    // Add non-service-specific imports.
    thirdParty.add(GoImport.create("golang.org/x/net/context"));
    thirdParty.add(GoImport.create("google.golang.org/grpc"));
    thirdParty.add(GoImport.create("google.golang.org/grpc/codes"));
    thirdParty.add(GoImport.create("google.golang.org/grpc/metadata"));
    thirdParty.add(GoImport.create(GAX_PACKAGE_BASE, "gax"));

    thirdParty.addAll(getMessageImports(service));
    Set<GoImport> standard = getStandardImports(service);
    return formatImports(standard, thirdParty);
  }

  /**
   * Same as getImports, but scoped to the client_test.go file.
   */
  public Iterable<String> getTestImports(Interface service) {
    TreeSet<GoImport> thirdParty = new TreeSet<>();

    thirdParty.add(GoImport.create(getClientPackagePath(service)));
    thirdParty.add(GoImport.create("golang.org/x/net/context"));
    thirdParty.add(GoImport.create(GAX_PACKAGE_BASE, "gax"));
    thirdParty.addAll(getMessageImports(service));

    return formatImports(null, thirdParty);
  }

  /**
   * Returns true if the speficied type is the Empty type.
   */
  public boolean isEmpty(TypeRef type) {
    return type.isMessage() && type.getMessageType().getFullName().equals("google.protobuf.Empty");
  }

  /**
   * Returns true if the service has a page streaming method.
   */
  public boolean hasPageStreamingMethod(Interface service) {
    for (Method method : service.getMethods()) {
      MethodConfig methodConfig =
          getApiConfig().getInterfaceConfig(service).getMethodConfig(method);
      if (methodConfig.isPageStreaming()) {
        return true;
      }
    }
    return false;
  }
}

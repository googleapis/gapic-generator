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
package com.google.api.codegen.csharp;

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.CollectionConfig;
import com.google.api.codegen.FlatteningConfig;
import com.google.api.codegen.GapicContext;
import com.google.api.codegen.InterfaceConfig;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.PageStreamingConfig;
import com.google.api.codegen.ServiceConfig;
import com.google.api.gax.core.RetrySettings;
import com.google.api.gax.protobuf.PathTemplate;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import autovalue.shaded.com.google.common.common.collect.ImmutableList;
import autovalue.shaded.com.google.common.common.collect.Iterables;

import io.grpc.Status;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * A GapicContext specialized for C#.
 */
public class CSharpGapicContext extends GapicContext {

  /**
   * A map from primitive types in proto to C# counterparts.
   */
  private static final ImmutableMap<Type, String> PRIMITIVE_TYPE_MAP =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "bool")
          .put(Type.TYPE_DOUBLE, "double")
          .put(Type.TYPE_FLOAT, "float")
          .put(Type.TYPE_INT64, "long")
          .put(Type.TYPE_UINT64, "ulong")
          .put(Type.TYPE_SINT64, "long")
          .put(Type.TYPE_FIXED64, "ulong")
          .put(Type.TYPE_SFIXED64, "long")
          .put(Type.TYPE_INT32, "int")
          .put(Type.TYPE_UINT32, "uint")
          .put(Type.TYPE_SINT32, "int")
          .put(Type.TYPE_FIXED32, "uint")
          .put(Type.TYPE_SFIXED32, "int")
          .put(Type.TYPE_STRING, "string")
          .put(Type.TYPE_BYTES, "ByteString")
          .build();

  private CSharpContextCommon csharpCommon;

  /**
   * Constructs the C# language provider.
   */
  public CSharpGapicContext(Model model, ApiConfig config) {
    super(model, config);
  }

  public void resetState(CSharpContextCommon csharpCommon) {
    this.csharpCommon = csharpCommon;
  }

  // Snippet Helpers
  // ===============

  /**
   * Adds the given type name to the import list. Returns an empty string so that the output is not
   * affected.
   */
  public String addImport(String namespace) {
    return csharpCommon.addImport(namespace);
  }

  // This member function is necessary to provide access to snippets for
  // the functionality, since snippets can't call static functions.
  public String getNamespace(ProtoFile file) {
    return s_getNamespace(file);
  }

  /**
   * Gets the C# namespace for the given proto file.
   */
  // Code effectively copied from protoc, in csharp_helpers.cc, GetFileNamespace
  // This function is necessary to provide a static entry point for the same-named
  // member function.
  public static String s_getNamespace(ProtoFile file) {
    String optionsNamespace = file.getProto().getOptions().getCsharpNamespace();
    if (!Strings.isNullOrEmpty(optionsNamespace)) {
      return optionsNamespace;
    }
    return CSharpContextCommon.s_underscoresToCamelCase(file.getProto().getPackage(), true, true);
  }

  @AutoValue
  public static abstract class ServiceInfo {
    public static ServiceInfo create(String host, int port, Iterable<String> scopes) {
      return new AutoValue_CSharpGapicContext_ServiceInfo(host, port, scopes);
    }

    public abstract String host();

    public abstract int port();

    public abstract Iterable<String> scopes();
  }

  public ServiceInfo getServiceInfo(Interface service) {
    ServiceConfig serviceConfig = getServiceConfig();
    return ServiceInfo.create(
        serviceConfig.getServiceAddress(service),
        serviceConfig.getServicePort(),
        serviceConfig.getAuthScopes(service));
  }

  @AutoValue
  public static abstract class RetryDefInfo {
    public static RetryDefInfo create(
        String rawName, String name, String statusCodeUseList,
        boolean anyStatusCodes, Iterable<String> statusCodeNames) {
      return new AutoValue_CSharpGapicContext_RetryDefInfo(
        rawName, name, statusCodeUseList,
        anyStatusCodes, statusCodeNames);
    }
    public abstract String rawName();
    public abstract String name();
    public abstract String statusCodeUseList();
    public abstract boolean anyStatusCodes();
    public abstract Iterable<String> statusCodeNames();
  }

  @AutoValue
  public static abstract class RetrySettingInfo {
    public static RetrySettingInfo create(
        String rawName, String name,
        long delayMs, double delayMultiplier, long delayMaxMs,
        long timeoutMs, double timeoutMultiplier, long timeoutMaxMs,
        long totalTimeoutMs) {
      return new AutoValue_CSharpGapicContext_RetrySettingInfo(
          rawName, name,
          delayMs, delayMultiplier, delayMaxMs,
          timeoutMs, timeoutMultiplier, timeoutMaxMs,
          totalTimeoutMs);
    }
    public abstract String rawName();
    public abstract String name();
    public abstract long delayMs();
    public abstract double delayMultiplier();
    public abstract long delayMaxMs();
    public abstract long timeoutMs();
    public abstract double timeoutMultiplier();
    public abstract long timeoutMaxMs();
    public abstract long totalTimeoutMs();
  }

  @AutoValue
  public static abstract class RetryInfo {
    public static RetryInfo create(
        List<RetryDefInfo> defs, List<RetrySettingInfo> settings) {
      return new AutoValue_CSharpGapicContext_RetryInfo(
          defs, settings);
    }
    public abstract List<RetryDefInfo> defs();
    public abstract List<RetrySettingInfo> settings();
  }

  public RetryInfo getRetryInfo(Interface service) {
    final InterfaceConfig interfaceConfig = getApiConfig().getInterfaceConfig(service);
    List<RetryDefInfo> defs = FluentIterable.from(interfaceConfig.getRetryCodesDefinition().entrySet())
        .transform(new Function<Map.Entry<String, ImmutableSet<Status.Code>>, RetryDefInfo>() {
          @Override public RetryDefInfo apply(Map.Entry<String, ImmutableSet<Status.Code>> entry) {
            Iterable<String> statusCodeNames = FluentIterable.from(entry.getValue())
                .transform(new Function<Status.Code, String>() {
                  @Override public String apply(Status.Code statusCode) {
                    String statusCodeNameLower = statusCode.toString().toLowerCase();
                    return CSharpContextCommon.s_underscoresToPascalCase(statusCodeNameLower);
                  }
                });
            return RetryDefInfo.create(
                entry.getKey(),
                CSharpContextCommon.s_underscoresToPascalCase(entry.getKey()),
                Joiner.on(", ").join(CSharpContextCommon.s_prefix(statusCodeNames, "StatusCode.")),
                entry.getValue().size() > 0,
                statusCodeNames);
          }
        })
        .toList();
    List<RetrySettingInfo> settings = FluentIterable.from(interfaceConfig.getRetrySettingsDefinition().entrySet())
        .transform(new Function<Map.Entry<String, RetrySettings>, RetrySettingInfo>() {
          @Override public RetrySettingInfo apply(Map.Entry<String, RetrySettings> entry) {
            RetrySettings retrySettings = entry.getValue();
            return RetrySettingInfo.create(
                entry.getKey(),
                CSharpContextCommon.s_underscoresToPascalCase(entry.getKey()),
                retrySettings.getInitialRetryDelay().getMillis(),
                retrySettings.getRetryDelayMultiplier(),
                retrySettings.getMaxRetryDelay().getMillis(),
                retrySettings.getInitialRpcTimeout().getMillis(),
                retrySettings.getRpcTimeoutMultiplier(),
                retrySettings.getMaxRpcTimeout().getMillis(),
                retrySettings.getTotalTimeout().getMillis());
          }
        })
        .toList();
    return RetryInfo.create(defs, settings);
  }

  @AutoValue
  public static abstract class ParamInfo {
    public static ParamInfo create(
        String name, String typeName, String propertyName, boolean isRepeated) {
      return new AutoValue_CSharpGapicContext_ParamInfo(name, typeName, propertyName, isRepeated);
    }

    public abstract String name();

    public abstract String typeName();

    public abstract String propertyName();

    public abstract boolean isRepeated();
  }

  @AutoValue
  public static abstract class PageStreamerInfo {
    public static PageStreamerInfo create(
        String resourceTypeName,
        String requestTypeName,
        String responseTypeName,
        String tokenTypeName,
        String staticFieldName,
        String requestPageTokenFieldName,
        String responseNextPageTokenFieldName,
        String responseResourceFieldName,
        String emptyPageToken) {
      return new AutoValue_CSharpGapicContext_PageStreamerInfo(
          resourceTypeName,
          requestTypeName,
          responseTypeName,
          tokenTypeName,
          staticFieldName,
          requestPageTokenFieldName,
          responseNextPageTokenFieldName,
          responseResourceFieldName,
          emptyPageToken);
    }

    public abstract String resourceTypeName();

    public abstract String requestTypeName();

    public abstract String responseTypeName();

    public abstract String tokenTypeName();

    public abstract String staticFieldName();

    public abstract String requestPageTokenFieldName();

    public abstract String responseNextPageTokenFieldName();

    public abstract String responseResourceFieldName();

    public abstract String emptyPageToken();
  }

  @AutoValue
  public static abstract class FlatInfo {
    public static FlatInfo create(
      Iterable<ParamInfo> params,
      Iterable<String> xmlDocAsync,
      Iterable<String> xmlDocSync) {
      return new AutoValue_CSharpGapicContext_FlatInfo(
          params, xmlDocAsync, xmlDocSync);
    }
    public abstract Iterable<ParamInfo> params();
    public abstract Iterable<String> xmlDocAsync();
    public abstract Iterable<String> xmlDocSync();
  }

  private FlatInfo createFlatInfo(Method method, List<Field> flat) {
    List<ParamInfo> params = FluentIterable.from(flat)
        .transform(new Function<Field, ParamInfo>() {
          @Override public ParamInfo apply(Field field) {
            return ParamInfo.create(
                CSharpContextCommon.s_underscoresToCamelCase(field.getSimpleName()),
                typeName(field.getType()),
                CSharpContextCommon.s_underscoresToPascalCase(field.getSimpleName()),
                field.getType().isRepeated());
          }
        })
        .toList();
    return FlatInfo.create(
        params,
        makeMethodXmlDoc(method, flat, true),
        makeMethodXmlDoc(method, flat, false));
  }

  @AutoValue
  public static abstract class MethodInfo {
    public static MethodInfo create(
        String name,
        String asyncReturnTypeName,
        String syncReturnTypeName,
        boolean isPageStreaming,
        PageStreamerInfo pageStreaming,
        String requestTypeName,
        String responseTypeName,
        String syncReturnStatement,
        Iterable<FlatInfo> flats,
        RetryDefInfo retryCodes,
        RetrySettingInfo retryParams) {
      return new AutoValue_CSharpGapicContext_MethodInfo(
          name,
          asyncReturnTypeName,
          syncReturnTypeName,
          isPageStreaming,
          pageStreaming,
          requestTypeName,
          responseTypeName,
          syncReturnStatement,
          flats,
          retryCodes,
          retryParams);
    }

    public abstract String name();

    public abstract String asyncReturnTypeName();

    public abstract String syncReturnTypeName();

    public abstract boolean isPageStreaming();

    @Nullable
    public abstract PageStreamerInfo pageStreaming();

    public abstract String requestTypeName();

    public abstract String responseTypeName();

    public abstract String syncReturnStatement();

    public abstract Iterable<FlatInfo> flats();

    public abstract RetryDefInfo retryCodes();

    public abstract RetrySettingInfo retrySetting();
  }

  private MethodInfo createMethodInfo(
      InterfaceConfig interfaceConfig,
      final Method method,
      MethodConfig methodConfig,
      RetryDefInfo retryDef,
      RetrySettingInfo retrySetting) {
    PageStreamingConfig pageStreamingConfig = methodConfig.getPageStreaming();
    FlatteningConfig flattening = methodConfig.getFlattening();
    TypeRef returnType = method.getOutputType();
    boolean returnTypeEmpty = messages().isEmptyType(returnType);
    String asyncReturnTypeName;
    String syncReturnTypeName;
    if (returnTypeEmpty) {
      asyncReturnTypeName = "Task";
      syncReturnTypeName = "void";
    } else {
      if (pageStreamingConfig != null) {
        TypeRef resourceType = pageStreamingConfig.getResourcesField().getType();
        String elementTypeName = basicTypeName(resourceType);
        asyncReturnTypeName = "IAsyncEnumerable<" + elementTypeName + ">";
        syncReturnTypeName = "IEnumerable<" + elementTypeName + ">";
      } else {
        asyncReturnTypeName = "Task<" + typeName(returnType) + ">";
        syncReturnTypeName = typeName(returnType);
      }
    }
    Iterable<FlatInfo> flats = flattening != null ?
        FluentIterable.from(flattening.getFlatteningGroups())
            .transform(new Function<List<Field>, FlatInfo>() {
              @Override public FlatInfo apply(List<Field> flat) {
                return createFlatInfo(method, flat);
              }
            }) :
        Collections.<FlatInfo>emptyList();
    return MethodInfo.create(
        method.getSimpleName(),
        asyncReturnTypeName,
        syncReturnTypeName,
        pageStreamingConfig != null,
        getPageStreamerInfo(interfaceConfig, method),
        typeName(method.getInputType()),
        typeName(returnType),
        returnTypeEmpty ? "" : "return ",
        flats,
        retryDef,
        retrySetting);
  }

  public List<MethodInfo> getMethodInfos(Interface service) {
    final InterfaceConfig interfaceConfig = getApiConfig().getInterfaceConfig(service);
    RetryInfo retryInfo = getRetryInfo(service);
    final Map<String, RetryDefInfo> retryDefByName = Maps.uniqueIndex(retryInfo.defs(),
        new Function<RetryDefInfo, String>() {
          @Override public String apply(RetryDefInfo value) {
            return value.rawName();
          }
        });
    final Map<String, RetrySettingInfo> retrySettingByName = Maps.uniqueIndex(retryInfo.settings(),
        new Function<RetrySettingInfo, String>() {
          @Override public String apply(RetrySettingInfo value) {
            return value.rawName();
          }
        });
    return FluentIterable.from(service.getMethods())
        .transform(new Function<Method, MethodInfo>() {
          @Override public MethodInfo apply(Method method) {
            MethodConfig methodConfig = interfaceConfig.getMethodConfig(method);
            return createMethodInfo(interfaceConfig, method, methodConfig,
                retryDefByName.get(methodConfig.getRetryCodesConfigName()),
                retrySettingByName.get(methodConfig.getRetrySettingsConfigName()));
          }
        })
        .toList();
  }

  private PageStreamerInfo getPageStreamerInfo(InterfaceConfig interfaceConfig, Method method) {
    MethodConfig methodConfig = interfaceConfig.getMethodConfig(method);
    PageStreamingConfig pageStreamingConfig = methodConfig.getPageStreaming();
    if (pageStreamingConfig == null) {
      return null;
    }
    return PageStreamerInfo.create(
        basicTypeName(pageStreamingConfig.getResourcesField().getType()),
        typeName(method.getInputType()),
        typeName(method.getOutputType()),
        typeName(pageStreamingConfig.getRequestTokenField().getType()),
        "s_" + firstLetterToLower(method.getSimpleName()) + "PageStreamer",
        CSharpContextCommon.s_underscoresToPascalCase(pageStreamingConfig.getRequestTokenField().getSimpleName()),
        CSharpContextCommon.s_underscoresToPascalCase(pageStreamingConfig.getResponseTokenField().getSimpleName()),
        CSharpContextCommon.s_underscoresToPascalCase(pageStreamingConfig.getResourcesField().getSimpleName()),
        "\"\""); // TODO(chrisbacon): Support non-string page-tokens
  }

  public List<PageStreamerInfo> getPageStreamerInfos(Interface service) {
    final InterfaceConfig interfaceConfig = getApiConfig().getInterfaceConfig(service);
    return FluentIterable.from(service.getMethods())
        .transform(new Function<Method, PageStreamerInfo>() {
          @Override public PageStreamerInfo apply(Method method) {
            return getPageStreamerInfo(interfaceConfig, method);
          }
        })
        .filter(Predicates.notNull())
        .toList();
  }

  @AutoValue
  public static abstract class PathTemplateInfo {
    public static PathTemplateInfo create(
        String baseName,
        String docName,
        String namePattern,
        Iterable<String> vars,
        String varArgDeclList,
        String varArgUseList) {
      return new AutoValue_CSharpGapicContext_PathTemplateInfo(
          baseName, docName, namePattern, vars, varArgDeclList, varArgUseList);
    }

    public abstract String baseName();

    public abstract String docName();

    public abstract String namePattern();

    public abstract Iterable<String> vars();

    public abstract String varArgDeclList();

    public abstract String varArgUseList();
  }

  public List<PathTemplateInfo> getPathTemplateInfos(Interface service) {
    InterfaceConfig interfaceConfig = getApiConfig().getInterfaceConfig(service);
    return FluentIterable.from(interfaceConfig.getCollectionConfigs())
        .transform(new Function<CollectionConfig, PathTemplateInfo>() {
          @Override public PathTemplateInfo apply(CollectionConfig collection) {
            PathTemplate template = collection.getNameTemplate();
            Set<String> vars = template.vars();
            StringBuilder varArgDeclList = new StringBuilder();
            StringBuilder varArgUseList = new StringBuilder();
            for (String var : vars) {
              varArgDeclList.append("string " + var + "Id, ");
              varArgUseList.append(var + "Id, ");
            }
            return PathTemplateInfo.create(
                CSharpContextCommon.s_underscoresToPascalCase(collection.getEntityName()),
                CSharpContextCommon.s_underscoresToCamelCase(collection.getEntityName()),
                collection.getNamePattern(),
                vars,
                varArgDeclList.substring(0, varArgDeclList.length() - 2),
                varArgUseList.substring(0, varArgUseList.length() - 2));
          }
        })
        .toList();
  }

  /**
   * Returns the C# representation of a reference to a type.
   */
  private String typeName(TypeRef type) {
    if (type.isMap()) {
      TypeRef keyType = type.getMapKeyField().getType();
      TypeRef valueType = type.getMapValueField().getType();
      return "IDictionary<" + typeName(keyType) + ", " + typeName(valueType) + ">";
    }
    // Must check for map first, as a map is also repeated
    if (type.isRepeated()) {
      return String.format("IEnumerable<%s>", basicTypeName(type));
    }
    return basicTypeName(type);
  }

  /**
   * Returns the C# representation of a type, without cardinality.
   */
  private String basicTypeName(TypeRef type) {
    String result = PRIMITIVE_TYPE_MAP.get(type.getKind());
    if (result != null) {
      if (type.getKind() == Type.TYPE_BYTES) {
        // Special handling of ByteString.
        // It requires a 'using' directive, unlike all other primitive types.
        addImport("Google.Protobuf");
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
   * Gets the full name of the message or enum type in C#.
   */
  private String getTypeName(ProtoElement elem) {
    // TODO: Handle naming collisions. This will probably require
    // using alias directives, which will be awkward...

    // Handle nested types, construct the required type prefix
    ProtoElement parentEl = elem.getParent();
    String prefix = "";
    while (parentEl != null && parentEl instanceof MessageType) {
      prefix = parentEl.getSimpleName() + ".Types." + prefix;
      parentEl = parentEl.getParent();
    }
    // Add an import for the type, if not already imported
    addImport(getNamespace(elem.getFile()));
    // Return the combined type prefix and type name
    return prefix + elem.getSimpleName();
  }

  private List<String> docLines(ProtoElement element, final String prefix) {
    FluentIterable<String> lines = FluentIterable.from(
        Splitter.on(String.format("%n")).split(DocumentationUtil.getDescription(element))
    );
    return lines
        .transform(new Function<String, String>() {
          @Override public String apply(String line) {
            return prefix + line.replace("&", "&amp;").replace("<", "&lt;");
          }
        })
        .toList();
  }

  private List<String> makeMethodXmlDoc(Method method, List<Field> params, boolean isAsync) {
    Iterable<String> parameters = FluentIterable.from(params)
        .transformAndConcat(new Function<Field, Iterable<String>>() {
          @Override public Iterable<String> apply(Field param) {
            String header = "/// <param name=\"" + param.getSimpleName() + "\">";
            List<String> lines = docLines(param, "");
            if (lines.size() > 1) {
              return ImmutableList.<String>builder()
                  .add(header)
                  .addAll(FluentIterable.from(lines).transform(
                      new Function<String, String>() {
                        @Override public String apply(String line) {
                          return "/// " + line;
                        }
                      }))
                  .add("/// </param>")
                  .build();
            } else {
              return Collections.singletonList(header + lines.get(0) + "</param>");
            }
          }
        });
    return ImmutableList.<String>builder()
        .add("/// <summary>")
        .addAll(docLines(method, "/// "))
        .add("/// </summary>")
        .addAll(parameters)
        .build();
  }

  private String firstLetterToLower(String input) {
    if (input != null && input.length() >= 1) {
      return input.substring(0, 1).toLowerCase(Locale.ENGLISH) + input.substring(1);
    } else {
      return input;
    }
  }

  public String prependComma(String text) {
    return text.isEmpty() ? "" : ", " + text;
  }
}

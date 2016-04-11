/* Copyright 2016 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http: *www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gapi.vgen.csharp;

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
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.snippet.SnippetSet;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import autovalue.shaded.com.google.common.common.collect.ImmutableList;
import io.gapi.vgen.ApiConfig;
import io.gapi.vgen.FlatteningConfig;
import io.gapi.vgen.GeneratedResult;
import io.gapi.vgen.InterfaceConfig;
import io.gapi.vgen.LanguageProvider;
import io.gapi.vgen.MethodConfig;
import io.gapi.vgen.PageStreamingConfig;
import io.gapi.vgen.ServiceConfig;
import io.gapi.vgen.SnippetDescriptor;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;

/**
 * Language provider for C# codegen.
 */
public class CSharpLanguageProvider extends LanguageProvider {
  /**
   * Entry points for the snippet set. Generation is partitioned into a first phase
   * which generates the content of the class without namespace and using directives,
   * and a second phase which completes the class based on the knowledge of which
   * other classes have been imported.
   */
  interface CSharpSnippetSet {
    /**
     * Generates the result filename for the generated document
     */
    Doc generateFilename(Interface iface);

    /**
     * Generates the body of the class for the service interface.
     */
    Doc generateBody(Interface iface);

    /**
     * Generates the result class, based on the result for the body,
     * and a set of accumulated types to be imported.
     */
    Doc generateClass(Interface iface, Doc body, Iterable<String> imports);
  }

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

  /**
   * The path to the root of snippet resources.
   */
  private static final String SNIPPET_RESOURCE_ROOT =
      CSharpLanguageProvider.class.getPackage().getName().replace('.', '/');

  /**
   * The set of namespaces which need to be imported.
   */
  // TODO(jonskeet): Handle naming collisions.
  private final TreeSet<String> imports = new TreeSet<>();

  /**
   * Constructs the C# language provider.
   */
  public CSharpLanguageProvider(Model model, ApiConfig config) {
    super(model, config);
  }

  @Override
  public void outputCode(String outputArchiveFile, List<GeneratedResult> results,
      boolean archive) throws IOException {
    Map<String, Doc> files = new LinkedHashMap<>();
    for (GeneratedResult result : results) {
      String path = getApiConfig().getPackageName().replace('.', '/');
      files.put(path + "/" + result.getFilename(), result.getDoc());
    }
    if (archive) {
      ToolUtil.writeJar(files, outputArchiveFile);
    } else {
      ToolUtil.writeFiles(files, outputArchiveFile);
    }
  }

  @Override
  public GeneratedResult generate(Interface service, SnippetDescriptor snippetDescriptor) {
    CSharpSnippetSet snippets = SnippetSet.createSnippetInterface(CSharpSnippetSet.class,
        SNIPPET_RESOURCE_ROOT, snippetDescriptor.getSnippetInputName(),
        ImmutableMap.<String, Object>of("context", this));

    Doc filenameDoc = snippets.generateFilename(service);
    String outputFilename = filenameDoc.prettyPrint();

    // Generate the body, which will collect the imports.
    // Note that imports is a field of the language provider, which is
    // populated during generateBody.
    imports.clear();
    Doc body = snippets.generateBody(service);

    imports.remove(getNamespace(service.getFile()));

    // Generate result.
    Doc result = snippets.generateClass(service, body, imports);
    return GeneratedResult.create(result, outputFilename);
  }

  /**
   * Adds the given type name to the import list. Returns an empty string so that the
   * output is not affected.
   */
  public String addImport(String namespace) {
    imports.add(namespace);
    return "";
  }

  // Snippet Helpers
  // ===============

  /**
   * Gets the C# namespace for the given proto file.
   */
  // Code effectively copied from protoc, in csharp_helpers.cc, GetFileNamespace
  public String getNamespace(ProtoFile file) {
    String optionsNamespace = file.getProto().getOptions().getCsharpNamespace();
    if (!Strings.isNullOrEmpty(optionsNamespace)) {
      return optionsNamespace;
    }
    return underscoresToCamelCase(file.getProto().getPackage(), true, true);
  }

  @AutoValue
  public static abstract class ServiceInfo {
    public static ServiceInfo create(
        String host,
        int port,
        Iterable<String> scopes) {
      return new AutoValue_CSharpLanguageProvider_ServiceInfo(
          host, port, scopes);
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
  public static abstract class ParamInfo {
    public static ParamInfo create(
        String name,
        String typeName,
        String propertyName,
        boolean isRepeated) {
      return new AutoValue_CSharpLanguageProvider_ParamInfo(
          name, typeName, propertyName, isRepeated);
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
      return new AutoValue_CSharpLanguageProvider_PageStreamerInfo(
          resourceTypeName, requestTypeName, responseTypeName, tokenTypeName,
          staticFieldName, requestPageTokenFieldName, responseNextPageTokenFieldName,
          responseResourceFieldName, emptyPageToken);
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
  public static abstract class MethodInfo {
    public static MethodInfo create(
        String name,
        String asyncReturnTypeName,
        String syncReturnTypeName,
        Iterable<ParamInfo> params,
        boolean isPageStreaming,
        PageStreamerInfo pageStreaming,
        String requestTypeName,
        String syncReturnStatement,
        Iterable<String> xmlDocAsync,
        Iterable<String> xmlDocSync) {
      return new AutoValue_CSharpLanguageProvider_MethodInfo(
          name, asyncReturnTypeName, syncReturnTypeName, params, isPageStreaming,
          pageStreaming, requestTypeName, syncReturnStatement,
          xmlDocAsync, xmlDocSync);
    }
    public abstract String name();
    public abstract String asyncReturnTypeName();
    public abstract String syncReturnTypeName();
    public abstract Iterable<ParamInfo> params();
    public abstract boolean isPageStreaming();
    @Nullable public abstract PageStreamerInfo pageStreaming();
    public abstract String requestTypeName();
    public abstract String syncReturnStatement();
    public abstract Iterable<String> xmlDocAsync();
    public abstract Iterable<String> xmlDocSync();
  }

  private MethodInfo createMethodInfo(InterfaceConfig interfaceConfig, Method method,
      List<Field> flattening, PageStreamingConfig pageStreamingConfig) {
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
    Stream<ParamInfo> params = flattening.stream().map(field ->
      ParamInfo.create(
          lowerUnderscoreToLowerCamel(field.getSimpleName()),
          typeName(field.getType()),
          underscoresToCamelCase(field.getSimpleName(), true, false),
          field.getType().isRepeated())
    );
    return MethodInfo.create(
        method.getSimpleName(),
        asyncReturnTypeName, syncReturnTypeName,
        params.collect(Collectors.toList()),
        pageStreamingConfig != null,
        getPageStreamerInfo(interfaceConfig, method),
        typeName(method.getInputType()),
        returnTypeEmpty ? "" : "return ",
        makeMethodXmlDoc(method, flattening, true),
        makeMethodXmlDoc(method, flattening, false));
  }

  public List<MethodInfo> getMethodInfos(Interface service) {
    // FlatteningConfig is just a List<Field>
    InterfaceConfig interfaceConfig = getApiConfig().getInterfaceConfig(service);
    return service.getMethods().stream().flatMap(method -> {
      MethodConfig methodConfig = interfaceConfig.getMethodConfig(method);
      PageStreamingConfig pageStreamingConfig = methodConfig.getPageStreaming();
      FlatteningConfig flatConfig = methodConfig.getFlattening();
      if (flatConfig != null) {
        return flatConfig.getFlatteningGroups().stream().map(
            flattening -> createMethodInfo(interfaceConfig, method, flattening, pageStreamingConfig));
      } else {
        return Stream.of();
      }
    }).collect(Collectors.toList());
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
        underscoresToCamelCase(pageStreamingConfig.getRequestTokenField().getSimpleName(), true, false),
        underscoresToCamelCase(pageStreamingConfig.getResponseTokenField().getSimpleName(), true, false),
        underscoresToCamelCase(pageStreamingConfig.getResourcesField().getSimpleName(), true, false),
        "\"\""); // TODO(chrisbacon): Support non-string page-tokens
  }

  public List<PageStreamerInfo> getPageStreamerInfos(Interface service) {
    InterfaceConfig interfaceConfig = getApiConfig().getInterfaceConfig(service);
    return service.getMethods().stream()
        .map(method -> getPageStreamerInfo(interfaceConfig, method))
        .filter(pageStreamerInfo -> pageStreamerInfo != null)
        .collect(Collectors.toList());
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
      return new AutoValue_CSharpLanguageProvider_PathTemplateInfo(
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
    return interfaceConfig.getCollectionConfigs().stream()
        .map(collection -> {
          PathTemplate template = collection.getNameTemplate();
          Set<String> vars = template.vars();
          return PathTemplateInfo.create(
              underscoresToCamelCase(collection.getMethodBaseName(), true, false),
              underscoresToCamelCase(collection.getMethodBaseName(), false, false),
              collection.getNamePattern(),
              vars,
              vars.stream()
                  .map(var -> "string " + var + "Id")
                  .reduce((a, b) -> a + ", " + b)
                  .get(),
              vars.stream()
                  .map(var -> var + "Id")
                  .reduce((a, b) -> a + ", " + b)
                  .get());
        })
        .collect(Collectors.toList());
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

  private List<String> docLines(ProtoElement element, String prefix) {
    Iterable<String> lines = Splitter.on(String.format("%n"))
        .split(DocumentationUtil.getDescription(element));
    return StreamSupport.stream(lines.spliterator(), false)
        .map(line -> prefix + line.replace("&", "&amp;").replace("<", "&lt;"))
        .collect(Collectors.toList());
  }

  private List<String> makeMethodXmlDoc(Method method, List<Field> params, boolean isAsync) {
    List<String> parameters = params.stream()
        .flatMap(param -> {
            String header = "/// <param name=\"" + param.getSimpleName() + "\">";
            List<String> lines = docLines(param, "");
            if (lines.size() > 1) {
              return ImmutableList.<String>builder()
                  .add(header)
                  .addAll(lines.stream().map(line -> "/// " + line).collect(Collectors.toList()))
                  .add("/// </param>")
                  .build().stream();
            } else {
              return Stream.of(header + lines.get(0) + "</param>");
            }
        })
        .collect(Collectors.toList());
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

  // Copied from csharp_helpers.cc and converted into Java.
  // The existing lowerUnderscoreToUpperCamel etc don't handle dots in the way we want.
  // TODO: investigate that and add more common methods if necessary.
  private String underscoresToCamelCase(
      String input, boolean capNextLetter, boolean preservePeriod) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if ('a' <= c && c <= 'z') {
        result.append(capNextLetter ? Character.toUpperCase(c) : c);
        capNextLetter = false;
      } else if ('A' <= c && c <= 'Z') {
        if (i == 0 && !capNextLetter) {
          // Force first letter to lower-case unless explicitly told to
          // capitalize it.
          result.append(Character.toUpperCase(c));
        } else {
          // Capital letters after the first are left as-is.
          result.append(c);
        }
        capNextLetter = false;
      } else if ('0' <= c && c <= '9') {
        result.append(c);
        capNextLetter = true;
      } else {
        capNextLetter = true;
        if (c == '.' && preservePeriod) {
          result.append('.');
        }
      }
    }
    // Add a trailing "_" if the name should be altered.
    if (input.endsWith("#")) {
      result.append('_');
    }
    return result.toString();
  }
}

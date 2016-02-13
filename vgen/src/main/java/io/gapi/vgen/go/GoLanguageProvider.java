package io.gapi.vgen.go;

import com.google.api.Documentation;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.aspects.documentation.model.ElementDocumentationAttribute;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.TypeRef;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.snippet.SnippetSet;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import io.gapi.vgen.ApiConfig;
import io.gapi.vgen.GeneratedResult;
import io.gapi.vgen.LanguageProvider;
import io.gapi.vgen.MethodConfig;
import io.gapi.vgen.SnippetDescriptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Language provider for Go codegen.
 */
public class GoLanguageProvider extends LanguageProvider {

  /**
   * Entry points for the snippet set. Generation is partitioned into a first phase
   * which generates the content of the class without package and imports header,
   * and a second phase which completes the class based on the knowledge of which
   * other classes have been imported.
   */
  interface GoSnippetSet {

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
    Doc generateClass(Interface iface, Doc body);
  }

  /**
   * Class to represent one Go import.
   */
  @AutoValue
  abstract static class GoImport implements Comparable<GoImport> {

    public abstract String moduleName();

    public abstract String localName();

    /**
     * Creates a Go import with the given module name with the local name.
     */
    public static GoImport create(String moduleName, String localName) {
      return new AutoValue_GoLanguageProvider_GoImport(moduleName, localName);
    }

    /**
     * Creates a Go import with then given module name.
     */
    public static GoImport create(String moduleName) {
      return create(moduleName, "");
    }

    /**
     * Returns a line of import declaration used in the generated Go files.
     */
    public String importString() {
      if (Strings.isNullOrEmpty(localName())) {
        return "\"" + moduleName() + "\"";
      } else {
        return localName() + " \"" + moduleName() + "\"";
      }
    }

    @Override
    public int compareTo(GoImport other) {
      return moduleName().compareTo(other.moduleName());
    }
  }

  /**
   * The path to the root of snippet resources.
   */
  private static final String SNIPPET_RESOURCE_ROOT =
      GoLanguageProvider.class.getPackage().getName().replace('.', '/');

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
   * TODO(mukai): Change this address when it's decided to merge into gcloud-golang.
   */
  private static final String GAX_PACKAGE_BASE = "github.com/googleapis/gax-go";

  /**
   * The import path for generated pb.go files for core-proto files.
   *
   * Right now this and CORE_PROTO_PACKAGES are not used, assumed they are placed
   * inside of this package, which is recommended by gcloud-golang team.
   *
   * TODO(mukai): Set the proper location when the location is known.
   */
  private static final String CORE_PROTO_BASE = "<TBD>";

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

  /**
   * Constructs the Go language provider.
   */
  public GoLanguageProvider(Model model, ApiConfig apiConfig) {
    super(model, apiConfig);
  }

  @Override
  public void outputCode(String outputArchiveFile, Multimap<Interface, GeneratedResult> services,
      boolean archive)
          throws IOException {
    Map<String, Doc> files = new LinkedHashMap<>();
    for (Map.Entry<Interface, GeneratedResult> serviceEntry : services.entries()) {
      Interface service = serviceEntry.getKey();
      GeneratedResult generatedResult = serviceEntry.getValue();
      files.put(generatedResult.getFilename(), generatedResult.getDoc());
    }
    ToolUtil.writeFiles(files, outputArchiveFile);
  }

  @Override
  public GeneratedResult generate(Interface service, SnippetDescriptor snippetDescriptor) {
    GoSnippetSet snippets = SnippetSet.createSnippetInterface(
        GoSnippetSet.class,
        SNIPPET_RESOURCE_ROOT,
        snippetDescriptor.getSnippetInputName(),
        ImmutableMap.<String, Object>of("context", this));

    Doc filenameDoc = snippets.generateFilename(service);
    String outputFilename = filenameDoc.prettyPrint();

    Doc body = snippets.generateBody(service);

    // Generate result.
    Doc result = snippets.generateClass(service, body);
    return GeneratedResult.create(result, outputFilename);
  }

  /**
   * Returns the package name symbol used for the package declaration.
   */
  public String getPackageName() {
    String fullPackageName = getApiConfig().getPackageName();
    int lastSlash = fullPackageName.lastIndexOf('/');
    if (lastSlash < 0) {
      return fullPackageName;
    }
    return fullPackageName.substring(lastSlash + 1);
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
    if (type.isMessage()) {
      MessageType messageType = type.getMessageType();
      return "*" + localPackageName(messageType) + "." + messageType.getProto().getName();
    } else if (type.isPrimitive()) {
      return PRIMITIVE_TYPE_MAP.get(type.getKind());
    } else {
      return "";
    }
  }

  /**
   * Returns the Go type name for the PageStreamable implementation of the method.
   */
  public String getStreamableTypeName(Method method) {
    return upperCamelToLowerCamel(method.getSimpleName()) + "Streamable";
  }

  /**
   * Returns the zero-value representation for the specified type in Go.
   * See: https://golang.org/ref/spec#The_zero_value
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
   * Returns the Go type name of (gRPC's) response-streaming methods which receives
   * the element objects.
   */
  public String getStreamReceiver(Interface service, Method method) {
    return localPackageName(service) + "." + service.getSimpleName() + "_" +
        method.getSimpleName() + "Client";
  }

  /**
   * Returns the doc comments of Go for the proto elements.
   */
  public Iterable<String> comments(ProtoElement element) {
    if (!element.hasAttribute(ElementDocumentationAttribute.KEY)) {
      return ImmutableList.<String>of("");
    }
    return getCommentLines(DocumentationUtil.getScopedDescription(element));
  }

  /**
   * Returns the package doc comments for the entire config.
   */
  public Iterable<String> getPackageDocument(Interface service) {
    Documentation doc = service.getModel().getServiceConfig().getDocumentation();
    return getCommentLines(doc.getSummary());
  }

  /**
   * Converts the specified text into a comment block in the generated Go file.
   */
  public Iterable<String> getCommentLines(String text) {
    List<String> result = new ArrayList<>();
    for (String line : Splitter.on(String.format("%n")).split(text)) {
      result.add(line.isEmpty() ? "//" : "// " + line);
    }
    return result;
  }

  /**
   * Creates a Go import from the message import.
   */
  private GoImport createMessageImport(MessageType messageType) {
    String pkgName = messageType.getFile().getProto().getPackage().replace(".", "/");
    String localName = localPackageName(messageType);
    return GoImport.create(getApiConfig().getPackageName() + "/proto/" + pkgName, localName);
  }

  /**
   * Calculates the set of imports and returns a sorted set of Go import output strings.
   * This imitates the same order which gofmt does, which means:
   *  - core imports (Go standard libraries) in alphabetical order
   *  - a blank line (so an empty string)
   *  - other imports, alphabetical order
   */
  public Iterable<String> getImports(Interface service) {
    TreeSet<GoImport> coreImports = new TreeSet<>();
    TreeSet<GoImport> imports = new TreeSet<>();

    // Add non-service-specific imports.
    imports.add(GoImport.create("golang.org/x/net/context"));
    imports.add(GoImport.create("google.golang.org/grpc"));
    imports.add(GoImport.create("google.golang.org/cloud"));
    imports.add(GoImport.create("google.golang.org/cloud/internal/transport"));
    imports.add(GoImport.create("github.com/googleapis/gax-go", "gax"));

    // Add method request-type imports
    for (Method method : service.getMethods()) {
      MessageType inputMessage = method.getInputMessage();
      MessageType outputMessage = method.getOutputMessage();
      MethodConfig methodConfig =
          getApiConfig().getInterfaceConfig(service).getMethodConfig(method);
      imports.add(createMessageImport(inputMessage));
      imports.add(createMessageImport(outputMessage));
      if (methodConfig.isPageStreaming()) {
        TypeRef resourceType =
            methodConfig.getPageStreaming().getResourcesField().getType();
        if (resourceType.isMessage()) {
          imports.add(createMessageImport(resourceType.getMessageType()));
        }
        coreImports.add(GoImport.create("io"));
      }
    }

    List<String> result = new ArrayList<>();
    for (GoImport goImport : coreImports) {
      result.add(goImport.importString());
    }
    // An empty string to bring the blank line between core imports and others.
    result.add("");
    for (GoImport goImport : imports) {
      result.add(goImport.importString());
    }
    return result;
  }
}

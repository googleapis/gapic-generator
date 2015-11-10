package io.gapi.vgen.py;

import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.aspects.documentation.model.ElementDocumentationAttribute;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.TypeRef;
import com.google.api.tools.framework.model.TypeRef.Cardinality;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.snippet.SnippetSet;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import io.gapi.vgen.ApiConfig;
import io.gapi.vgen.GeneratedResult;
import io.gapi.vgen.LanguageProvider;
import io.gapi.vgen.SnippetDescriptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Language provider for Python codegen.
 */
public class PythonLanguageProvider extends LanguageProvider {

  /**
   * Entry points for the snippet set. Generation is partitioned into a first phase
   * which generates the content of the class without package and imports header,
   * and a second phase which completes the class based on the knowledge of which
   * other classes have been imported.
   */
  interface PythonSnippetSet {

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

  /*
   * Class to represent one python import.
   */
  @AutoValue
  abstract static class PythonImport {

    public abstract String moduleName();

    public abstract String attributeName();

    public abstract String localName();

    /*
     * Create a Python import with the given module, attribute and local names.
     */
    public static PythonImport create(String moduleName, String attributeName, String localName) {
      return new AutoValue_PythonLanguageProvider_PythonImport(
          moduleName, attributeName, localName);
    }

    /*
     * Create a Python import with then given module and attribute names.
     */
    public static PythonImport create(String moduleName, String attributeName) {
      return create(moduleName, attributeName, "");
    }

    /*
     * Create a Python import with the given attribute name.
     */
    public static PythonImport create(String attributeName) {
      return create("", attributeName, "");
    }

    public String importString() {
      return (Strings.isNullOrEmpty(moduleName()) ? "" : "from " + moduleName() + " ") + "import "
          + attributeName() + (Strings.isNullOrEmpty(localName()) ? "" : "as " + localName());
    }
  }

  /**
   * A map from primitive types to its default value.
   */
  private static final ImmutableMap<Type, String> DEFAULT_VALUE_MAP =
      ImmutableMap.<Type, String>builder()
      .put(Type.TYPE_BOOL, "False")
      .put(Type.TYPE_DOUBLE, "0.0J")
      .put(Type.TYPE_FLOAT, "0.0J")
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
      .put(Type.TYPE_STRING, "\"\"")
      .put(Type.TYPE_BYTES, "\"\"")
      .build();

  /**
   * The path to the root of snippet resources.
   */
  private static final String SNIPPET_RESOURCE_ROOT =
      PythonLanguageProvider.class.getPackage().getName().replace('.', '/');

  /**
   * A bi-map from full names to short names indicating the import map.
   */
  private final BiMap<String, PythonImport> imports = HashBiMap.create();

  @Override
  public void outputCode(String outputArchiveFile, Multimap<Interface, GeneratedResult> services,
      boolean archive)
      throws IOException {
    Map<String, Doc> files = new LinkedHashMap<>();
    for (Map.Entry<Interface, GeneratedResult> serviceEntry : services.entries()) {
      Interface service = serviceEntry.getKey();
      GeneratedResult generatedResult = serviceEntry.getValue();
      String path = service.getFile().getFullName().replace('.', '/');
      files.put(path + "/" + generatedResult.getFilename(), generatedResult.getDoc());
    }
    if (archive) {
      ToolUtil.writeJar(files, outputArchiveFile);
    } else {
      ToolUtil.writeFiles(files, outputArchiveFile);
    }
  }

  /**
   * Constructs the Python language provider.
   */
  public PythonLanguageProvider(Model model, ApiConfig apiConfig) {
    super(model, apiConfig);
  }

  @Override
  public GeneratedResult generate(Interface service, SnippetDescriptor snippetDescriptor) {
    PythonSnippetSet snippets = SnippetSet.createSnippetInterface(
        PythonSnippetSet.class,
        SNIPPET_RESOURCE_ROOT,
        snippetDescriptor.getSnippetInputName(),
        ImmutableMap.<String, Object>of("context", this));

    Doc filenameDoc = snippets.generateFilename(service);
    String outputFilename = filenameDoc.prettyPrint();

    List<String> importList = calculateImports(service);
    Doc body = snippets.generateBody(service);

    // Generate result.
    Doc result = snippets.generateClass(service, body, importList);
    return GeneratedResult.create(result, outputFilename);
  }

  /**
   * Return canonical oauth scopes of the given service.
   * @param service
   */
  public Set<String> getOauthScopes(Interface service) {
    Set<String> scopes = Sets.newHashSet();
    // TODO(cbao): Add oauth scopes of methods inside the service once AuthAttribute gets included
    // in core.
    return ImmutableSortedSet.copyOf(scopes);
  }

  /**
   * Return comments lines of the proto elements. Those lines will be printed out in the generated
   * proto.
   */
  public List<String> comments(ProtoElement element) {
    if (!element.hasAttribute(ElementDocumentationAttribute.KEY)) {
      return ImmutableList.<String>of("");
    }
    return convertToCommentedBlock(DocumentationUtil.getScopedDescription(element));
  }

  /**
   * Return name of service pb file name of the given service.
   */
  public String getServicePbFileName(Interface service) {
    // FileDescriptorProto.name returns file name, relative to root of the source tree. Return the
    // last segment of the file name without proto extension appended by "_pb2".
    String filename = service.getFile().getProto().getName().substring(
        service.getFile().getProto().getName().lastIndexOf("/") + 1);
    return filename.substring(0, filename.length() - ".proto".length()) + "_pb2";
  }

  /**
   * Return the default value for the given field. Return null if there is no default value.
   */
  public String defaultValue(Field field) {
    TypeRef type = field.getType();
    // Return empty array if the type is repeated.
    if (type.getCardinality() == Cardinality.REPEATED) {
      return "[]";
    }

    switch (type.getKind()) {
      case TYPE_MESSAGE:
        return "None";
      case TYPE_ENUM:
        Preconditions.checkArgument(type.getEnumType().getValues().size() > 0);
        return type.getEnumType().getSimpleName() + "."
            + type.getEnumType().getValues().get(0).getSimpleName();
      default:
        if (type.isPrimitive()) {
          return DEFAULT_VALUE_MAP.get(type.getKind());
        }
        throw new IllegalArgumentException("unknown type kind: " + type.getKind());
    }
  }

  /*
   * Convert the content string into a commented block that can be directly printed out in the
   * generated py files.
   */
  private List<String> convertToCommentedBlock(String content) {
    if (Strings.isNullOrEmpty(content)) {
      return ImmutableList.<String>of();
    }
    List<String> comments = Splitter.on("\n").splitToList(content);
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    if (comments.size() > 1) {
      builder.add("\"\"\"");
      for (String comment : comments) {
        builder.add(comment);
      }
      builder.add("\"\"\"");
    } else {
      // one-line comment.
      builder.add("\"\"\"" + content + "\"\"\"");
    }
    return builder.build();
  }

  /*
   * Calculate the imports map and return a sorted set of python import output strings.
   */
  private List<String> calculateImports(Interface service) {
    imports.clear();

    // Add non-service-specific imports.
    imports.put("grpc.beta.implementations", PythonImport.create("grpc.beta", "implementations"));
    imports.put("oauth2client.client", PythonImport.create("oauth2client", "client", "auth_client"));

    // Add service-specific imports.
    imports.put(
        service.getFile().getProto().getName(),
        PythonImport.create(service.getFile().getProto().getPackage(), // package name
            getServicePbFileName(service)));

    // Generate a sorted list with import strings.
    List<String> result = new ArrayList<>();
    for (PythonImport protoImport : imports.values()) {
      result.add(protoImport.importString());
    }
    Collections.sort(result);
    return result;
  }
}


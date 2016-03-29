package io.gapi.vgen.csharp;

import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.api.tools.framework.model.TypeRef.Cardinality;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.snippet.SnippetSet;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import io.gapi.vgen.ApiConfig;
import io.gapi.vgen.GeneratedResult;
import io.gapi.vgen.LanguageProvider;
import io.gapi.vgen.SnippetDescriptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

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

  /**
   * Gets the name of the class which provides extension methods for this service interface.
   */
  public String getExtensionsClassName(Interface service) {
    return service.getSimpleName() + "Extensions";
  }

  /**
   * Gets the name of the class which provides factory methods for this service interface.
   */
  public String getFactoryClassName(Interface service) {
    return service.getSimpleName() + "Factory";
  }

  /**
   * Gets the name of the class which is the grpc container for this service interface.
   */
  public String getGrpcName(Interface service) {
    return service.getSimpleName();
  }

  /**
   * Gets the name of the client interface for this service.
   */
  public String getClientName(Interface service) {
    return getGrpcName(service) + ".I" + service.getSimpleName() + "Client";
  }

  /**
   * Gets the name of the class which implements this service.
   */
  public String getClientImplementationName(Interface service) {
    return getGrpcName(service) + "." + service.getSimpleName() + "Client";
  }

  /**
   * Given a TypeRef, returns the return statement for that type. Specifically, this will
   * return an empty string for the empty type (we don't want a return statement for void).
   */
  public String methodReturnStatement(TypeRef type) {
    return messages().isEmptyType(type) ? "" : "return ";
  }

  /**
   * Given a TypeRef, returns the String form of the type to be used as a return value.
   * Special case: this will return "void" for the Empty return type.
   */
  public String methodReturnTypeName(TypeRef type) {
    return messages().isEmptyType(type) ? "void" : typeName(type);
  }

  /**
   * Given a TypeRef, returns the String form of the type to be used as a return value from
   * an async method.
   * Special case: this will return "Task" for the Empty return type.
   */
  public String asyncMethodReturnTypeName(TypeRef type) {
    return messages().isEmptyType(type) ? "Task" : "Task<" + typeName(type) + ">";
  }

  /**
   * Returns the C# representation of a reference to a type.
   */
  public String typeName(TypeRef type) {
    // TODO(jonskeet): Maps! (Use IDictionary<...>)
    if (type.getCardinality() == Cardinality.REPEATED) {
      return String.format("IEnumerable<%s>", basicTypeName(type));
    }
    return basicTypeName(type);
  }

  /**
   * Returns the C# representation of a type, without cardinality.
   */
  public String basicTypeName(TypeRef type) {
    String result = PRIMITIVE_TYPE_MAP.get(type.getKind());
    if (result != null) {
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
  public String getTypeName(ProtoElement elem) {
    // TODO: Handle nested types, and naming collisions. (The latter will probably require
    // using alias directives, which will be awkward...)
    addImport(getNamespace(elem.getFile()));
    return elem.getSimpleName();
  }

  /**
   * Returns the description of the proto element, in markdown format.
   */
  public String getDescription(ProtoElement element) {
    return DocumentationUtil.getDescription(element);
  }

  /**
   * Splits given text into lines and returns an iterable of strings each one representing a
   * line decorated for an XML documentation comment, wrapped in the given element
   */
  public Iterable<String> getXmlDocLines(String text, String element) {
    // TODO(jonskeet): Convert markdown to XML documentation format.
    List<String> result = new ArrayList<>();
    result.add("/// <" + element + ">");
    for (String line : Splitter.on(String.format("%n")).split(text)) {
      result.add("/// " + line.replace("&", "&amp;").replace("<", "&lt;"));
    }
    result.add("/// </" + element + ">");
    return result;
  }

  /**
   * Splits given text into lines and returns an iterable of strings each one representing a
   * line decorated for an XML documentation comment, wrapped in the given element
   */
  public Iterable<String> getXmlParameterLines(String text, String parameterName) {
    // TODO(jonskeet): Convert markdown to XML documentation format.
    List<String> result = new ArrayList<>();
    result.add("/// <paramref name=\"" + parameterName + "\">");
    for (String line : Splitter.on(String.format("%n")).split(text)) {
      result.add("/// " + line.replace("&", "&amp;").replace("<", "&lt;"));
    }
    result.add("/// </paramref>");
    return result;
  }

  public String prependComma(String text) {
    return text.isEmpty() ? "" : ", " + text;
  }

  // Copied from csharp_helpers.cc and converted into Java.
  // The existing lowerUnderscoreToUpperCamel etc don't handle dots in the way we want.
  // TODO: investigate that and add more common methods if necessary.
  public String underscoresToCamelCase(
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

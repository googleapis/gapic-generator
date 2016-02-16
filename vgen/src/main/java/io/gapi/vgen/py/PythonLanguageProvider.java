package io.gapi.vgen.py;

import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.aspects.documentation.model.ElementDocumentationAttribute;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.TypeRef;
import com.google.api.tools.framework.model.TypeRef.Cardinality;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.snippet.SnippetSet;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import io.gapi.vgen.ApiConfig;
import io.gapi.vgen.GeneratedResult;
import io.gapi.vgen.LanguageProvider;
import io.gapi.vgen.SnippetDescriptor;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
     * @param iface
     */
    Doc generateBody(Interface iface);

    /**
     * Generates the result class, based on the result for the body,
     * and a set of accumulated types to be imported.
     */
    Doc generateClass(Interface iface, Doc body, Iterable<String> imports);
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
      .put(Type.TYPE_STRING, "\'\'")
      .put(Type.TYPE_BYTES, "\'\'")
      .build();

  /**
   * The path to the root of snippet resources.
   */
  private static final String SNIPPET_RESOURCE_ROOT =
      PythonLanguageProvider.class.getPackage().getName().replace('.', '/');

  /**
   * A set of python keywords and built-ins.
   * Built-ins derived from: https://docs.python.org/2/library/functions.html
   */
  private static final ImmutableSet<String> KEYWORD_BUILT_IN_SET =
      ImmutableSet.<String>builder()
      .add("and", "as", "assert", "break", "class", "continue", "def", "del", "elif", "else",
          "except", "exec", "finally", "for", "from", "global", "if", "import", "in", "is",
          "lambda", "not", "or", "pass", "print", "raise", "return", "try", "while", "with",
          "yield", "abs", "all", "any", "basestring", "bin", "bool", "bytearray", "callable", "chr",
          "classmethod", "cmp", "compile", "complex", "delattr", "dict", "dir", "divmod",
          "enumerate", "eval", "execfile", "file", "filter", "float", "format", "frozenset",
          "getattr", "globals", "hasattr", "hash", "help", "hex", "id", "input", "int",
          "isinstance", "issubclass", "iter", "len", "list", "locals", "long", "map", "max",
          "memoryview", "min", "next", "object", "oct", "open", "ord", "pow", "print", "property",
          "range", "raw_input", "reduce", "reload", "repr", "reversed", "round", "set", "setattr",
          "slice", "sorted", "staticmethod", "str", "sum", "super", "tuple", "type", "unichr",
          "unicode", "vars", "xrange", "zip", "__import__")
      .build();

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
    PythonImportHandler importHandler = new PythonImportHandler(service);
    ImmutableMap<String, Object> globalMap = ImmutableMap.<String, Object>builder()
        .put("context", this)
        .put("pyproto", new PythonProtoElements())
        .put("importHandler", importHandler)
        .build();
    PythonSnippetSet snippets = SnippetSet.createSnippetInterface(
        PythonSnippetSet.class,
        SNIPPET_RESOURCE_ROOT,
        snippetDescriptor.getSnippetInputName(),
        globalMap);

    Doc filenameDoc = snippets.generateFilename(service);
    String outputFilename = filenameDoc.prettyPrint();

    List<String> importList = importHandler.calculateImports();
    Doc body = snippets.generateBody(service);

    // Generate result.
    Doc result = snippets.generateClass(service, body, importList);
    return GeneratedResult.create(result, outputFilename);
  }

  /**
   * Return a Python docstring to be associated with the given ProtoElement.
   */
  public List<String> comments(ProtoElement element, PythonImportHandler importHandler) {
    List<String> comments;
    if (element instanceof Method) {
      comments = methodComments((Method) element, importHandler);
    } else {
      comments = defaultComments(element);
    }

    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (int i = 0; i < comments.size(); i++) {
      builder.add(PythonSphinxCommentFixer.sphinxify(comments.get(i)));
    }
    return builder.build();
  }

  /**
   * Return comments lines for a given proto element, extracted directly from the proto doc
   */
  private List<String> defaultComments(ProtoElement element) {
    if (!element.hasAttribute(ElementDocumentationAttribute.KEY)) {
      return ImmutableList.<String>of("");
    }
    return convertToCommentedBlock(DocumentationUtil.getScopedDescription(element));
  }

  /**
   * Return comments lines for a given method, consisting of proto doc and parameter type
   * documentation
   */
  private List<String> methodComments(Method msg, PythonImportHandler importHandler) {
    // Generate parameter types
    StringBuilder paramTypesBuilder = new StringBuilder();
    for (Field field : this.messages().flattenedFields(msg.getInputType())) {
      TypeRef type = field.getType();

      String cardinalityComment;
      if (type.getCardinality() == Cardinality.REPEATED) {
        cardinalityComment = "list of ";
      } else {
        cardinalityComment = "";
      }

      String typeComment;
      switch (type.getKind()) {
        case TYPE_MESSAGE:
          String msgPath = prefixInFile(type.getMessageType());
          msgPath = Strings.isNullOrEmpty(msgPath) ? "" : msgPath + ".";
          typeComment = importHandler.fileToModule(type.getMessageType().getFile()) + "."
              + msgPath + type.getMessageType().getSimpleName();
          break;

        case TYPE_ENUM:
          Preconditions.checkArgument(type.getEnumType().getValues().size() > 0);
          String enumPath = prefixInFile(type.getEnumType());
          enumPath = Strings.isNullOrEmpty(enumPath) ? "" : enumPath + ".";
          typeComment = "enum " + importHandler.fileToModule(type.getEnumType().getFile()) + "."
              + enumPath + type.getEnumType().getSimpleName();
          break;

        default:
          if (type.isPrimitive()) {
            typeComment = type.getPrimitiveTypeName();
          } else {
            throw new IllegalArgumentException("unknown type kind: " + type.getKind());
          }
          break;
      }

      paramTypesBuilder.append(
          String.format(":type %s: %s%s\n",
              field.getSimpleName(), cardinalityComment, typeComment));
    }
    // Remove trailing newline if exists
    if (paramTypesBuilder.length() > 0) {
      paramTypesBuilder.setLength(paramTypesBuilder.length() - 1);
    }
    String paramTypes = paramTypesBuilder.toString();

    // Generate comment contents
    StringBuilder contentBuilder = new StringBuilder();
    if (msg.hasAttribute(ElementDocumentationAttribute.KEY)) {
      contentBuilder.append(DocumentationUtil.getScopedDescription(msg));
      if (!Strings.isNullOrEmpty(paramTypes)) {
        contentBuilder.append("\n\n");
      }
    }
    if (!Strings.isNullOrEmpty(paramTypes)){
      contentBuilder.append(paramTypes);
    }

    // Don't generate empty docstrings
    if (contentBuilder.length() == 0) {
      return ImmutableList.<String>of("");
    }

    return convertToCommentedBlock(contentBuilder.toString());
  }

  /**
   * Return a non-conflicting safe name if name is a python built-in.
   */
  public String wrapIfKeywordOrBuiltIn(String name) {
    if (KEYWORD_BUILT_IN_SET.contains(name)) {
      return name + "_";
    }
    return name;
  }

  /**
   * Return the default value for the given field. Return null if there is no default value.
   */
  public String defaultValue(Field field, PythonImportHandler importHandler) {
    TypeRef type = field.getType();
    // Return empty array if the type is repeated.
    if (type.getCardinality() == Cardinality.REPEATED) {
      return "[]";
    }

    switch (type.getKind()) {
      case TYPE_MESSAGE:
        String msgPath = prefixInFile(type.getMessageType());
        msgPath = Strings.isNullOrEmpty(msgPath) ? "" : msgPath + ".";
        return importHandler.fileToModule(type.getMessageType().getFile()) + "." + msgPath
            + type.getMessageType().getSimpleName() + "()";
      case TYPE_ENUM:
        Preconditions.checkArgument(type.getEnumType().getValues().size() > 0);
        String enumPath = prefixInFile(type.getEnumType());
        enumPath = Strings.isNullOrEmpty(enumPath) ? "" : enumPath + ".";
        return importHandler.fileToModule(type.getEnumType().getFile()) + "."
            + enumPath + type.getEnumType().getValues().get(0).getSimpleName();
      default:
        if (type.isPrimitive()) {
          return DEFAULT_VALUE_MAP.get(type.getKind());
        }
        throw new IllegalArgumentException("unknown type kind: " + type.getKind());
    }
  }

  /**
   * Return whether the given field's default value is mutable in python.
   */
  public boolean isDefaultValueMutable(Field field) {
    TypeRef type = field.getType();
    if (type.getCardinality() == Cardinality.REPEATED) {
      return true;
    }
    switch(type.getKind()) {
      case TYPE_MESSAGE: // Fall-through.
      case TYPE_ENUM:
        return true;
      default:
        return false;
    }
  }

  /**
   * The dot-separated nested messages that contain an element in a Proto file.
   * Returns null if the element is top-level or a proto file itself.
   */
  public String prefixInFile(ProtoElement elt) {
    ProtoElement parent = elt.getParent();
    // elt is a proto file itself, or elt is top-level
    if (parent == null || parent.getParent() == null) {
      return null;
    }
    String prefix = parent.getSimpleName();
    for (parent = parent.getParent(); parent.getParent() != null; parent = parent.getParent()) {
      prefix = parent.getSimpleName() + "." + prefix;
    }
    return prefix;
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

}

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
package io.gapi.vgen.py;

import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.aspects.documentation.model.ElementDocumentationAttribute;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
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
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import io.gapi.vgen.ApiConfig;
import io.gapi.vgen.GeneratedResult;
import io.gapi.vgen.LanguageProvider;
import io.gapi.vgen.MethodConfig;
import io.gapi.vgen.SnippetDescriptor;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

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
     * Generates the result class, and a set of accumulated types to be imported.
     */
    Doc generateClass(Interface iface, Iterable<String> imports);
  }

  interface PythonDocSnippetSet {
    Doc generateFilename(ProtoFile file);
    Doc generateClass(ProtoFile file, Iterable<String> imports);
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
  public void outputCode(String outputArchiveFile, List<GeneratedResult> results,
        boolean archive) throws IOException {
    Map<String, Doc> files = new LinkedHashMap<>();
    for (GeneratedResult result : results) {
      files.put(result.getFilename(), result.getDoc());
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
    PythonImportHandler importHandler =
        new PythonImportHandler(service, getApiConfig().getInterfaceConfig(service));
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
    // Generate result.
    Doc result = snippets.generateClass(service, importList);

    String pathPrefix;
    if (!Strings.isNullOrEmpty(getApiConfig().getPackageName())) {
      pathPrefix = getApiConfig().getPackageName().replace('.', '/') + "/";
    } else {
      pathPrefix = "";
    }
    return GeneratedResult.create(result, pathPrefix + outputFilename);
  }

  public String filePath(ProtoFile file) {
    return file.getSimpleName().replace(".proto", "_pb2.py");
  }

  @Override
  public GeneratedResult generateDoc(ProtoFile file, SnippetDescriptor snippetDescriptor) {
    PythonImportHandler importHandler = new PythonImportHandler(file);
    ImmutableMap<String, Object> globalMap = ImmutableMap.<String, Object>builder()
        .put("context", this)
        .put("file", file)
        .put("importHandler", importHandler)
        .build();
    PythonDocSnippetSet snippets = SnippetSet.createSnippetInterface(
        PythonDocSnippetSet.class,
        SNIPPET_RESOURCE_ROOT,
        snippetDescriptor.getSnippetInputName(),
        globalMap);
    Doc filenameDoc = snippets.generateFilename(file);
    String outputFilename = filenameDoc.prettyPrint();
    List<String> importList = importHandler.calculateImports();
    Doc result = snippets.generateClass(file, importList);
    return GeneratedResult.create(result, outputFilename);
  }

  /**
   * Return a Python docstring to be associated with the given ProtoElement.
   */
  public List<String> comments(ProtoElement element, PythonImportHandler importHandler) {
    if (element instanceof Method) {
      return methodComments((Method) element, importHandler);
    } else if (element instanceof MessageType) {
      return messageComments((MessageType) element, importHandler);
    } else {
      return defaultComments(element);
    }
  }

  /**
   * Return comments lines for a given proto element, extracted directly from the proto doc
   */
  private List<String> defaultComments(ProtoElement element) {
    if (!element.hasAttribute(ElementDocumentationAttribute.KEY)) {
      return ImmutableList.<String>of("");
    }
    return convertToCommentedBlock(PythonSphinxCommentFixer.sphinxify(
        DocumentationUtil.getScopedDescription(element)));
  }

  /**
   * Returns type information for a field in Sphinx docstring style.
   */
  private String fieldTypeCardinalityComment(Field field, PythonImportHandler importHandler) {
    TypeRef type = field.getType();
    boolean closingBrace = false;
    String prefix;
    if (type.getCardinality() == Cardinality.REPEATED) {
      if (type.isMap()) {
        prefix = String.format("dict[%s, ", fieldTypeComment(type.getMapKeyField(),
            importHandler));
      } else {
        prefix = "list[";
      }
      closingBrace = true;
    } else {
      prefix = "";
    }
    String typeComment = fieldTypeComment(field, importHandler);
    return String.format("%s%s%s",
        prefix, typeComment, closingBrace ? "]" : "");
  }

  /**
   * Returns type information for a field in Sphinx docstring style.
   */
  private String fieldTypeComment(Field field, PythonImportHandler importHandler) {
    TypeRef type = field.getType();
    switch (type.getKind()) {
      case TYPE_MESSAGE:
        return ":class:`" + importHandler.elementPath(type.getMessageType(), true) + "`";
      case TYPE_ENUM:
        return ":class:`" + importHandler.elementPath(type.getEnumType(), true) + "`";
      default:
        if (type.isPrimitive()) {
          return type.getPrimitiveTypeName();
        } else {
          throw new IllegalArgumentException("unknown type kind: " + type.getKind());
        }
    }
  }

  /**
   * Returns a comment string for field, consisting of type information and proto comment.
   */
  private String fieldComment(Field field, PythonImportHandler importHandler) {
    String comment = String.format("  %s (%s)", field.getSimpleName(),
        fieldTypeCardinalityComment(field, importHandler));
    String paramComment = DocumentationUtil.getScopedDescription(field);
    if (!Strings.isNullOrEmpty(paramComment)) {
      if (paramComment.charAt(paramComment.length() - 1) == '\n') {
        paramComment = paramComment.substring(0, paramComment.length() - 1);
      }
      comment += ": " + paramComment.replaceAll("(\\r?\\n)", "\n    ");
    }
    return comment + "\n";
  }

  /**
   * Return comments lines for a given message, consisting of proto doc and argument type
   * documentation.
   */
  private List<String> messageComments(MessageType msg, PythonImportHandler importHandler) {
    // Generate parameter types
    StringBuilder paramTypesBuilder = new StringBuilder();
    paramTypesBuilder.append("Attributes:\n");
    for (Field field : msg.getFields()) {
      paramTypesBuilder.append(fieldComment(field, importHandler));
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
    contentBuilder.append(paramTypes);
    return convertToCommentedBlock(contentBuilder.toString());
  }

  /**
   * Return Sphinx-style return type string for the given method, or null if the return type is
   * None.
   */
  @Nullable
  private String returnTypeComment(Method method, PythonImportHandler importHandler) {
    MessageType returnMessageType = method.getOutputMessage();
    if (PythonProtoElements.isEmptyMessage(returnMessageType)) {
      return null;

    }

    String path = importHandler.elementPath(returnMessageType, true);
    String classInfo = ":class:`" + path + "` instance";
    MethodConfig config = getApiConfig().getInterfaceConfig((Interface) method.getParent())
        .getMethodConfig(method);

    if (config.isPageStreaming()) {
      return "Yields:\n  Instances of "
        + fieldTypeComment(config.getPageStreaming().getResourcesField(), importHandler)
        + "\n  unless page streaming is disabled through the call options. If"
        + "\n  page streaming is disabled, a single \n  " + classInfo + "\n  is returned.";

    } else {
      return "Returns:\n  A " + classInfo + ".";
    }
  }


  /**
   * Return comments lines for a given method, consisting of proto doc and parameter type
   * documentation.
   */
  private List<String> methodComments(Method msg, PythonImportHandler importHandler) {
    // Generate parameter types
    StringBuilder paramTypesBuilder = new StringBuilder();
    paramTypesBuilder.append("Args:\n");
    for (Field field : this.messages().flattenedFields(msg.getInputType())) {
      paramTypesBuilder.append(fieldComment(field, importHandler));
    }
    paramTypesBuilder.append("  options (:class:`google.gax.CallOptions`): " +
        "Overrides the default\n    settings for this call, e.g, timeout, retries etc.");
    String paramTypes = paramTypesBuilder.toString();

    String returnType = returnTypeComment(msg, importHandler);
    // Generate comment contents
    StringBuilder contentBuilder = new StringBuilder();
    if (msg.hasAttribute(ElementDocumentationAttribute.KEY)) {
      String sphinxified = PythonSphinxCommentFixer.sphinxify(
          DocumentationUtil.getScopedDescription(msg));
      sphinxified = sphinxified.trim();
      contentBuilder.append(sphinxified.replaceAll("\\s*\\n\\s*", "\n"));
      if (!Strings.isNullOrEmpty(paramTypes)) {
        contentBuilder.append("\n\n");
      }
    }
    contentBuilder.append(paramTypes);
    if (returnType != null) {
      contentBuilder.append("\n\n" + returnType);
    }

    contentBuilder.append(
        "\n\nRaises:\n  :exc:`google.gax.errors.GaxError` if the RPC is aborted.");
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
        return importHandler.elementPath(type.getMessageType(), false) + "()";
      case TYPE_ENUM:
        Preconditions.checkArgument(type.getEnumType().getValues().size() > 0,
            "enum must have a value");
        return importHandler.elementPath(type.getEnumType().getValues().get(0), false);
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

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
package io.gapi.vgen.java;

import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.FieldSelector;
import com.google.api.tools.framework.model.Interface;
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
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import io.gapi.vgen.ApiConfig;
import io.gapi.vgen.GeneratedResult;
import io.gapi.vgen.LanguageProvider;
import io.gapi.vgen.SnippetDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Language provider for Java codegen.
 */
public class JavaLanguageProvider extends LanguageProvider {

  /**
   * Entry points for the snippet set. Generation is partitioned into a first phase
   * which generates the content of the class without package and imports header,
   * and a second phase which completes the class based on the knowledge of which
   * other classes have been imported.
   */
  interface JavaSnippetSet {

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

    /**
     * Generates a code sample for a method.
     *
     * This method is meant to be indirectly called from within the snippet itself.
     * It takes a JavaDocConfig as an argument, since a lot of processing would have been completed
     * by the time this method is invoked and it would be pointless to redo all of them.
     */
    Doc generateMethodSampleCode(JavaDocConfig config);
  }

  /**
   * The package prefix protoc uses if no java package option was provided.
   */
  private static final String DEFAULT_JAVA_PACKAGE_PREFIX = "com.google.protos";

  /**
   * A prefix to match types from java.lang. Assumes well-formed qualified type names.
   */
  private static final String JAVA_LANG_TYPE_PREFIX = "java.lang.";

  /**
   * A map from primitive types in proto to Java counterparts.
   */
  private static final ImmutableMap<Type, String> PRIMITIVE_TYPE_MAP =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "boolean")
          .put(Type.TYPE_DOUBLE, "double")
          .put(Type.TYPE_FLOAT, "float")
          .put(Type.TYPE_INT64, "long")
          .put(Type.TYPE_UINT64, "long")
          .put(Type.TYPE_SINT64, "long")
          .put(Type.TYPE_FIXED64, "long")
          .put(Type.TYPE_SFIXED64, "long")
          .put(Type.TYPE_INT32, "int")
          .put(Type.TYPE_UINT32, "int")
          .put(Type.TYPE_SINT32, "int")
          .put(Type.TYPE_FIXED32, "int")
          .put(Type.TYPE_SFIXED32, "int")
          .put(Type.TYPE_STRING, "java.lang.String")
          .put(Type.TYPE_BYTES, "com.google.protobuf.ByteString")
          .build();

  /**
   * A map from primitive types in proto to zero value in Java
   */
  private static final ImmutableMap<Type, String> PRIMITIVE_ZERO_VALUE =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "false")
          .put(Type.TYPE_DOUBLE, "0.0")
          .put(Type.TYPE_FLOAT, "0.0F")
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
          .put(Type.TYPE_BYTES, "ByteString.EMPTY")
          .build();

  /**
   * A map from unboxed Java type to boxed counterpart.
   */
  private static final ImmutableMap<String, String> BOXED_TYPE_MAP =
      ImmutableMap.<String, String>builder()
          .put("boolean", "Boolean")
          .put("int", "Integer")
          .put("long", "Long")
          .put("double", "Double")
          .put("float", "Float")
          .build();

  /**
   * The path to the root of snippet resources.
   */
  private static final String SNIPPET_RESOURCE_ROOT =
      JavaLanguageProvider.class.getPackage().getName().replace('.', '/');

  /**
   * A bi-map from full names to short names indicating the import map.
   */
  private final BiMap<String, String> imports = HashBiMap.create();

  /**
   * A map from simple type name to a boolean, indicating whether its in java.lang or not.
   * If a simple type name is not in the map, this information is unknown.
   */
  private final Map<String, Boolean> implicitImports = Maps.newHashMap();

  private JavaSnippetSet snippets = null;

  /**
   * Constructs the Java language provider.
   */
  public JavaLanguageProvider(Model model, ApiConfig apiConfig) {
    super(model, apiConfig);
  }

  @Override
  public void outputCode(String outputPath, List<GeneratedResult> results, boolean archive)
      throws IOException {
    Map<String, Doc> files = new LinkedHashMap<>();
    for (GeneratedResult result : results) {
      String path = getApiConfig().getPackageName().replace('.', '/');
      files.put(path + "/" + result.getFilename(), result.getDoc());
    }
    if (archive) {
      ToolUtil.writeJar(files, outputPath);
    } else {
      ToolUtil.writeFiles(files, outputPath);
    }
  }

  @Override
  public GeneratedResult generate(Interface service, SnippetDescriptor snippetDescriptor) {
    JavaSnippetSet snippets =
        SnippetSet.createSnippetInterface(
            JavaSnippetSet.class,
            SNIPPET_RESOURCE_ROOT,
            snippetDescriptor.getSnippetInputName(),
            ImmutableMap.<String, Object>of("context", this));

    this.snippets = snippets;

    Doc filenameDoc = snippets.generateFilename(service);
    String outputFilename = filenameDoc.prettyPrint();

    // Generate the body, which will collect the imports.
    imports.clear();
    Doc body = snippets.generateBody(service);

    // Clean up the imports.
    List<String> cleanedImports = new ArrayList<>();
    String defaultPackagePrefix = getApiConfig().getPackageName() + ".";
    for (String imported : imports.keySet()) {
      if (imported.startsWith(JAVA_LANG_TYPE_PREFIX) || imported.startsWith(defaultPackagePrefix)) {
        // Imported type is in package or in java.lang, can be be ignored.
        continue;
      }
      cleanedImports.add(imported);
    }
    Collections.sort(cleanedImports);

    // Generate result.
    Doc result = snippets.generateClass(service, body, cleanedImports);
    return GeneratedResult.create(result, outputFilename);
  }

  /**
   * Takes a fully-qualified type name and returns its simple name, and also saves the
   * type in the import list.
   */
  public String getTypeName(String typeName) {
    int lastDotIndex = typeName.lastIndexOf('.');
    if (lastDotIndex < 0) {
      throw new IllegalArgumentException("expected fully qualified name");
    }
    String shortTypeName = typeName.substring(lastDotIndex + 1);
    return getMinimallyQualifiedName(typeName, shortTypeName);
  }

  /**
   * Adds the given type name to the import list. Returns an empty string so that the
   * output is not affected in snippets.
   */
  public String addImport(String typeName) {
    // used for its side effect of adding the type to the import list if the short name
    // hasn't been imported yet
    getTypeName(typeName);
    return "";
  }

  // Snippet Helpers
  // ===============

  /**
   * Gets the java package for the given proto file.
   */
  public String getJavaPackage(ProtoFile file) {
    String packageName = file.getProto().getOptions().getJavaPackage();
    if (Strings.isNullOrEmpty(packageName)) {
      return DEFAULT_JAVA_PACKAGE_PREFIX + "." + file.getFullName();
    }
    return packageName;
  }

  /**
   * Gets the name of the class which is the grpc container for this service interface.
   */
  public String getGrpcName(Interface service) {
    String fullName =
        String.format("%s.%sGrpc", getJavaPackage(service.getFile()), service.getSimpleName());
    return getTypeName(fullName);
  }

  /**
   * Given a TypeRef, returns the return statement for that type. Specifically, this will
   * return an empty string for the empty type (we don't want a return statement for void).
   */
  public String methodReturnStatement(TypeRef type) {
    if (messages().isEmptyType(type)) {
      return "";
    } else {
      return "return ";
    }
  }

  /**
   * Given a TypeRef, returns the String form of the type to be used as a return value.
   * Special case: this will return "void" for the Empty return type.
   */
  public String methodReturnTypeName(TypeRef type) {
    if (messages().isEmptyType(type)) {
      return "void";
    } else {
      return typeName(type);
    }
  }

  /**
   * Returns the Java representation of a reference to a type.
   */
  public String typeName(TypeRef type) {
    if (type.isMap()) {
      String mapTypeName = getTypeName("java.util.Map");
      return String.format(
          "%s<%s, %s>",
          mapTypeName,
          basicTypeNameBoxed(type.getMapKeyField().getType()),
          basicTypeNameBoxed(type.getMapValueField().getType()));
    } else if (type.isRepeated()) {
      String listTypeName = getTypeName("java.util.List");
      return String.format("%s<%s>", listTypeName, basicTypeNameBoxed(type));
    } else {
      return basicTypeName(type);
    }
  }

  /**
   * Returns the Java representation of a zero value for that type, to be used in code sample doc.
   *
   * Parametric types may use the diamond operator, since the return value will be used only in
   * initialization.
   */
  public String zeroValue(TypeRef type) {
    // Don't call getTypeName; we don't need to import these.
    if (type.isMap()) {
      return "new HashMap<>()";
    }
    if (type.isRepeated()) {
      return "new ArrayList<>()";
    }
    if (PRIMITIVE_ZERO_VALUE.containsKey(type.getKind())) {
      return PRIMITIVE_ZERO_VALUE.get(type.getKind());
    }
    if (type.isMessage()) {
      return typeName(type) + ".newBuilder().build()";
    }
    return "null";
  }

  /**
   * Returns the Java representation of a type, without cardinality, in boxed form.
   */
  public String basicTypeNameBoxed(TypeRef type) {
    String unboxed = basicTypeName(type);
    String boxed = BOXED_TYPE_MAP.get(unboxed);
    if (boxed != null) {
      return boxed;
    } else {
      return unboxed;
    }
  }

  /**
   * Returns the Java representation of a type, without cardinality.
   * If the type is a Java primitive, basicTypeName returns it in unboxed form.
   */
  public String basicTypeName(TypeRef type) {
    String result = PRIMITIVE_TYPE_MAP.get(type.getKind());
    if (result != null) {
      if (result.contains(".")) {
        // Fully qualified type name, use regular type name resolver. Can skip boxing logic
        // because those types are already boxed.
        return getTypeName(result);
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
   * Gets the full name of the message or enum type in Java.
   */
  public String getTypeName(ProtoElement elem) {
    // Construct the full name in Java
    String name = getJavaPackage(elem.getFile());
    if (!elem.getFile().getProto().getOptions().getJavaMultipleFiles()) {
      String outerClassName = elem.getFile().getProto().getOptions().getJavaOuterClassname();
      if (outerClassName.isEmpty()) {
        outerClassName = getFileClassName(elem.getFile());
      }
      name = name + "." + outerClassName;
    }
    String shortName = elem.getFullName().substring(elem.getFile().getFullName().length() + 1);
    name = name + "." + shortName;

    return getMinimallyQualifiedName(name, shortName);
  }

  private String getMinimallyQualifiedName(String fullName, String shortName) {
    // Derive a short name if possible
    if (imports.containsKey(fullName)) {
      // Short name already there.
      return imports.get(fullName);
    }
    if (imports.containsValue(shortName)
        || !fullName.startsWith(JAVA_LANG_TYPE_PREFIX) && isImplicitImport(shortName)) {
      // Short name clashes, use long name.
      return fullName;
    }
    imports.put(fullName, shortName);
    return shortName;
  }

  /**
   * Checks whether the simple type name is implicitly imported from java.lang.
   */
  private boolean isImplicitImport(String name) {
    Boolean yes = implicitImports.get(name);
    if (yes != null) {
      return yes;
    }
    // Use reflection to determine whether the name exists in java.lang.
    try {
      Class.forName("java.lang." + name);
      yes = true;
    } catch (Exception e) {
      yes = false;
    }
    implicitImports.put(name, yes);
    return yes;
  }

  /**
   * Gets the class name for the given proto file.
   */
  private String getFileClassName(ProtoFile file) {
    String baseName = Files.getNameWithoutExtension(new File(file.getSimpleName()).getName());
    return lowerUnderscoreToUpperCamel(baseName);
  }

  /**
   * Returns the description of the proto element, in markdown format.
   */
  public String getDescription(ProtoElement element) {
    return DocumentationUtil.getDescription(element);
  }

  public String generateMethodSampleCode(JavaDocConfig config) {
    return snippets.generateMethodSampleCode(config).prettyPrint();
  }

  /**
   * Splits given text into lines and returns an iterable of strings each one representing a
   * line decorated for a javadoc documentation comment. Markdown will be translated to javadoc.
   */
  public Iterable<String> getJavaDocLines(String text) {
    return getJavaDocLinesWithPrefix(text, "");
  }

  /**
   * Splits given text into lines and returns an iterable of strings each one representing a
   * line decorated for a javadoc documentation comment, with the first line prefixed with
   * firstLinePrefix. Markdown will be translated to javadoc.
   */
  public Iterable<String> getJavaDocLinesWithPrefix(String text, String firstLinePrefix) {
    // TODO(wgg): convert markdown to javadoc
    List<String> result = new ArrayList<>();
    String linePrefix = firstLinePrefix;
    for (String line : Splitter.on(String.format("%n")).split(text)) {
      line = line.replace("*/", "&ast;/");
      result.add(" * " + linePrefix + line);
      linePrefix = "";
    }
    return result;
  }

  public String defaultTokenValue(Field field) {
    if (field.getType().getKind().equals(Type.TYPE_STRING)) {
      return "\"\"";
    } else if (field.getType().getKind().equals(Type.TYPE_BYTES)) {
      String byteStringTypeName = getTypeName("com.google.protobuf.ByteString");
      return byteStringTypeName + ".EMPTY";
    } else {
      throw new IllegalArgumentException(
          String.format(
              "Unsupported type for field %s - found %s, "
                  + "but expected TYPE_STRING or TYPE_BYTES",
              field.getFullName(),
              field.getType().getKind()));
    }
  }

  // Workaround for the fact that quotes can't be used in a snippet @join
  public String partitionKeyCode(ImmutableList<FieldSelector> discriminatorFields) {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < discriminatorFields.size(); i++) {
      if (i > 0) {
        buf.append(" + \"|\" + ");
      }
      String simpleName = discriminatorFields.get(i).getLastField().getSimpleName();
      buf.append("request.get" + lowerUnderscoreToUpperCamel(simpleName) + "()");
    }
    return buf.toString();
  }

  @AutoValue
  abstract static class Variable {
    public abstract TypeRef getType();

    public abstract String getName();

    public abstract String getDescription();
  }

  public Variable newVariable(TypeRef type, String name, String description) {
    return new AutoValue_JavaLanguageProvider_Variable(type, name, description);
  }

  @AutoValue
  abstract static class JavaDocConfig {
    public abstract String getMethodName();

    public abstract String getReturnType();

    public abstract ImmutableList<Variable> getParams();

    public abstract boolean isIterableVariant();

    public abstract boolean isCallableVariant();

    @AutoValue.Builder
    abstract static class Builder {
      public abstract Builder setMethodName(String methodName);

      public abstract Builder setReturnType(String returnType);

      public abstract Builder setParams(ImmutableList<Variable> params);

      public Builder setParams(JavaLanguageProvider languageProvider, ImmutableList<Field> fields) {
        ImmutableList.Builder<Variable> params = ImmutableList.<Variable>builder();
        for (Field field : fields) {
          params.add(
              languageProvider.newVariable(
                  field.getType(),
                  languageProvider.lowerUnderscoreToLowerCamel(field.getSimpleName()),
                  languageProvider.getDescription(field)));
        }
        return setParams(params.build());
      }

      public Builder setSingleParam(
          JavaLanguageProvider languageProvider, TypeRef requestType, String name, String doc) {
        return setParams(
            ImmutableList.of(
                languageProvider.newVariable(requestType, name, doc)));
      }

      public abstract Builder setIterableVariant(boolean iterable);

      public abstract Builder setCallableVariant(boolean callable);

      public abstract JavaDocConfig build();
    }
  }

  public JavaDocConfig.Builder newJavaDocConfigBuilder() {
    return new AutoValue_JavaLanguageProvider_JavaDocConfig.Builder();
  }

  public String returnTypeOrEmpty(TypeRef returnType) {
    return messages().isEmptyType(returnType) ? "" : typeName(returnType);
  }

  public boolean getTrue() {
    return true;
  }

  public boolean getFalse() {
    return false;
  }

  public String requestParamDoc() {
    return "The request object containing all of the parameters for the API call.";
  }

  public String requestParam() {
    return "request";
  }
}

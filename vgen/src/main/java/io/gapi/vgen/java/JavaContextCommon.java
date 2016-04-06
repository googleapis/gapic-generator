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

import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.TypeRef;
import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;

import io.gapi.vgen.LanguageContext;
import io.gapi.vgen.LanguageUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A class that provides helper methods for snippet files generating Java code
 * to get data and perform data transformations that are difficult or messy to do
 * in the snippets themselves.
 */
public class JavaContextCommon {

  /**
   * A regexp to match types from java.lang. Assumes well-formed qualified type names.
   */
  private static final String JAVA_LANG_TYPE_PREFIX = "java.lang.";

  /**
   * Escaper for formatting javadoc strings.
   */
  private static final Escaper JAVADOC_ESCAPER =
      Escapers.builder()
      .addEscape('&', "&amp;")
      .addEscape('<', "&lt;")
      .addEscape('>', "&gt;")
      .addEscape('*', "&ast;")
      .build();

  /**
   * A map from unboxed Java primitive type name to boxed counterpart.
   */
  private static final ImmutableMap<String, String> BOXED_TYPE_MAP =
      ImmutableMap.<String, String>builder()
      .put("boolean", "Boolean")
      .put("int", "Integer")
      .put("long", "Long")
      .put("float", "Float")
      .put("double", "Double")
      .build();

  /**
   * A bi-map from full names to short names indicating the import map.
   */
  private final BiMap<String, String> imports = HashBiMap.create();

  /**
   * A map from simple type name to a boolean, indicating whether its in java.lang or not.
   * If a simple type name is not in the map, this information is unknown.
   */
  private final Map<String, Boolean> implicitImports = Maps.newHashMap();

  private final String defaultPackagePrefix;

  public JavaContextCommon(String defaultPackagePrefix) {
    this.defaultPackagePrefix = defaultPackagePrefix;
  }

  /**
   * Returns the Java representation of a basic type in boxed form.
   */
  public String boxedTypeName(String typeName) {
    return LanguageUtil.getRename(typeName, BOXED_TYPE_MAP);
  }

  public String getMinimallyQualifiedName(String fullName, String shortName) {
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
    text = JAVADOC_ESCAPER.escape(text);
    for (String line : Splitter.on(String.format("%n")).split(text)) {
      result.add(" * " + linePrefix + line);
      linePrefix = "";
    }
    return result;
  }

  @AutoValue
  abstract static class Variable {
    public abstract TypeRef getType();

    public abstract String getName();

    public abstract String getDescription();
  }

  // This member function is necessary to provide access to snippets for
  // the functionality, since snippets can't call static functions.
  public Variable newVariable(TypeRef type, String name, String description) {
    return s_newVariable(type, name, description);
  }

  // This function is necessary to provide a static entry point for the same-named
  // member function.
  public static Variable s_newVariable(TypeRef type, String name, String description) {
    return new AutoValue_JavaContextCommon_Variable(type, name, description);
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

      public Builder setParams(JavaGapicContext languageProvider, ImmutableList<Field> fields) {
        ImmutableList.Builder<Variable> params = ImmutableList.<Variable>builder();
        for (Field field : fields) {
          params.add(
              s_newVariable(
                  field.getType(),
                  LanguageUtil.lowerUnderscoreToLowerCamel(field.getSimpleName()),
                  languageProvider.getDescription(field)));
        }
        return setParams(params.build());
      }

      public Builder setSingleParam(
          JavaGapicContext languageProvider, TypeRef requestType, String name, String doc) {
        return setParams(
            ImmutableList.of(
                s_newVariable(requestType, name, doc)));
      }

      public abstract Builder setIterableVariant(boolean iterable);

      public abstract Builder setCallableVariant(boolean callable);

      public abstract JavaDocConfig build();
    }
  }

  public JavaDocConfig.Builder newJavaDocConfigBuilder() {
    return new AutoValue_JavaContextCommon_JavaDocConfig.Builder();
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

  public List<String> getImports() {
    // Clean up the imports.
    List<String> cleanedImports = new ArrayList<>();
    for (String imported : imports.keySet()) {
      if (imported.startsWith(JAVA_LANG_TYPE_PREFIX) ||
          defaultPackagePrefix != null && imported.startsWith(defaultPackagePrefix)) {
        // Imported type is in java.lang or in package, can be ignored.
        continue;
      }
      cleanedImports.add(imported);
    }
    Collections.sort(cleanedImports);
    return cleanedImports;
  }
}

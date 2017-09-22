/* Copyright 2017 Google Inc
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
package com.google.api.codegen.discogapic.transformer;

import com.google.api.codegen.Inflector;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.common.base.Strings;

/** Provides language-specific names for variables and classes of Discovery-Document models. */
public class DiscoGapicNamer {
  private final SurfaceNamer languageNamer;
  private static final String regexDelimiter = "\\.";

  /* Create a JavaSurfaceNamer for a Discovery-based API. */
  public DiscoGapicNamer(SurfaceNamer parentNamer) {
    this.languageNamer = parentNamer;
  }

  public DiscoGapicNamer cloneWithPackageName(String packageName) {
    return new DiscoGapicNamer(languageNamer.cloneWithPackageNameForDiscovery(packageName));
  }

  /* @return the underlying language surface namer. */
  public SurfaceNamer getLanguageNamer() {
    return languageNamer;
  }

  /** Returns the variable name for a field. */
  public String getFieldVarName(String fieldName) {
    return languageNamer.privateFieldName(Name.anyCamel(fieldName));
  }

  /** Returns the resource getter method name for a resource field. */
  public String getResourceGetterName(String fieldName) {
    Name name;
    if (fieldName.contains("_")) {
      name = Name.anyCamel(fieldName.split("_"));
    } else {
      name = Name.anyCamel(fieldName);
    }
    return languageNamer.publicMethodName(Name.anyCamel("get").join(name));
  }

  /** Returns the resource setter method name for a resource field. */
  public String getResourceSetterName(String fieldName) {
    Name name;
    if (fieldName.contains("_")) {
      name = Name.anyCamel(fieldName.split("_"));
    } else {
      name = Name.anyCamel(fieldName);
    }
    return languageNamer.publicMethodName(Name.anyCamel("set").join(name));
  }

  /** Returns the name for a ResourceName for the resource of the given method. */
  public String getResourceNameName(Method method, String parentResource) {
    return languageNamer.localVarName(
        getQualifiedResourceIdentifier(method, parentResource).join("name"));
  }

  /** Returns the name for a ResourceName for the resource of the given method. */
  public String getResourceNameTypeName(Method method, String parentResource) {
    return languageNamer.publicClassName(
        getQualifiedResourceIdentifier(method, parentResource).join("name").join("type"));
  }

  /**
   * Formats the method as a Name. Methods are generally in the format
   * "[api].[resource].[function]".
   */
  public static Name methodAsName(Method method) {
    String[] pieces = method.id().split(regexDelimiter);
    String resourceLastName = pieces[pieces.length - 2];
    if (!method.isPluralMethod()) {
      resourceLastName = Inflector.singularize(resourceLastName);
    }
    Name resource = Name.anyCamel(resourceLastName);
    for (int i = pieces.length - 3; i > 0; i--) {
      resource = Name.anyCamel(pieces[i]).join(resource);
    }
    Name function = Name.anyCamel(pieces[pieces.length - 1]);
    return function.join(resource);
  }

  /** Return the name of the qualified resource from a given method's path. */
  public static Name getQualifiedResourceIdentifier(Method method, String qualifyingResource) {
    String methodPath = method.flatPath();
    Name qualifier = Name.anyCamel(qualifyingResource);
    String baseResource = getResourceIdentifier(methodPath).toLowerCamel();
    Name baseResourceName = Name.anyCamel(baseResource);

    if (!Inflector.singularize(qualifier.toLowerCamel()).equals(baseResource)) {
      baseResourceName = qualifier.join(baseResourceName);
    }

    return baseResourceName;
  }

  /** Return the name of the unqualified resource from a given method's path. */
  public static Name getResourceIdentifier(String methodPath) {
    // Assumes the resource is the last curly-bracketed String in the path.
    String baseResource =
        methodPath.substring(methodPath.lastIndexOf('{') + 1, methodPath.lastIndexOf('}'));
    return Name.anyCamel(baseResource);
  }

  public static String getSimpleInterfaceName(String interfaceName) {
    String[] pieces = interfaceName.split(regexDelimiter);
    return pieces[pieces.length - 1];
  }

  /** Get the request type name from a method. */
  public static Name getRequestName(Method method) {
    String[] pieces = method.id().split(regexDelimiter);
    String methodName = pieces[pieces.length - 1];
    String resourceName = pieces[pieces.length - 2];
    if (!method.isPluralMethod()) {
      resourceName = Inflector.singularize(resourceName);
    }
    return Name.anyCamel(methodName, resourceName, "http", "request");
  }

  /**
   * Assuming the input is a child of a Method, returns the name of the field as a parameter. If the
   * schema is a path or query parameter, then returns the schema's id(). If the schema is the
   * request object, then returns "resource" appended to the schema's id().
   */
  public static Name getSchemaNameAsParameter(Schema schema) {
    String paramString =
        Strings.isNullOrEmpty(schema.reference()) ? schema.getIdentifier() : schema.reference();
    Name param = Name.fromUnderScoreOrCamel(paramString);
    if (Strings.isNullOrEmpty(schema.location())) {
      param = param.join("resource");
    }
    return param;
  }

  /** Get the request type name from a method. */
  public TypeName getRequestTypeName(Method method) {
    TypeNameConverter typeNameConverter = languageNamer.getTypeNameConverter();
    return typeNameConverter.getTypeNameInImplicitPackage(
        languageNamer.publicClassName(getRequestName(method)));
  }

  public static Name getInterfaceName(String defaultInterfaceName) {
    String[] pieces = defaultInterfaceName.split(regexDelimiter);
    String resource = pieces[pieces.length - 1];
    return Name.anyCamel(Inflector.singularize(resource), "admin");
  }

  /** Get the response type name from a method. */
  public static Name getResponseName(Method method) {
    if (method.response() != null) {
      String typeName =
          method.response().reference() != null
              ? method.response().reference()
              : method.response().getIdentifier();
      return Name.anyCamel(typeName);
    } else {
      String[] pieces = method.id().split(regexDelimiter);
      String methodName = pieces[pieces.length - 1];
      String resourceName = pieces[pieces.length - 2];
      if (!method.isPluralMethod()) {
        resourceName = Inflector.singularize(resourceName);
      }
      return Name.anyCamel(methodName, resourceName, "http", "response");
    }
  }

  //TODO(andrealin): Naming methods for service name.
}

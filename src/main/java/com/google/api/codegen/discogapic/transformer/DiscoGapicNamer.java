/* Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.discogapic.transformer;

import com.google.api.codegen.Inflector;
import com.google.api.codegen.config.DiscoveryField;
import com.google.api.codegen.config.ResourceNameConfig;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.common.base.Strings;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/** Provides language-specific names for variables and classes of Discovery-Document models. */
public class DiscoGapicNamer {
  private final SurfaceNamer languageNamer;
  private static final String REGEX_DELIMITER = "\\.";
  private static final Pattern UNBRACKETED_PATH_SEGMENTS_PATTERN =
      Pattern.compile("\\}/((?:[a-zA-Z]+/){2,})\\{");

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

  public Name stringToName(String fieldName) {
    if (fieldName.contains("_")) {
      return Name.anyCamel(fieldName.split("_"));
    } else {
      return Name.anyCamel(fieldName);
    }
  }

  /** Returns the resource getter method name for a resource field. */
  public String getResourceGetterName(String fieldName) {
    return languageNamer.publicMethodName(Name.anyCamel("get").join(stringToName(fieldName)));
  }

  /** Returns the resource setter method name for a resource field. */
  public String getResourceSetterName(String fieldName, boolean isRepeated) {
    if (isRepeated) {
      return languageNamer.publicMethodName(Name.from("add", "all").join(stringToName(fieldName)));
    } else {
      return languageNamer.publicMethodName(Name.from("set").join(stringToName(fieldName)));
    }
  }

  /** Returns the resource adder method name for a resource field assuming the field is repeated. */
  public String getResourceAdderName(String fieldName) {
    return languageNamer.publicMethodName(Name.from("add").join(stringToName(fieldName)));
  }

  /** Returns the name for a ResourceName for the resource of the given method. */
  public String getResourceNameName(ResourceNameConfig resourceNameConfig) {
    return languageNamer.localVarName(
        Name.anyCamel(resourceNameConfig.getEntityName()).join("name"));
  }

  /** Returns the name for a ResourceName for the resource of the given method. */
  public String getResourceNameTypeName(ResourceNameConfig resourceNameConfig) {
    return languageNamer.publicClassName(
        Name.anyCamel(resourceNameConfig.getEntityName()).join("name").join("type"));
  }

  /**
   * Formats the method as a Name. Methods are generally in the format
   * "[api].[resource].[function]".
   */
  public static Name methodAsName(Method method) {
    String[] pieces = method.id().split(REGEX_DELIMITER);
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
    Name baseResourceName = Name.anyCamel(getResourceIdentifier(methodPath).toLowerCamel());

    // If the qualifying resource is the same as the base resource, just return the base resource name.
    if (!Inflector.singularize(qualifier.toLowerCamel()).equals(baseResourceName.toLowerCamel())) {
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
    String[] pieces = interfaceName.split(REGEX_DELIMITER);
    return pieces[pieces.length - 1];
  }

  /** Get the request type name from a method. */
  public static Name getRequestName(Method method) {
    String[] pieces = method.id().split(REGEX_DELIMITER);
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
    String[] pieces = paramString.split("_");
    Name param = Name.anyCamel(pieces);
    if (Strings.isNullOrEmpty(schema.location()) && schema.type().equals(Schema.Type.OBJECT)) {
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
    String[] pieces = defaultInterfaceName.split(REGEX_DELIMITER);
    String resource = pieces[pieces.length - 1];
    return Name.anyCamel(Inflector.singularize(resource));
  }

  /** Get the response type from a method if the method has a non-null response type. */
  @Nullable
  public DiscoveryField getResponseField(Method method) {
    if (method.response() != null) {
      Schema responseSchema;
      if (method.response().reference() != null) {
        responseSchema = method.response().dereference();
      } else {
        responseSchema = method.getDocument().schemas().get(method.response().getIdentifier());
      }
      return DiscoveryField.create(responseSchema, this);
    }
    return null;
  }

  /**
   * Get the canonical path for a method, in the form "(%s/\{%s\})+" e.g. for a method path
   * "{project}/regions/{region}/addresses", this returns "projects/{project}/regions/{region}".
   */
  public static String getCanonicalPath(Method method) {
    String namePattern = method.flatPath();
    // Ensure the first path segment is a string literal representing a resource type.
    if (namePattern.charAt(0) == '{') {
      String firstResource =
          Inflector.pluralize(namePattern.substring(1, namePattern.indexOf("}")));
      namePattern = String.format("%s/%s", firstResource, namePattern);
    }
    // Remove any trailing non-bracketed substring
    if (!namePattern.endsWith("}") && namePattern.contains("}")) {
      namePattern = namePattern.substring(0, namePattern.lastIndexOf('}') + 1);
    }
    // For each sequence of consecutive non-bracketed path segments,
    // replace those segments with the last one in the sequence.
    Matcher m = UNBRACKETED_PATH_SEGMENTS_PATTERN.matcher(namePattern);
    if (m.find()) {
      StringBuffer sb = new StringBuffer();
      for (int i = 1; i <= m.groupCount(); i++) {
        String multipleSegment = m.group(i);
        String[] segmentPieces = multipleSegment.split("/");
        Name segment = Name.anyCamel(segmentPieces[segmentPieces.length - 1]);
        m.appendReplacement(sb, String.format("}/%s/{", segment.toLowerCamel()));
      }
      namePattern = m.appendTail(sb).toString();
    }
    return namePattern;
  }
}

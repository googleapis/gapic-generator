/* Copyright 2018 Google LLC
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
import com.google.api.codegen.util.Name;
import com.google.common.base.Strings;
import java.util.regex.Pattern;

/**
 * Utility class for parsing Discovery document fields into canonical inputs for the discogapic
 * transformer pipeline.
 */
public class DiscoGapicParser {
  private static final String REGEX_DELIMITER = "\\.";
  private static final String PATH_DELIMITER = "/";

  private static final Pattern UNBRACKETED_PATH_SEGMENTS_PATTERN =
      Pattern.compile("\\}/((?:[a-zA-Z]+/){2,})\\{");

  public static Name stringToName(String fieldName) {
    if (fieldName.contains("_")) {
      return Name.anyCamel(fieldName.split("_"));
    } else {
      return Name.anyCamel(fieldName);
    }
  }

  /**
   * Assuming the input is a child of a Method, returns the name of the field as a parameter. If the
   * schema is a path or query parameter, then returns the schema's id(). If the schema is the
   * request object, then returns "resource" appended to the schema's id().
   *
   * @param field
   */
  public static Name getFieldNameAsParameter(DiscoveryField field) {
    Schema deref = field.getDiscoveryField().dereference();
    if (Strings.isNullOrEmpty(deref.location())
        && deref.type().equals(Schema.Type.OBJECT)
        && field.getOriginalDiscoveryField().parent() instanceof Method) {
      // This is the resource object for an API request message type.
      Name param = DiscoGapicParser.stringToName(deref.getIdentifier());
      return param.join("resource");
    } else {
      return DiscoGapicParser.stringToName(field.getOriginalDiscoveryField().getIdentifier());
    }
  }

  /**
   * Returns the name of a method's request object as a parameter. For non-null The name is the
   * string "resource" appended to the schema's id().
   */
  public static Name getMethodInputName(Method method) {
    Schema deref = method.request().dereference();
    // This is the resource object for an API request message type.
    Name param = DiscoGapicParser.stringToName(deref.getIdentifier());
    return param.join("resource");
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
   * Get the canonical path for a path, in the form "(%s/\{%s\})+" e.g. for a method path
   * "{project}/regions/{foo}/addresses", this returns "projects/{project}/regions/{region}".
   */
  public static String getCanonicalPath(String namePattern) {
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

    return namePattern;
  }

  public static Name getInterfaceName(String defaultInterfaceName) {
    String[] pieces = defaultInterfaceName.split(REGEX_DELIMITER);
    String resource = pieces[pieces.length - 1];
    return Name.anyCamel(Inflector.singularize(resource));
  }

  public static String getSimpleInterfaceName(String interfaceName) {
    String[] pieces = interfaceName.split(REGEX_DELIMITER);
    return pieces[pieces.length - 1];
  }

  /** Return the name of the unqualified resource from a given method's path. */
  public static Name getResourceIdentifier(String methodPath) {
    // Assumes the resource is the last curly-bracketed String in the path.
    String baseResource =
        methodPath.substring(methodPath.lastIndexOf('{') + 1, methodPath.lastIndexOf('}'));
    return Name.anyCamel(baseResource);
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

  /**
   * Return the name of the fully qualified resource from a given canonicalized path. Use {@link
   * #getCanonicalPath(String)}} for canonicalization of the parameter. This method includes all
   * segments from the input path in the resulting Name except for consecutive duplicate segments.
   */
  public static Name getQualifiedResourceIdentifier(String canonicalPath) {
    String[] pieces = canonicalPath.split(PATH_DELIMITER);

    Name name = Name.from();
    String previous = null;
    for (String segment : pieces) {
      String next = segment;
      if (segment.contains("}")) {
        next = segment.substring(1, segment.length() - 1);
      }
      next = Inflector.singularize(next);
      if (!next.equals(previous)) {
        // Only append to the name if this segment is not identical to the previous segment.
        name = name.join(stringToName(next));
        previous = next;
      }
    }

    return name;
  }

  /** Returns the name for a ResourceName for the resource of the given method. */
  public static Name getResourceNameName(ResourceNameConfig resourceNameConfig) {
    return Name.anyCamel(resourceNameConfig.getEntityName()).join("name");
  }
}

/* Copyright 2016 Google Inc
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
package com.google.api.codegen.discovery.config.go;

import com.google.api.codegen.DiscoveryImporter;
import com.google.api.codegen.discovery.DefaultString;
import com.google.api.codegen.discovery.config.TypeNameGenerator;
import com.google.api.codegen.util.Name;
import com.google.common.base.Strings;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoTypeNameGenerator implements TypeNameGenerator {

  // Pattern used to rename some Go package versions.
  private static final Pattern SUB_VERSION = Pattern.compile("^(.+)_(v[0-9.]+)$");

  @Override
  public String getApiVersion(String apiVersion) {
    if (apiVersion.equals("alpha") || apiVersion.equals("beta")) {
      return "v0." + apiVersion;
    }
    Matcher subVersion = SUB_VERSION.matcher(apiVersion);
    if (subVersion.matches()) {
      return subVersion.group(1) + "/" + subVersion.group(2);
    }
    return apiVersion;
  }

  @Override
  public String getPackagePrefix(String apiName, String apiVersion) {
    return "google.golang.org/api/" + apiName + "/" + apiVersion;
  }

  @Override
  public String getApiTypeName(String apiName) {
    // N/A
    return "";
  }

  @Override
  public String getRequestTypeName(List<String> methodNameComponents) {
    String copy[] = methodNameComponents.toArray(new String[methodNameComponents.size() + 1]);
    copy[copy.length - 1] = "call";
    return Name.lowerCamel(copy).toUpperCamel();
  }

  @Override
  public String getResponseTypeUrl(String responseTypeUrl) {
    // Go client libraries return an empty struct if the responseTypeUrl is
    // "Empty". If the responseTypeName is truly empty ("empty$"), nothing is
    // returned.
    if (responseTypeUrl.equals(DiscoveryImporter.EMPTY_TYPE_NAME)) {
      return "";
    }
    return responseTypeUrl;
  }

  @Override
  public String getMessageTypeName(String messageTypeName) {
    // Avoid cases like "DatasetList.Datasets"
    String pieces[] = messageTypeName.split("\\.");
    return pieces[pieces.length - 1];
  }

  @Override
  public String getSubpackage(boolean isRequest) {
    // N/A
    return "";
  }

  @Override
  public String getStringFormatExample(String format) {
    return "";
  }

  @Override
  public String getFieldPatternExample(String pattern) {
    String def = DefaultString.getNonTrivialPlaceholder(pattern);
    if (Strings.isNullOrEmpty(def)) {
      return "";
    }
    return String.format("\"%s\"", def);
  }
}

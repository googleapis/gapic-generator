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
package com.google.api.codegen.discovery.config.nodejs;

import com.google.api.codegen.DiscoveryImporter;
import com.google.api.codegen.discovery.DefaultString;
import com.google.api.codegen.discovery.config.TypeNameGenerator;
import com.google.common.base.Strings;
import java.util.List;

public class NodeJSTypeNameGenerator implements TypeNameGenerator {

  @Override
  public String getApiVersion(String apiVersion) {
    return apiVersion;
  }

  @Override
  public String getPackagePrefix(String apiName, String apiVersion) {
    // N/A
    return "";
  }

  @Override
  public String getApiTypeName(String apiName) {
    return apiName;
  }

  @Override
  public String getRequestTypeName(List<String> methodNameComponents) {
    // N/A
    return "";
  }

  @Override
  public String getResponseTypeUrl(String responseTypeUrl) {
    if (responseTypeUrl.equals(DiscoveryImporter.EMPTY_TYPE_NAME)
        || responseTypeUrl.equals(DiscoveryImporter.EMPTY_TYPE_URL)) {
      return "";
    }
    return responseTypeUrl;
  }

  @Override
  public String getMessageTypeName(String messageTypeName) {
    // N/A
    return "";
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
    return String.format("'%s'", def);
  }
}

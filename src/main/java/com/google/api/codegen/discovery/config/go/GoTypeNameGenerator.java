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

import java.util.List;

import com.google.api.codegen.discovery.DefaultString;
import com.google.api.codegen.discovery.config.TypeNameGenerator;
import com.google.common.base.Strings;

public class GoTypeNameGenerator implements TypeNameGenerator {

  @Override
  public String getPackagePrefix(String apiName, String apiVersion) {
    // TODO Auto-generated method stub
    return "google.golang.org/api/" + apiName + "/" + apiVersion;
  }

  @Override
  public String getApiTypeName(String apiName) {
    // N/A
    return "";
  }

  @Override
  public String getRequestTypeName(List<String> methodNameComponents) {
    // N/A
    return "";
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

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
package com.google.api.codegen.discovery.config.java;

import com.google.api.codegen.discovery.config.TypeNameGenerator;
import com.google.api.codegen.util.Name;
import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.List;

public class JavaTypeNameGenerator extends TypeNameGenerator {

  private static final String PACKAGE_PREFIX = "com.google.api.services";
  private static final String NON_REQUEST_SUBPACKAGE = "model";

  @Override
  public String getPackagePrefix(String apiName, String apiVersion) {
    // Most Java libraries don't include the apiVersion in their package.
    return Joiner.on('.').join(PACKAGE_PREFIX, apiName);
  }

  @Override
  public String getRequestTypeName(List<String> methodNameComponents) {
    List<String> copy = new ArrayList<>(methodNameComponents);
    for (int i = 0; i < copy.size(); i++) {
      copy.set(i, Name.lowerCamel(copy.get(i)).toUpperCamel());
    }
    return Joiner.on('.').join(copy);
  }

  @Override
  public String getSubpackage(boolean isRequest) {
    if (!isRequest) {
      return NON_REQUEST_SUBPACKAGE;
    }
    return "";
  }

  @Override
  public String getStringFormatExample(String format) {
    return getStringFormatExample(
        format,
        "java.text.SimpleDateFormat",
        "com.google.api.client.util.DateTime.toStringRfc3339()");
  }
}

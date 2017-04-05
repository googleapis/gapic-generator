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
package com.google.api.codegen.discovery.config.js;

import com.google.api.codegen.discovery.config.TypeNameGenerator;
import java.util.List;

public class JSTypeNameGenerator extends TypeNameGenerator {

  @Override
  public List<String> getMethodNameComponents(List<String> nameComponents) {
    // In JS, we want the whole name components list to make it to the
    // transformer because every element is part of the method construction.
    return nameComponents;
  }

  @Override
  public String stringDelimiter() {
    return "'";
  }

  @Override
  public String getStringFormatExample(String format) {
    return getStringFormatExample(format, "Date.toISOString()", "Date.toISOString()");
  }

  @Override
  public String getDiscoveryDocUrl(String apiName, String apiVersion) {
    return String.format(
        "https://content.googleapis.com/discovery/v1/apis/%s/%s/rest", apiName, apiVersion);
  }
}

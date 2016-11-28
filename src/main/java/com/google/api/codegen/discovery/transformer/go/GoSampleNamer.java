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
package com.google.api.codegen.discovery.transformer.go;

import com.google.api.codegen.discovery.transformer.SampleNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.go.GoNameFormatter;
import com.google.common.base.Splitter;
import java.util.List;

public class GoSampleNamer extends SampleNamer {

  public GoSampleNamer() {
    super(new GoNameFormatter());
  }

  @Override
  public String getServiceVarName(String packagePrefix) {
    return localVarName(Name.lowerCamel(packagePrefix, "service"));
  }

  /**
   * Returns the package name of the service.
   *
   * <p>Only intended for use in Go. For example: "google.golang.org/api/logging/v2beta1" returns
   * "logging"
   */
  public static String getServicePackageName(String packagePrefix) {
    List<String> split = Splitter.on('/').splitToList(packagePrefix);
    String localName = "";
    if (split.size() < 2) {
      throw new IllegalArgumentException("expected packagePrefix to have at least 2 segments");
    }
    // Get the second to last value.
    // "google.golang.org/api/logging/v2beta1"
    //                        ^^^^^^^
    localName = split.get(split.size() - 2);
    return localName;
  }

  /**
   * Returns the constant form of given an authScope URL.
   *
   * <p>For example: DriveScope
   */
  public static String getAuthScopeConst(String authScope) {
    int slash = authScope.lastIndexOf('/');
    if (slash < 0) {
      throw new IllegalArgumentException(
          String.format("malformed scope, cannot find slash: %s", authScope));
    }
    authScope = authScope.substring(slash + 1).replace('.', '_').replace('-', '_');
    return Name.from(authScope, "scope").toUpperCamel();
  }

  @Override
  public String getResponseVarName() {
    return "resp";
  }

  @Override
  public String getRequestBodyVarName() {
    return "rb";
  }
}

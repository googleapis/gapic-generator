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
package com.google.api.codegen.discovery.config.csharp;

import com.google.api.codegen.discovery.config.TypeNameGenerator;
import com.google.api.codegen.util.Name;
import com.google.common.base.Joiner;
import java.util.LinkedList;
import java.util.List;

public class CSharpTypeNameGenerator extends TypeNameGenerator {

  @Override
  public List<String> getMethodNameComponents(List<String> nameComponents) {
    LinkedList<String> copy = new LinkedList<String>(nameComponents);
    // Don't edit the original object.
    copy.removeFirst();
    for (int i = 0; i < copy.size(); i++) {
      copy.set(i, Name.lowerCamel(copy.get(i)).toUpperCamel());
    }
    return copy;
  }

  @Override
  public String getPackagePrefix(String apiName, String apiCanonicalName, String apiVersion) {
    return Joiner.on('.')
        .join("Google", "Apis", apiCanonicalName.replace(" ", ""), apiVersion.replace(".", "_"));
  }

  @Override
  public String getApiTypeName(String canonicalName) {
    return canonicalName.replace(" ", "") + "Service";
  }
}

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
package com.google.api.codegen.discovery.config.php;

import com.google.api.codegen.discovery.config.TypeNameGenerator;
import com.google.api.codegen.util.Name;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;

public class PhpTypeNameGenerator extends TypeNameGenerator {

  /**
   * Set of method names that must have the previous name components as upper-camel suffixes.
   *
   * <p>For example: "foo.bar.list" to "foo.bar.listFooBar"
   */
  ImmutableSet<String> RENAMED_METHODS = ImmutableSet.of("list", "clone");

  @Override
  public String stringDelimiter() {
    return "'";
  }

  @Override
  public String getApiTypeName(String apiName) {
    return "Google_Service_" + apiName.replace(" ", "");
  }

  @Override
  public List<String> getMethodNameComponents(List<String> nameComponents) {
    ArrayList<String> out = new ArrayList<>();

    nameComponents = super.getMethodNameComponents(nameComponents);
    String verb = nameComponents.remove(nameComponents.size() - 1); // Pop the last element.
    if (RENAMED_METHODS.contains(verb)) {
      for (String s : nameComponents) {
        verb += Name.lowerCamel(s).toUpperCamel();
      }
    }
    // If there are multiple resources before the verb, they're joined on '_'.
    // Ex: "$service->billingAccounts_projects->listBillingAccountsProjects"
    if (nameComponents.size() > 1) {
      out.add(Joiner.on('_').join(nameComponents));
    } else if (nameComponents.size() == 1) {
      out.add(nameComponents.get(0));
    }
    out.add(verb);
    return out;
  }

  @Override
  public String getMessageTypeName(String messageTypeName) {
    // Avoid cases like "DatasetList.Datasets"
    String pieces[] = messageTypeName.split("\\.");
    return pieces[pieces.length - 1];
  }
}

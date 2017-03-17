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
import com.google.api.codegen.util.java.JavaNameFormatter;
import com.google.common.base.Joiner;
import java.util.LinkedList;
import java.util.List;

public class JavaTypeNameGenerator extends TypeNameGenerator {

  @Override
  public List<String> getMethodNameComponents(List<String> nameComponents) {
    // Don't edit the original object.
    LinkedList<String> copy = new LinkedList<>(nameComponents);
    copy.removeFirst();
    // Handle cases where the method signature contains keywords.
    // ex: "variants.import" to "variants.genomicsImport"
    for (int i = 0; i < copy.size(); i++) {
      if (JavaNameFormatter.RESERVED_IDENTIFIER_SET.contains(copy.get(i))) {
        String prefix = Name.upperCamel(apiCanonicalName).toLowerCamel();
        copy.set(i, Name.lowerCamel(prefix, copy.get(i)).toLowerCamel());
      }
    }
    return copy;
  }

  @Override
  public String getRequestTypeName(List<String> methodNameComponents) {
    // Use getMethodNameComponents to ensure we keep any transformations on the
    // method signature.
    List<String> copy = getMethodNameComponents(methodNameComponents);
    for (int i = 0; i < copy.size(); i++) {
      // When generating the request type name, if the current method name
      // component is the same as the first method name component (which is
      // removed from the copy returned by `getMethodNameComponents`), then
      // "Operations" is appended.
      // ex: "sheets.spreadsheets.sheets.copyTo" -> "Spreadsheets.SheetsOperations.CopyTo"
      if (copy.get(i).equals(methodNameComponents.get(0))) {
        copy.set(i, Name.lowerCamel(copy.get(i), "operations").toLowerCamel());
      }
      copy.set(i, Name.lowerCamel(copy.get(i)).toUpperCamel());
    }
    return Joiner.on('.').join(copy);
  }

  @Override
  public String getStringFormatExample(String format) {
    return getStringFormatExample(
        format,
        "java.text.SimpleDateFormat",
        "com.google.api.client.util.DateTime.toStringRfc3339()");
  }
}

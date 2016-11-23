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
package com.google.api.codegen.discovery.transformer.csharp;

import com.google.api.codegen.discovery.transformer.SampleNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.csharp.CSharpNameFormatter;

class CSharpSampleNamer extends SampleNamer {

  public CSharpSampleNamer() {
    super(new CSharpNameFormatter());
  }

  @Override
  public String getSampleClassName(String apiCanonicalName) {
    // Using the Name class causes replacements like "SQL" to "Sql", which we
    // don't want in C#. So, we use regular concatenation.
    return apiCanonicalName.replace(" ", "") + "Example";
  }

  @Override
  public String getServiceVarName(String apiTypeName) {
    return Name.upperCamel(apiTypeName).toLowerCamel();
  }

  @Override
  public String getResourceVarName(String resourceTypeName) {
    String[] pieces = resourceTypeName.split("\\.");
    return super.getResourceVarName(pieces[pieces.length - 1]);
  }

  public static String getNamespaceName(String apiCanonicalName) {
    return apiCanonicalName.replace(" ", "") + "Sample";
  }
}

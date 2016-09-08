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
package com.google.api.codegen.discovery.transformer;

import com.google.api.codegen.discovery.SampleConfig;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NameFormatter;
import com.google.api.codegen.util.NameFormatterDelegator;

/**
 * TODO(saicheems)
 */
public class SampleNamer extends NameFormatterDelegator {

  public SampleNamer(NameFormatter nameFormatter) {
    super(nameFormatter);
  }

  private String getNotImplementedString(String feature) {
    return "$ NOT IMPLEMENTED: " + feature + " $";
  }

  public String getServiceVarName() {
    return "service";
  }

  public String getServiceClassName(SampleConfig sampleConfig) {
    String name = sampleConfig.apiName();
    // TODO(saicheems): WTF is this for?? Why convert to class name here...
    // Converts name to a lower camel format Name (b/c name is lower camel) which is then converted
    // to upper camel in util.java.NameFormatter...
    return className(Name.lowerCamel(name));
  }

  public String getFieldVarName(String fieldName) {
    return varName(Name.lowerCamel(fieldName));
  }

  public String getMapEntryTypeFromMapType(String nickname) {
    return getNotImplementedString("getMapEntryTypeFromMapType");
  }
}

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
package com.google.api.codegen.discovery.transformer.ruby;

import com.google.api.codegen.discovery.transformer.SampleNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.ruby.RubyNameFormatter;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

public class RubySampleNamer extends SampleNamer {

  // Map of rename rules for fields.
  // Could be done through overrides, but this is a rule implemented in the Ruby
  // client library generator.
  private static final ImmutableMap<String, String> FIELD_RENAMES =
      ImmutableMap.of("objectId", "object_id_");

  public RubySampleNamer() {
    super(new RubyNameFormatter());
  }

  public static String getServiceTypeNamespace(String apiName, String apiVersion) {
    // The version suffix doesn't seem to follow any particular pattern other
    // than that the first character is capitalized.
    apiVersion = apiVersion.substring(0, 1).toUpperCase() + apiVersion.substring(1);
    // For whatever reason the namespace that contains the service constructor
    // isn't based on apiTypeName, so generate it from the apiName and
    // apiVersion instead.
    // Ex: "Google::Apis::MyapiV1beta1" instead of "Google::Apis::MyApiV1beta1"
    return Joiner.on("::")
        .join("Google", "Apis", Name.lowerCamel(apiName).toUpperCamel() + apiVersion);
  }

  @Override
  public String getServiceVarName(String apiTypeName) {
    return localVarName(Name.from("service"));
  }

  @Override
  public String getFieldVarName(String fieldName) {
    if (FIELD_RENAMES.containsKey(fieldName)) {
      return FIELD_RENAMES.get(fieldName);
    }
    return localVarName(Name.lowerCamel(fieldName));
  }

  @Override
  public String getRequestBodyFieldSetterName(String fieldName) {
    return publicMethodName(Name.lowerCamel(fieldName));
  }
}

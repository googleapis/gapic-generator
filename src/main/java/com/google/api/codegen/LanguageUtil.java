/* Copyright 2016 Google LLC
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
package com.google.api.codegen;

import com.google.api.codegen.util.CommonAcronyms;
import com.google.common.base.CaseFormat;
import java.util.Map;

/** Some static utility methods. */
public class LanguageUtil {
  public static String getRename(String name, Map<String, String> map) {
    String rename = map.get(name);
    if (rename != null) {
      return rename;
    } else {
      return name;
    }
  }

  public static String getTODO() {
    return "TODO:";
  }

  public static String upperCamelToUpperUnderscore(String name) {
    name = CommonAcronyms.camelizeUpperAcronyms(name);
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name);
  }

  public static String upperCamelToLowerCamel(String name) {
    name = CommonAcronyms.camelizeUpperAcronyms(name);
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name);
  }

  public static String upperCamelToLowerUnderscore(String name) {
    name = CommonAcronyms.camelizeUpperAcronyms(name);
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
  }

  public static String upperUnderscoreToUpperCamel(String name) {
    return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name);
  }

  public static String lowerUnderscoreToUpperUnderscore(String name) {
    return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_UNDERSCORE, name);
  }

  public static String lowerUnderscoreToUpperCamel(String name) {
    return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name);
  }

  public static String lowerUnderscoreToLowerCamel(String name) {
    return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name);
  }

  public static String lowerCamelToUpperCamel(String name) {
    return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name);
  }

  public static String lowerCamelToLowerUnderscore(String name) {
    return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
  }

  public static String lowerCamelToUpperUnderscore(String name) {
    return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name);
  }
}

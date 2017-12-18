/* Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A CodegenContext or a derived class of it provides helper methods for snippet files to get data
 * and perform data transformations that are difficult or messy to do in the snippets themselves. At
 * this level, functions are provided that are not specific to any language or use case. The
 * CodegenContext hierarchy specializes first by use case (e.g. Gapic vs Discovery), and secondarily
 * by language.
 */
public class CodegenContext {

  // Note the below methods are instance-based, even if they don't depend on instance state,
  // so they can be accessed by templates.

  public String getRename(String name, Map<String, String> map) {
    return LanguageUtil.getRename(name, map);
  }

  public <T> List<T> getMost(List<T> list) {
    return list.subList(0, list.size() - 1);
  }

  public <T> T getLast(List<T> list) {
    return list.get(list.size() - 1);
  }

  public boolean isSingleton(List list) {
    return list.size() == 1;
  }

  public String getTODO() {
    return LanguageUtil.getTODO();
  }

  public String upperCamelToUpperUnderscore(String name) {
    return LanguageUtil.upperCamelToUpperUnderscore(name);
  }

  public String upperCamelToLowerCamel(String name) {
    return LanguageUtil.upperCamelToLowerCamel(name);
  }

  public String upperCamelToLowerUnderscore(String name) {
    return LanguageUtil.upperCamelToLowerUnderscore(name);
  }

  public String upperUnderscoreToUpperCamel(String name) {
    return LanguageUtil.upperUnderscoreToUpperCamel(name);
  }

  public String lowerUnderscoreToUpperUnderscore(String name) {
    return LanguageUtil.lowerUnderscoreToUpperUnderscore(name);
  }

  public String lowerUnderscoreToUpperCamel(String name) {
    return LanguageUtil.lowerUnderscoreToUpperCamel(name);
  }

  public String lowerUnderscoreToLowerCamel(String name) {
    return LanguageUtil.lowerUnderscoreToLowerCamel(name);
  }

  public String lowerCamelToUpperCamel(String name) {
    return LanguageUtil.lowerCamelToUpperCamel(name);
  }

  public String lowerCamelToLowerUnderscore(String name) {
    return LanguageUtil.lowerCamelToLowerUnderscore(name);
  }

  /*
   * This method is necessary to call m.entrySet() from snippets,
   * due to method resolution complexities.
   * See com.google.api.tools.framework.snippet.Elem::findMethod for more details.
   */
  public <K, V> Collection<Map.Entry<K, V>> entrySet(Map<K, V> m) {
    return m.entrySet();
  }
}

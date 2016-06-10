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
package com.google.api.codegen;

import com.google.api.tools.framework.model.ProtoElement;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * An implementation of CodePathMapper that generates the output path from a prefix, and/or package
 * name.
 */
public class PrefixPackageCodePathMapper implements CodePathMapper {
  private String prefix;
  private String separator;

  public PrefixPackageCodePathMapper(String prefix, String separator) {
    this.prefix = prefix;
    this.separator = separator;
  }

  public PrefixPackageCodePathMapper(String prefix) {
    this(prefix, null);
  }

  @Override
  public String getOutputPath(ProtoElement element, ApiConfig config) {
    ArrayList<String> dirs = new ArrayList<>();
    if (!Strings.isNullOrEmpty(prefix)) {
      dirs.add(prefix);
    }

    if (!Strings.isNullOrEmpty(separator) && !Strings.isNullOrEmpty(config.getPackageName())) {
      for (String segment : config.getPackageName().split(Pattern.quote(separator))) {
        dirs.add(segment.toLowerCase());
      }
    }
    return Joiner.on("/").join(dirs);
  }
}

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

import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Utility methods for code generation. */
public class CodeGeneratorUtil {

  /** Writes generated output to files under the given root. */
  public static <Element> void writeGeneratedOutput(
      String root, Multimap<Element, GeneratedResult> elements) throws IOException {
    Map<String, Doc> files = new LinkedHashMap<>();
    for (GeneratedResult generatedResult : elements.values()) {
      files.put(generatedResult.getFilename(), generatedResult.getDoc());
    }
    ToolUtil.writeFiles(files, root);
  }
}

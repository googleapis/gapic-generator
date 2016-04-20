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
package io.gapi.vgen.py;

import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.snippet.SnippetSet;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import io.gapi.vgen.ApiConfig;
import io.gapi.vgen.GeneratedResult;
import io.gapi.vgen.SnippetDescriptor;

import java.util.List;

/**
 * A PythonLanguageProvider provides general Python code generation logic.
 */
public class PythonLanguageProvider {

  /**
   * The path to the root of snippet resources.
   */
  static final String SNIPPET_RESOURCE_ROOT =
      PythonContextCommon.class.getPackage().getName().replace('.', '/');

  @SuppressWarnings("unchecked")
  public <Element> GeneratedResult generate(Element element, ApiConfig apiConfig,
      SnippetDescriptor snippetDescriptor, PythonGapicContext context,
      PythonImportHandler importHandler) {
    ImmutableMap<String, Object> globalMap = ImmutableMap.<String, Object>builder()
        .put("context", context)
        .put("pyproto", new PythonProtoElements())
        .put("importHandler", importHandler)
        .build();
    PythonSnippetSet<Element> snippets = SnippetSet.createSnippetInterface(
        PythonSnippetSet.class,
        SNIPPET_RESOURCE_ROOT,
        snippetDescriptor.getSnippetInputName(),
        globalMap);

    Doc filenameDoc = snippets.generateFilename(element);
    String outputFilename = filenameDoc.prettyPrint();
    List<String> importList = importHandler.calculateImports();
    // Generate result.
    Doc result = snippets.generateClass(element, importList);
    String pathPrefix;
    if (!Strings.isNullOrEmpty(apiConfig.getPackageName())) {
      pathPrefix = apiConfig.getPackageName().replace('.', '/') + "/";
    } else {
      pathPrefix = "";
    }
    return GeneratedResult.create(result, pathPrefix + outputFilename);
  }
}

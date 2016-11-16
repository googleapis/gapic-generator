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
package com.google.api.codegen.metadatagen;

import com.google.api.codegen.GeneratedResult;
import com.google.api.codegen.SnippetSetRunner;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.snippet.SnippetSet;
import com.google.common.collect.ImmutableMap;

/** A SnippetSetRunner for package metadata template rendering. */
public class PackageMetadataSnippetSetRunner {
  public GeneratedResult generate(
      Model model, String snippetFileName, PackageMetadataContext context) {
    PackageMetadataSnippetSet snippets =
        SnippetSet.createSnippetInterface(
            PackageMetadataSnippetSet.class,
            SnippetSetRunner.SNIPPET_RESOURCE_ROOT + "/metadatagen",
            snippetFileName,
            ImmutableMap.<String, Object>of("context", context));
    return GeneratedResult.create(
        snippets.generateBody(model), snippets.generateFilename().prettyPrint());
  }
}

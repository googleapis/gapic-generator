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
package io.gapi.vgen.java;

import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.FieldSelector;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.snippet.SnippetSet;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.google.common.io.Files;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.google.common.collect.Multimap;

import io.gapi.vgen.GeneratedResult;
import io.gapi.vgen.LanguageProvider;
import io.gapi.vgen.MethodConfig;
import io.gapi.vgen.SnippetDescriptor;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A JavaLanguageProvider provides general Java code generation logic that is agnostic
 * to the use case (e.g. Gapic vs Discovery). Behavior that is specific to a use
 * case is provided through a subclass of JavaContext.
 */
public class JavaLanguageProvider {

  /**
   * The path to the root of snippet resources.
   */
  private static final String SNIPPET_RESOURCE_ROOT =
      JavaContextCommon.class.getPackage().getName().replace('.', '/');

  public <Element> void output(String root, String outputPath,
      Multimap<Element, GeneratedResult> elements, boolean archive)
          throws IOException {
    Map<String, Doc> files = new LinkedHashMap<>();
    for (GeneratedResult generatedResult : elements.values()) {
      files.put(root + "/" + generatedResult.getFilename(), generatedResult.getDoc());
    }
    if (archive) {
      ToolUtil.writeJar(files, outputPath);
    } else {
      ToolUtil.writeFiles(files, outputPath);
    }
  }

  @SuppressWarnings("unchecked")
  public <Element> GeneratedResult generate(Element element,
      SnippetDescriptor snippetDescriptor, JavaContext context, String defaultPackagePrefix) {
    JavaSnippetSet<Element> snippets = SnippetSet.createSnippetInterface(
        JavaSnippetSet.class,
        SNIPPET_RESOURCE_ROOT,
        snippetDescriptor.getSnippetInputName(),
        ImmutableMap.<String, Object>of("context", context));

    String outputFilename = snippets.generateFilename(element).prettyPrint();
    JavaContextCommon javaContextCommon = new JavaContextCommon(defaultPackagePrefix);
    context.resetState(snippets, javaContextCommon);

    // Generate the body, which will collect the imports.
    Doc body = snippets.generateBody(element);

    List<String> cleanedImports = javaContextCommon.getImports();

    // Generate result.
    Doc result = snippets.generateClass(element, body, cleanedImports);
    return GeneratedResult.create(result, outputFilename);
  }

  public <Element> GeneratedResult generate(Element element,
      SnippetDescriptor snippetDescriptor, JavaContext context) {
    return generate(element, snippetDescriptor, context, null);
  }
}

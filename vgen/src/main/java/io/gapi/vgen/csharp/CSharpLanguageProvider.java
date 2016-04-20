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
package io.gapi.vgen.csharp;

import com.google.api.gax.protobuf.PathTemplate;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.snippet.SnippetSet;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import autovalue.shaded.com.google.common.common.collect.ImmutableList;
import io.gapi.vgen.ApiConfig;
import io.gapi.vgen.FlatteningConfig;
import io.gapi.vgen.GeneratedResult;
import io.gapi.vgen.SnippetDescriptor;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A CSharpLanguageProvider provides general CSharp code generation logic.
 */
public class CSharpLanguageProvider {

  /**
   * The path to the root of snippet resources.
   */
  private static final String SNIPPET_RESOURCE_ROOT =
      CSharpContextCommon.class.getPackage().getName().replace('.', '/');

  @SuppressWarnings("unchecked")
  public <Element> GeneratedResult generate(Element element,
      SnippetDescriptor snippetDescriptor, CSharpGapicContext context, String serviceNamespace) {
    CSharpSnippetSet<Element> snippets = SnippetSet.createSnippetInterface(
        CSharpSnippetSet.class,
        SNIPPET_RESOURCE_ROOT,
        snippetDescriptor.getSnippetInputName(),
        ImmutableMap.<String, Object>of("context", context));

    String outputFilename = snippets.generateFilename(element).prettyPrint();

    CSharpContextCommon csharpCommon = new CSharpContextCommon(serviceNamespace);
    context.resetState(csharpCommon);

    // Generate the body, which will collect the imports.
    // Note that generateBody populates imports.
    Doc body = snippets.generateBody(element);

    TreeSet<String> imports = csharpCommon.getImports();

    // Generate result.
    Doc result = snippets.generateClass(element, body, imports);
    return GeneratedResult.create(result, outputFilename);
  }
}

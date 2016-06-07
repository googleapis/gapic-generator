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
import com.google.protobuf.Method;

import java.util.Map;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Fragment generator baseline tests.
 */
@RunWith(Parameterized.class)
public abstract class DiscoveryFragmentGeneratorTestBase extends DiscoveryGeneratorTestBase {

  public DiscoveryFragmentGeneratorTestBase(
      String name, String discoveryDocFileName, String[] gapicConfigFileNames) {
    super(name, discoveryDocFileName, gapicConfigFileNames);
  }

  @Override
  protected Object run() {
    String snippetInputName = config.getTemplates(0).getSnippetFiles(0);
    Map<Method, GeneratedResult> result =
        DiscoveryFragmentGenerator.create(config, discoveryImporter)
            .generateFragments(snippetInputName);
    if (result == null) {
      return null;
    } else {
      Doc output = Doc.EMPTY;
      for (GeneratedResult fragment : result.values()) {
        output = Doc.joinWith(Doc.BREAK, output, fragment.getDoc());
      }
      return Doc.vgroup(output);
    }
  }
}

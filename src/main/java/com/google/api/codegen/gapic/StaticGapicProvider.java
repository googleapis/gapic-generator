/* Copyright 2017 Google LLC
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
package com.google.api.codegen.gapic;

import com.google.api.tools.framework.snippet.Doc;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** An implememtation class of GapicProvider which handles static files. */
public class StaticGapicProvider<E> implements GapicProvider<E> {
  StaticFileRunner staticFileRunner;

  public StaticGapicProvider(StaticFileRunner staticFileRunner) {
    this.staticFileRunner = staticFileRunner;
  }

  @Override
  public Map<String, Doc> generate() {
    try {
      staticFileRunner.run();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return Collections.emptyMap();
  }

  @Override
  public List<String> getSnippetFileNames() {
    // Static provider does not support snippet files
    return Collections.emptyList();
  }

  @Override
  public Map<String, Doc> generate(String snippetFileName) {
    // Static provider does not support snippet files
    return Collections.emptyMap();
  }
}

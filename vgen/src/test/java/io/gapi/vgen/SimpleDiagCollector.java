/* Copyright 2016 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http: *www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gapi.vgen;

import java.util.List;

import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.common.collect.Lists;

public class SimpleDiagCollector implements DiagCollector {
  private final List<Diag> diags = Lists.newArrayList();
  private int errorCount;

  /**
   * Adds a diagnosis.
   */
  @Override
  public void addDiag(Diag diag) {
    diags.add(diag);
    if (diag.getKind() == Diag.Kind.ERROR) {
      errorCount++;
    }
  }

  /**
   * Returns the number of diagnosed proper errors.
   */
  @Override
  public int getErrorCount() {
    return errorCount;
  }

  /**
   * Returns the diagnosis accumulated.
   */
  public List<Diag> getDiags() {
    return diags;
  }
}

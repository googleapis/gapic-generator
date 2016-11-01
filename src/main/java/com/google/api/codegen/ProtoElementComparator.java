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
import java.util.Comparator;

/** A comparator for ProtoElements for, e.g., ensuring determinism in test output. */
public class ProtoElementComparator implements Comparator<ProtoElement> {

  @Override
  public int compare(ProtoElement elt1, ProtoElement elt2) {
    int simple = elt1.getFile().getSimpleName().compareTo(elt2.getFile().getSimpleName());
    if (simple == 0) {
      return elt1.getFullName().compareTo(elt2.getFullName());
    } else {
      return simple;
    }
  }
}

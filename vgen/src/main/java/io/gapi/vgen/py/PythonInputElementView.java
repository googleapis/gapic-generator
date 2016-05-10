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

import com.google.api.tools.framework.model.ProtoElement;
import com.google.common.collect.ImmutableMap;

import io.gapi.vgen.InputElementView;

public interface PythonInputElementView<InputElementT extends ProtoElement>
    extends InputElementView<InputElementT> {
  public PythonImportHandler getImportHandler(InputElementT t, PythonGapicContext context);

  public ImmutableMap<String, Object> getGlobalMap(InputElementT element);

  public String getPathPrefix(PythonGapicContext context);
}

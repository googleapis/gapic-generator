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
package com.google.api.codegen.py;

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.clientconfig.ClientConfigGapicContext;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;

/**
 * A GAPIC context adapted to the Python client config.
 */
public class PythonClientConfigGapicContext extends ClientConfigGapicContext {

  private PythonContextCommon pythonCommon;

  /**
   * Constructs the Python client config codegen context.
   */
  public PythonClientConfigGapicContext(Model model, ApiConfig config) {
    super(model, config);
    this.pythonCommon = new PythonContextCommon();
  }

  /**
   * Converts the dot-separated Python package name from the GAPIC config to a slash-separated
   * directory structure.
   */
  @Override
  public String getOutputSubPath(ProtoElement element) {
    return pythonCommon.getOutputSubPath(getApiConfig().getPackageName());
  }
}

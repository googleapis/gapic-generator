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
package com.google.api.codegen.nodejs;

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.clientconfig.ClientConfigGapicContext;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;

public class NodeJSClientConfigGapicContext extends ClientConfigGapicContext {
  public NodeJSClientConfigGapicContext(Model model, ApiConfig config) {
    super(model, config);
  }

  @Override
  public String getOutputSubPath(ProtoElement element) {
    return NodeJSGapicContextUtil.getOutputSubPath();
  }
}

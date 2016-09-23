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
import com.google.api.tools.framework.model.Interface;
import com.google.common.collect.ImmutableMap;

public class PythonInterfaceInitializer implements PythonSnippetSetInputInitializer<Interface> {

  private final ApiConfig apiConfig;

  public PythonInterfaceInitializer(ApiConfig apiConfig) {
    this.apiConfig = apiConfig;
  }

  @Override
  public PythonImportHandler getImportHandler(Interface iface) {
    return new PythonImportHandler(iface, apiConfig);
  }

  @Override
  public ImmutableMap<String, Object> getGlobalMap(Interface iface) {
    return ImmutableMap.of("pyproto", (Object) new PythonProtoElements());
  }
}

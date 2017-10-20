/* Copyright 2017 Google Inc
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
package com.google.api.codegen.configgen;

import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Location;
import com.google.api.tools.framework.model.SimpleLocation;

public class ConfigHelper {
  private static final Location CONFIG_LOCATION = new SimpleLocation("gapic config");

  private final DiagCollector diag;

  public ConfigHelper(DiagCollector diag) {
    this.diag = diag;
  }

  public void error(String message, Object... params) {
    error(CONFIG_LOCATION, message, params);
  }

  public void error(Location location, String message, Object... params) {
    diag.addDiag(Diag.error(location, message, params));
  }
}

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
package com.google.api.codegen.configgen;

import com.google.api.tools.framework.model.Location;
import com.google.api.tools.framework.model.SimpleLocation;

/** Implements LocationGenerator for a refresh config. */
public class RefreshConfigLocationGenerator implements LocationGenerator {
  private final String fileName;

  public RefreshConfigLocationGenerator(String fileName) {
    this.fileName = fileName;
  }

  @Override
  public Location getLocation() {
    return getLocation("?");
  }

  @Override
  public Location getLocation(Object line) {
    return new SimpleLocation(String.format("%s:%s", fileName, line), fileName);
  }
}

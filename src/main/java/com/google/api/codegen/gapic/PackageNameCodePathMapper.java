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
package com.google.api.codegen.gapic;

import com.google.api.codegen.config.ApiConfig;
import com.google.api.tools.framework.model.ProtoElement;

/**
 * An implementation of GapicCodePathMapper where the output path is simply the package name.
 */
public class PackageNameCodePathMapper implements GapicCodePathMapper {
  @Override
  public String getOutputPath(ProtoElement element, ApiConfig config) {
    return config.getPackageName();
  }
}

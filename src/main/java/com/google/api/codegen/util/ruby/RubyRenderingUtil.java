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
package com.google.api.codegen.util.ruby;

import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.common.base.Splitter;
import java.util.List;

public class RubyRenderingUtil {
  public Iterable<String> getApiModules(String packageName) {
    return Splitter.on("::").splitToList(packageName);
  }

  public List<String> getDocLines(String text) {
    return CommonRenderingUtil.getDocLines(text);
  }
}

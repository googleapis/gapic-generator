/* Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.util.ruby;

import com.google.api.codegen.util.CommonRenderingUtil;
import java.util.List;

public class RubyRenderingUtil {

  /** @see CommonRenderingUtil#getDocLines(String) */
  public List<String> getDocLines(String text) {
    return CommonRenderingUtil.getDocLines(text);
  }

  /** @see CommonRenderingUtil#getDocLines(String, int) */
  public List<String> getDocLines(String text, int maxWidth) {
    return CommonRenderingUtil.getDocLines(text, maxWidth);
  }

  /** @see CommonRenderingUtil#padding(int) */
  public String padding(int width) {
    return CommonRenderingUtil.padding(width);
  }

  /** @see CommonRenderingUtil#toInt(String) */
  public int toInt(String value) {
    return CommonRenderingUtil.toInt(value);
  }

  public static String wrapReserved(String name) {
    return RubyNameFormatter.wrapIfKeywordOrBuiltIn(name);
  }
}

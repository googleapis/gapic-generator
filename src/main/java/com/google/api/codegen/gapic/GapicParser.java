/* Copyright 2017 Google LLC
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
package com.google.api.codegen.gapic;

import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;

/**
 * Util class for parsing ProtoElements into canonical inputs for the gapic transformer pipeline.
 */
public class GapicParser {

  /** Provides the doc lines for the given proto element in the current language. */
  public static String getDocString(ProtoElement element) {
    return DocumentationUtil.getScopedDescription(element);
  }

  /** Get the url to the protobuf file located in github. */
  public static String getFileUrl(ProtoFile file) {
    String filePath = file.getSimpleName();
    if (filePath.startsWith("google/protobuf")) {
      return "https://github.com/google/protobuf/blob/master/src/" + filePath;
    } else {
      return "https://github.com/googleapis/googleapis/blob/master/" + filePath;
    }
  }
}

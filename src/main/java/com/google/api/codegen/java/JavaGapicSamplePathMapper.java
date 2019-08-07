/* Copyright 2019 Google LLC
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
package com.google.api.codegen.java;

import com.google.api.codegen.config.ProductConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.java.JavaSurfaceNamer;
import com.google.common.base.Joiner;
import java.util.ArrayList;

/** The PathMapper for Java standalone samples. */
public class JavaGapicSamplePathMapper implements GapicCodePathMapper {

  @Override
  public String getOutputPath(String elementFullName, ProductConfig config) {
    ArrayList<String> dirs = new ArrayList<>();
    dirs.add("samples/src/main/java");
    String samplePackageName = JavaSurfaceNamer.getExamplePackageName(config.getPackageName());
    for (String seg : samplePackageName.split("\\.")) {
      dirs.add(seg.toLowerCase());
    }
    return Joiner.on("/").join(dirs);
  }

  // TODO(hzyi): Deprecate this method.
  // The method was originally added to the interface `GapicCodePathMapper` because `method` was
  // needed. However, the method name is no longer needed in any of the seven languages we support,
  // and therefore we can reuse `getOutputPath` to calculate output path for sample generation.
  // This means we cannot use one instance of `GapicCodePathMapper` for both codegen and samplegen,
  // but that is fine.
  public String getSamplesOutputPath(String elementFullName, ProductConfig config, String method) {
    throw new UnsupportedOperationException("Deprecated: use getOutputPath instead.");
  }
}

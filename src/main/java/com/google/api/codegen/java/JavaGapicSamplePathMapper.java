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
}

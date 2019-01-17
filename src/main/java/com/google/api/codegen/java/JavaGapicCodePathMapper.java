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
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import java.util.ArrayList;

/**
 * An implementation of GapicCodePathMapper that generates the output path from a prefix, and/or
 * package name.
 */
@AutoValue
public abstract class JavaGapicCodePathMapper implements GapicCodePathMapper {

  private final String PACKAGE_SPLITTER = "\\.";

  public abstract String prefix();

  @Override
  public String getOutputPath(String elementFullName, ProductConfig config) {
    return getOutputPath(config, null);
  }

  @Override
  public String getSamplesOutputPath(String elementFullName, ProductConfig config, String method) {
    return getOutputPath(config, method);
  }

  /*
   * If the methodName is not null, returns the path for generated samples for this method.
   * Otherwise, returns the path for the client library classes.
   */
  private String getOutputPath(ProductConfig config, String methodName) {
    ArrayList<String> dirs = new ArrayList<>();
    boolean hasSample = !Strings.isNullOrEmpty(methodName);
    if (hasSample) {
      dirs.add(SAMPLES_DIRECTORY);
      dirs.add(prefix());
      for (String seg :
          JavaSurfaceNamer.getExamplePackageName(config.getPackageName()).split(PACKAGE_SPLITTER)) {
        dirs.add(seg.toLowerCase());
      }

      // The Java package name at the top of each sample file must match the output path for
      // that file. This means that since we want the output path to eventually be of the form
      // `samples/src/main/java/com/google/cloud/examples/API_NAME/VER/METHOD_NAME/SAMPLE_NAME.java`,
      // we need to make `methodName` be the last element of the Java package name at the top of the
      // sample file. This is non-trivial to do, so we'll do it separately. Once we do it, we can
      // then make `methodName` be the final path element for this output path so that they continue
      // to match by calling `dirs.add(methodName)` here.
    } else {
      dirs.add(prefix());
      for (String seg : config.getPackageName().split(PACKAGE_SPLITTER)) {
        dirs.add(seg.toLowerCase());
      }
    }
    return Joiner.on("/").join(dirs);
  }

  public static Builder newBuilder() {
    return new AutoValue_JavaGapicCodePathMapper.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder prefix(String prefix);

    public abstract JavaGapicCodePathMapper build();
  }
}

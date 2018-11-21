/* Copyright 2016 Google LLC
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
package com.google.api.codegen.php;

import com.google.api.codegen.config.ProductConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.util.php.PhpPackageUtil;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Arrays;
import javax.annotation.Nullable;

/**
 * An implementation of GapicCodePathMapper that generates the output path from a prefix, and/or
 * package name.
 */
@AutoValue
public abstract class PhpGapicCodePathMapper implements GapicCodePathMapper {

  @Nullable
  public abstract String getPrefix();

  @Nullable
  public abstract String getSuffix();

  @Override
  public String getOutputPath(String elementFullName, ProductConfig config) {
    return getOutputPath(config, null);
  }

  @Override
  public String getSamplesOutputPath(String elementFullName, ProductConfig config, String method) {
    return getOutputPath(config, method);
  }

  private String getOutputPath(ProductConfig config, String methodSample) {
    ArrayList<String> dirs = new ArrayList<>();
    String prefix = getPrefix();
    boolean haveSample = !Strings.isNullOrEmpty(methodSample);

    if (!Strings.isNullOrEmpty(prefix)) {
      dirs.add(prefix);
    }

    String shortenedPackageName = PhpPackageUtil.removeBasePackageName(config.getPackageName());
    if (shortenedPackageName != null) {
      dirs.addAll(Arrays.asList(PhpPackageUtil.splitPackageName(shortenedPackageName)));
    }

    if (haveSample) {
      dirs.add(SAMPLES_DIRECTORY);
    }

    String suffix = getSuffix();
    if (!Strings.isNullOrEmpty(suffix)) {
      dirs.add(suffix);
    }

    if (haveSample) {
      dirs.add(methodSample);
    }
    return Joiner.on("/").join(dirs);
  }

  public static Builder newBuilder() {
    return new AutoValue_PhpGapicCodePathMapper.Builder();
  }

  public static PhpGapicCodePathMapper defaultInstance() {
    return newBuilder().build();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setPrefix(String prefix);

    public abstract Builder setSuffix(String suffix);

    public abstract PhpGapicCodePathMapper build();
  }
}

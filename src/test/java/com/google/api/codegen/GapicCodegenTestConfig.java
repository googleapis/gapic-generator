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
package com.google.api.codegen;

import com.google.api.Service;
import com.google.api.codegen.common.CodeGenerator;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.gapic.ArtifactFlags;
import com.google.api.codegen.gapic.GapicGeneratorFactory;
import com.google.api.tools.framework.model.Model;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

@AutoValue
public abstract class GapicCodegenTestConfig {

  private static final Model model = Model.create(Service.getDefaultInstance());
  private static final GapicProductConfig productConfig = GapicProductConfig.createDummyInstance();
  private static final PackageMetadataConfig packageConfig =
      PackageMetadataConfig.createDummyPackageMetadataConfig();
  private static final ArtifactFlags artifactFlags =
      new ArtifactFlags(
          Arrays.asList("surface", "test", "samples"), ArtifactType.LEGACY_GAPIC_AND_PACKAGE, true);

  public abstract TargetLanguage targetLanguage();

  public abstract ImmutableList<String> gapicConfigFileNames();

  @Nullable
  public abstract String packageConfigFileName();

  public abstract String apiName();

  @Nullable
  public abstract String protoPackage();

  @Nullable
  public abstract String clientPackage();

  @Nullable
  public abstract String grpcServiceConfigFileName();

  public abstract ImmutableList<String> sampleConfigFileNames();

  @Nullable
  public abstract String baseline();

  public abstract ImmutableList<String> baseNames();

  public List<String> snippetNames() {
    List<CodeGenerator<?>> generators =
        GapicGeneratorFactory.create(
            targetLanguage(), model, productConfig, packageConfig, artifactFlags);
    return generators
        .stream()
        .flatMap(g -> g.getInputFileNames().stream())
        .collect(Collectors.toList());
  }

  public static Builder newBuilder() {
    return new AutoValue_GapicCodegenTestConfig.Builder()
        .gapicConfigFileNames(ImmutableList.of())
        .sampleConfigFileNames(ImmutableList.of())
        .baseNames(ImmutableList.of());
  }

  public Object[] toParameterizedTestInput() {
    return new Object[] {this, baseline()};
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder targetLanguage(TargetLanguage val);

    abstract TargetLanguage targetLanguage();

    public abstract Builder gapicConfigFileNames(ImmutableList<String> val);

    abstract ImmutableList<String> gapicConfigFileNames();

    public abstract Builder packageConfigFileName(String val);

    public abstract Builder apiName(String val);

    abstract String apiName();

    public abstract Builder protoPackage(String val);

    public abstract Builder clientPackage(String val);

    public abstract Builder grpcServiceConfigFileName(String val);

    abstract String grpcServiceConfigFileName();

    public abstract Builder sampleConfigFileNames(ImmutableList<String> val);

    public abstract Builder baseline(String val);

    abstract String baseline();

    public abstract Builder baseNames(ImmutableList<String> val);

    abstract ImmutableList<String> baseNames();

    public abstract GapicCodegenTestConfig autoBuild();

    public GapicCodegenTestConfig build() {
      // prepend API name to baseNames
      baseNames(ImmutableList.<String>builder().add(apiName()).addAll(baseNames()).build());

      // generate a baseline filename if not provided
      String baseline = baseline();
      if (baseline == null) {
        StringBuilder suffix = new StringBuilder();
        if (gapicConfigFileNames() == null || gapicConfigFileNames().size() == 0) {
          suffix.append("_no_gapic_config");
        }

        if (!Strings.isNullOrEmpty(grpcServiceConfigFileName())) {
          suffix.append("_with_grpc_service_config");
        }

        baseline =
            targetLanguage().toString().toLowerCase() + "_" + apiName() + suffix + ".baseline";
      }
      baseline(baseline);
      return autoBuild();
    }
  }
}

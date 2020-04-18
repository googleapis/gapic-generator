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
package com.google.api.codegen.config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.packagegen.PackagingArtifactType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class PackagingConfigTest {
  private final Map<String, ? extends InterfaceConfig> interfaceConfigMap;
  private final PackagingConfig expectedPackagingConfig;

  public PackagingConfigTest(
      String name, String interfaceName, boolean hasIam, PackagingConfig expectedPackagingConfig) {
    InterfaceConfig interfaceConfig = mock(InterfaceConfig.class);
    when(interfaceConfig.hasIamMethods()).thenReturn(hasIam);
    this.interfaceConfigMap = ImmutableMap.of(interfaceName, interfaceConfig);

    this.expectedPackagingConfig = expectedPackagingConfig;
  }

  @Test
  public void testLoadFromProductConfig() {

    PackagingConfig packagingConfig = null;
    try {
      packagingConfig = PackagingConfig.loadFromProductConfig(interfaceConfigMap);
    } catch (IllegalArgumentException e) {
      // Is thrown if input is not parsable, expectedConfig is null in this case
    }

    assertThat(packagingConfig).isEqualTo(expectedPackagingConfig);
  }

  @Parameters(name = "PackagingConfigTest {index} {0}")
  public static List<Object[]> parameters() {
    ImmutableList.Builder<Object[]> params = ImmutableList.builder();

    PackagingConfig expected;

    // 0
    expected =
        config(
            "pubsub",
            "v1",
            "google-cloud",
            "google/pubsub/v1",
            true,
            ReleaseLevel.UNSET_RELEASE_LEVEL);
    params.add(param("google.pubsub.v1.Subscriber", true, expected));

    // 1
    expected =
        config(
            "longrunning",
            "v1",
            "google-cloud",
            "google/longrunning",
            false,
            ReleaseLevel.UNSET_RELEASE_LEVEL);
    params.add(param("google.longrunning.Operations", false, expected));

    // 2
    expected =
        config(
            "vision",
            "v1p4beta1",
            "google-cloud",
            "google/cloud/vision/v1p4beta1",
            false,
            ReleaseLevel.BETA);
    params.add(param("google.cloud.vision.v1p4beta1.ProductSearch", false, expected));

    // 3
    expected =
        config(
            "vision",
            "v1p4beta",
            "google-cloud",
            "google/cloud/vision/v1p4beta",
            false,
            ReleaseLevel.BETA);
    params.add(param("google.cloud.vision.v1p4beta.ProductSearch", false, expected));

    // 4
    expected =
        config(
            "vision",
            "v1alpha2",
            "google-cloud",
            "google/cloud/vision/v1alpha2",
            false,
            ReleaseLevel.ALPHA);
    params.add(param("google.cloud.vision.v1alpha2.ProductSearch", false, expected));

    // 5
    expected =
        config(
            "vision",
            "v1beta1",
            "google-cloud",
            "google/cloud/vision/v1beta1",
            false,
            ReleaseLevel.BETA);
    params.add(param("google.cloud.vision.v1beta1.ProductSearch", false, expected));

    // 6
    expected =
        config(
            "home-graph",
            "v1",
            "google-cloud",
            "google/home/graph/v1",
            false,
            ReleaseLevel.UNSET_RELEASE_LEVEL);
    params.add(param("google.home.graph.v1.HomeGraphApiService", false, expected));

    // 7
    expected =
        config(
            "streetview-publish",
            "v1",
            "google-cloud",
            "google/streetview/publish/v1",
            false,
            ReleaseLevel.UNSET_RELEASE_LEVEL);
    params.add(param("google.streetview.publish.v1.StreetViewPublishService", false, expected));

    // 8
    expected =
        config(
            "videointelligence",
            "beta1",
            "google-cloud",
            "google/cloud/videointelligence/beta1",
            false,
            ReleaseLevel.BETA);
    params.add(
        param("google.cloud.videointelligence.beta1.VideoIntelligenceService", false, expected));

    // 9
    expected =
        config(
            "iam-admin",
            "v1",
            "google-cloud",
            "google/iam/admin/v1",
            false,
            ReleaseLevel.UNSET_RELEASE_LEVEL);
    params.add(param("google.iam.admin.v1.IAM", true, expected));

    // 10
    expected =
        config(
            "<wrong_input>",
            "v1",
            "google-cloud",
            "<wrong_input>",
            false,
            ReleaseLevel.UNSET_RELEASE_LEVEL);
    params.add(param("<wrong_input>", false, expected));

    // 11
    expected =
        config(
            "bigquery-datatransfer",
            "v1",
            "google-cloud",
            "google/cloud/bigquery/datatransfer/v1",
            false,
            ReleaseLevel.UNSET_RELEASE_LEVEL);
    params.add(param("google.cloud.bigquery.datatransfer.v1.DataTransferService", false, expected));

    // 12
    expected =
        config(
            "bigtable-admin",
            "v2",
            "google-cloud",
            "google/bigtable/admin/v2",
            false,
            ReleaseLevel.UNSET_RELEASE_LEVEL);
    params.add(param("google.bigtable.admin.v2.BigtableTableAdmin", false, expected));

    // 13
    expected =
        config(
            "grafeas", "v1", "google-cloud", "grafeas/v1", false, ReleaseLevel.UNSET_RELEASE_LEVEL);
    params.add(param("grafeas.v1.Grafeas", false, expected));

    return params.build();
  }

  private static PackagingConfig config(
      String apiName,
      String apiVersion,
      String organizationName,
      String protoPath,
      boolean hasIam,
      ReleaseLevel releaseLevel) {

    ImmutableList.Builder<String> deps = ImmutableList.builder();
    ImmutableList.Builder<String> testDeps = ImmutableList.builder();
    deps.add("google-common-protos");
    if (hasIam) {
      deps.add("google-iam-v1");
      testDeps.add("google-iam-v1");
    }

    return PackagingConfig.newBuilder()
        .apiName(apiName)
        .apiVersion(apiVersion)
        .organizationName(organizationName)
        .protoPackageDependencies(deps.build())
        .protoPackageTestDependencies(testDeps.build())
        .releaseLevel(releaseLevel)
        .artifactType(PackagingArtifactType.GAPIC)
        .protoPath(protoPath)
        .build();
  }

  private static Object[] param(String interfaceName, boolean hasIam, PackagingConfig config) {
    return new Object[] {
      interfaceName.replaceAll("(\\.|::|\\\\|\\/)", "_"), interfaceName, hasIam, config
    };
  }
}

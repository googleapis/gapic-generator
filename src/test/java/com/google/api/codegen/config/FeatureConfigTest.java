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

import com.google.api.codegen.transformer.csharp.CSharpFeatureConfig;
import com.google.api.codegen.transformer.java.JavaFeatureConfig;
import org.junit.Test;
import org.mockito.Mockito;

public class FeatureConfigTest {
  private GapicProductConfig productConfig = Mockito.mock(GapicProductConfig.class);
  private ResourceNameMessageConfigs resourceNameMessageConfigs =
      Mockito.mock(ResourceNameMessageConfigs.class);

  @Test
  public void testEnableStringFormatFunctionsOverride() {
    Mockito.doReturn(resourceNameMessageConfigs)
        .when(productConfig)
        .getResourceNameMessageConfigs();

    Mockito.doReturn(null).when(productConfig).enableStringFormattingFunctionsOverride();

    Mockito.doReturn(true).when(resourceNameMessageConfigs).isEmpty();
    assertThat(JavaFeatureConfig.create(productConfig).enableStringFormatFunctions()).isTrue();

    Mockito.doReturn(false).when(resourceNameMessageConfigs).isEmpty();
    assertThat(JavaFeatureConfig.create(productConfig).enableStringFormatFunctions()).isFalse();

    Mockito.doReturn(true).when(productConfig).enableStringFormattingFunctionsOverride();
    assertThat(JavaFeatureConfig.create(productConfig).enableStringFormatFunctions()).isTrue();

    Mockito.doReturn(true).when(resourceNameMessageConfigs).isEmpty();
    assertThat(JavaFeatureConfig.create(productConfig).enableStringFormatFunctions()).isTrue();

    CSharpFeatureConfig cSharpFeatureConfig = new CSharpFeatureConfig();
    assertThat(cSharpFeatureConfig.enableStringFormatFunctions()).isTrue();
  }
}

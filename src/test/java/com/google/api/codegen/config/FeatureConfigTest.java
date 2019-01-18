package com.google.api.codegen.config;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.csharp.CSharpFeatureConfig;
import com.google.api.codegen.transformer.java.JavaFeatureConfig;
import javax.swing.text.GapContent;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class FeatureConfigTest {
  private GapicProductConfig productConfig = Mockito.mock(GapicProductConfig.class);
  private ResourceNameMessageConfigs resourceNameMessageConfigs = Mockito.mock(ResourceNameMessageConfigs.class);

  @Test
  public void testEnableStringFormatFunctionsOverride() {
    Mockito.doReturn(resourceNameMessageConfigs).when(productConfig).getResourceNameMessageConfigs();

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

package com.google.api.codegen.config;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class FlatteningConfigTest {

  private static FieldModel repeatedField = Mockito.mock(FieldModel.class);
  private static FieldConfig repeatedFieldConfig = FieldConfig.createDefaultFieldConfig(repeatedField);

  private static FieldModel optionalSingularField = Mockito.mock(FieldModel.class);
  private static FieldConfig optionalSingularFieldConfig = FieldConfig.createDefaultFieldConfig(optionalSingularField);

  private static FieldModel requiredField = Mockito.mock(FieldModel.class);
  private static FieldConfig requiredFieldConfig = FieldConfig.createDefaultFieldConfig(requiredField);

  private static FlatteningConfig emptyFlattening = Mockito.mock(FlatteningConfig.class);

  @BeforeClass
  public static void setUp() {
    Mockito.when(repeatedField.isRepeated()).thenReturn(true);
    Mockito.when(requiredField.isRequired()).thenReturn(true);
    // Calling isRepeated and isRequired on mocks will default to "false" if not stubbed.

    // Only stub the construction of the FlatteningConfigs.
    Mockito.when(emptyFlattening.getFlattenedFieldConfigs()).thenReturn(
        ImmutableMap.of());
    Mockito.when(emptyFlattening.validateNoNonTerminalRepeatedField()).thenCallRealMethod();
    Mockito.when(emptyFlattening.validateRequiredArgumentsFirst()).thenCallRealMethod();
  }

  @Test
  public void testValidateRequiredArguments() {

    assertThat(emptyFlattening.validateRequiredArgumentsFirst()).isTrue();

    FlatteningConfig allRequiredFields = Mockito.mock(FlatteningConfig.class);
    Mockito.when(allRequiredFields.validateRequiredArgumentsFirst()).thenCallRealMethod();
    Mockito.when(allRequiredFields.getFlattenedFieldConfigs()).thenReturn(
        ImmutableMap.of(
            "required", requiredFieldConfig,
            "required2", requiredFieldConfig,
            "required3", requiredFieldConfig));
    assertThat(allRequiredFields.validateRequiredArgumentsFirst()).isTrue();

    FlatteningConfig allOptionalFields = Mockito.mock(FlatteningConfig.class);
    Mockito.when(allOptionalFields.validateRequiredArgumentsFirst()).thenCallRealMethod();
    Mockito.when(allOptionalFields.getFlattenedFieldConfigs()).thenReturn(
        ImmutableMap.of(
            "optional", optionalSingularFieldConfig,
            "optional2", optionalSingularFieldConfig,
            "optional3", optionalSingularFieldConfig));
    assertThat(allOptionalFields.validateRequiredArgumentsFirst()).isTrue();

    FlatteningConfig mixedFieldsTrue = Mockito.mock(FlatteningConfig.class);
    Mockito.when(mixedFieldsTrue.validateRequiredArgumentsFirst()).thenCallRealMethod();
    Mockito.when(mixedFieldsTrue.getFlattenedFieldConfigs()).thenReturn(
        ImmutableMap.of(
            "required", requiredFieldConfig,
            "optional2", optionalSingularFieldConfig,
            "optional3", optionalSingularFieldConfig));
    assertThat(mixedFieldsTrue.validateRequiredArgumentsFirst()).isTrue();

    FlatteningConfig mixedFieldsFalse = Mockito.mock(FlatteningConfig.class);
    Mockito.when(mixedFieldsFalse.validateRequiredArgumentsFirst()).thenCallRealMethod();
    Mockito.when(mixedFieldsFalse.getFlattenedFieldConfigs()).thenReturn(
        ImmutableMap.of(
            "required", requiredFieldConfig,
            "optional2", optionalSingularFieldConfig,
            "required3", requiredFieldConfig));
    assertThat(mixedFieldsFalse.validateRequiredArgumentsFirst()).isFalse();
  }

  @Test
  public void testValidateNoNonTerminalRepeatedField() {
    assertThat(emptyFlattening.validateNoNonTerminalRepeatedField()).isTrue();

    FlatteningConfig allRepeatedFields = Mockito.mock(FlatteningConfig.class);
    Mockito.when(allRepeatedFields.validateNoNonTerminalRepeatedField()).thenCallRealMethod();
    Mockito.when(allRepeatedFields.getFlattenedFieldConfigs()).thenReturn(
        ImmutableMap.of(
            "repeated", repeatedFieldConfig,
            "repeated2", repeatedFieldConfig,
            "repeated3", repeatedFieldConfig));
    assertThat(allRepeatedFields.validateNoNonTerminalRepeatedField()).isFalse();

    FlatteningConfig allOptionalFields = Mockito.mock(FlatteningConfig.class);
    Mockito.when(allOptionalFields.validateNoNonTerminalRepeatedField()).thenCallRealMethod();
    Mockito.when(allOptionalFields.getFlattenedFieldConfigs()).thenReturn(
        ImmutableMap.of(
            "optional", optionalSingularFieldConfig,
            "optional2", optionalSingularFieldConfig,
            "optional3", optionalSingularFieldConfig));
    assertThat(allOptionalFields.validateNoNonTerminalRepeatedField()).isTrue();

    FlatteningConfig mixedFieldsTrue = Mockito.mock(FlatteningConfig.class);
    Mockito.when(mixedFieldsTrue.validateNoNonTerminalRepeatedField()).thenCallRealMethod();
    Mockito.when(mixedFieldsTrue.getFlattenedFieldConfigs()).thenReturn(
        ImmutableMap.of(
            "optional", optionalSingularFieldConfig,
            "optional2", optionalSingularFieldConfig,
            "required3", repeatedFieldConfig));
    assertThat(mixedFieldsTrue.validateNoNonTerminalRepeatedField()).isTrue();

    FlatteningConfig mixedFieldsFalse = Mockito.mock(FlatteningConfig.class);
    Mockito.when(mixedFieldsFalse.validateNoNonTerminalRepeatedField()).thenCallRealMethod();
    Mockito.doReturn(ImmutableMap.of(
        "repeated", repeatedFieldConfig,
        "optional2", optionalSingularFieldConfig,
        "repeated3", repeatedFieldConfig)).when(mixedFieldsFalse).getFlattenedFieldConfigs();
    assertThat(mixedFieldsFalse.validateNoNonTerminalRepeatedField()).isFalse();
  }

}

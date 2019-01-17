package com.google.api.codegen.config;

import com.google.common.collect.ImmutableMap;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class FlatteningConfigTest {

  @Rule public final ExpectedException exception = ExpectedException.none();

  private static FieldModel repeatedField = Mockito.mock(FieldModel.class);
  private static FieldConfig repeatedFieldConfig =
      FieldConfig.createDefaultFieldConfig(repeatedField);

  private static FieldModel optionalSingularField = Mockito.mock(FieldModel.class);
  private static FieldConfig optionalSingularFieldConfig =
      FieldConfig.createDefaultFieldConfig(optionalSingularField);

  private static FieldModel requiredField = Mockito.mock(FieldModel.class);
  private static FieldConfig requiredFieldConfig =
      FieldConfig.createDefaultFieldConfig(requiredField);

  private static FlatteningConfig emptyFlattening = Mockito.mock(FlatteningConfig.class);

  @BeforeClass
  public static void setUp() {
    Mockito.when(repeatedField.isRepeated()).thenReturn(true);
    Mockito.when(requiredField.isRequired()).thenReturn(true);
    // Calling isRepeated and isRequired on mocks will default to "false" if not stubbed.

    // Only stub the construction of the FlatteningConfigs.
    Mockito.when(emptyFlattening.getFlattenedFieldConfigs()).thenReturn(ImmutableMap.of());
    Mockito.doCallRealMethod().when(emptyFlattening).validateNoNonTerminalRepeatedField();
    Mockito.doCallRealMethod().when(emptyFlattening).validateRequiredArgumentsFirst();
  }

  @Test
  public void testValidateRequiredArguments() {
    emptyFlattening.validateRequiredArgumentsFirst();
  }

  @Test
  public void testValidateRequiredArgumentsAllRequired() {
    FlatteningConfig allRequiredFields = Mockito.mock(FlatteningConfig.class);
    Mockito.doCallRealMethod().when(allRequiredFields).validateRequiredArgumentsFirst();
    Mockito.when(allRequiredFields.getFlattenedFieldConfigs())
        .thenReturn(
            ImmutableMap.of(
                "required", requiredFieldConfig,
                "required2", requiredFieldConfig,
                "required3", requiredFieldConfig));
    allRequiredFields.validateRequiredArgumentsFirst();
  }

  @Test
  public void testValidateRequiredArgumentsAllOptional() {
    FlatteningConfig allOptionalFields = Mockito.mock(FlatteningConfig.class);
    Mockito.doCallRealMethod().when(allOptionalFields).validateRequiredArgumentsFirst();
    Mockito.when(allOptionalFields.getFlattenedFieldConfigs())
        .thenReturn(
            ImmutableMap.of(
                "optional", optionalSingularFieldConfig,
                "optional2", optionalSingularFieldConfig,
                "optional3", optionalSingularFieldConfig));
    allOptionalFields.validateRequiredArgumentsFirst();
  }

  @Test
  public void testValidateRequiredArgumentsMixedTrue() {
    FlatteningConfig mixedFieldsTrue = Mockito.mock(FlatteningConfig.class);
    Mockito.doCallRealMethod().when(mixedFieldsTrue).validateRequiredArgumentsFirst();
    Mockito.when(mixedFieldsTrue.getFlattenedFieldConfigs())
        .thenReturn(
            ImmutableMap.of(
                "required", requiredFieldConfig,
                "optional2", optionalSingularFieldConfig,
                "optional3", optionalSingularFieldConfig));
    mixedFieldsTrue.validateRequiredArgumentsFirst();
  }

  @Test
  public void testValidateRequiredArgumentsMixedFalse() {
    FlatteningConfig mixedFieldsFalse = Mockito.mock(FlatteningConfig.class);
    Mockito.doCallRealMethod().when(mixedFieldsFalse).validateRequiredArgumentsFirst();
    Mockito.when(mixedFieldsFalse.getFlattenedFieldConfigs())
        .thenReturn(
            ImmutableMap.of(
                "required", requiredFieldConfig,
                "optional2", optionalSingularFieldConfig,
                "required3", requiredFieldConfig));

    exception.expect(IllegalArgumentException.class);

    mixedFieldsFalse.validateRequiredArgumentsFirst();
  }

  @Test
  public void testValidateRepeatedConditionOnEmpty() {
    emptyFlattening.validateNoNonTerminalRepeatedField();
  }

  @Test
  public void testValidateRepeatedConditionOnAllRepeated() {
    FlatteningConfig allRepeatedFields = Mockito.mock(FlatteningConfig.class);
    Mockito.doCallRealMethod().when(allRepeatedFields).validateNoNonTerminalRepeatedField();
    Mockito.when(allRepeatedFields.getFlattenedFieldConfigs())
        .thenReturn(
            ImmutableMap.of(
                "repeated", repeatedFieldConfig,
                "repeated2", repeatedFieldConfig,
                "repeated3", repeatedFieldConfig));

    exception.expect(IllegalArgumentException.class);

    allRepeatedFields.validateNoNonTerminalRepeatedField();
  }

  @Test
  public void testValidateRepeatedConditionOnAllOptional() {
    FlatteningConfig allOptionalFields = Mockito.mock(FlatteningConfig.class);
    Mockito.doCallRealMethod().when(allOptionalFields).validateNoNonTerminalRepeatedField();
    Mockito.when(allOptionalFields.getFlattenedFieldConfigs())
        .thenReturn(
            ImmutableMap.of(
                "optional", optionalSingularFieldConfig,
                "optional2", optionalSingularFieldConfig,
                "optional3", optionalSingularFieldConfig));

    allOptionalFields.validateNoNonTerminalRepeatedField();
  }

  @Test
  public void testValidateRepeatedConditionOnAllLastRequired() {
    FlatteningConfig mixedFieldsTrue = Mockito.mock(FlatteningConfig.class);
    Mockito.doCallRealMethod().when(mixedFieldsTrue).validateNoNonTerminalRepeatedField();
    Mockito.when(mixedFieldsTrue.getFlattenedFieldConfigs())
        .thenReturn(
            ImmutableMap.of(
                "optional", optionalSingularFieldConfig,
                "optional2", optionalSingularFieldConfig,
                "required3", repeatedFieldConfig));

    mixedFieldsTrue.validateNoNonTerminalRepeatedField();
  }

  @Test
  public void testValidateRepeatedConditionOnMixedFalse() {
    FlatteningConfig mixedFieldsFalse = Mockito.mock(FlatteningConfig.class);
    Mockito.doCallRealMethod().when(mixedFieldsFalse).validateNoNonTerminalRepeatedField();
    Mockito.doReturn(
            ImmutableMap.of(
                "repeated", repeatedFieldConfig,
                "optional2", optionalSingularFieldConfig,
                "repeated3", repeatedFieldConfig))
        .when(mixedFieldsFalse)
        .getFlattenedFieldConfigs();

    exception.expect(IllegalArgumentException.class);

    mixedFieldsFalse.validateNoNonTerminalRepeatedField();
  }
}

/* Copyright 2018 Google LLC
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

import com.google.api.codegen.LongRunningConfigProto;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.BoundedDiagCollector;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.SymbolTable;
import com.google.api.tools.framework.model.TypeRef;
import com.google.longrunning.OperationTypes;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class LongRunningConfigTest {
  private static final String GAPIC_CONFIG_RETURN_TYPE_NAME = "MethodResponse";
  private static final String GAPIC_CONFIG_METADATA_TYPE = "HeaderType";
  private static final String ANNOTATIONS_RETURN_TYPE_NAME = "BookType";
  private static final String ANNOTATIONS_METADATA_TYPE = "FooterType";
  private static boolean TEST_IMPLEMENTS_DELETE = false;
  private static boolean TEST_IMPLEMENTS_CANCEL = false;
  private static int TEST_INITIAL_POLL_DELAY = 5;
  private static double TEST_POLL_DELAY_MULTIPLIER = 10;
  private static long TEST_MAX_POLL_DELAY = 12500;
  private static int TEST_TOTAL_POLL_TIMEOUT = 50000;

  private static final ProtoParser protoParser = Mockito.mock(ProtoParser.class);

  private static final Method simpleMethod = Mockito.mock(Method.class);
  private static final Method lroAnnotatedMethod = Mockito.mock(Method.class);

  private static final Model model = Mockito.mock(Model.class);
  private static final SymbolTable symbolTable = Mockito.mock(SymbolTable.class);

  private static final MessageType gapicConfigMetadataMessage = Mockito.mock(MessageType.class);
  private static final MessageType gapicConfigReturnMessage = Mockito.mock(MessageType.class);
  private static final MessageType annotationsMetadataMessage = Mockito.mock(MessageType.class);
  private static final MessageType annotationsReturnMessage = Mockito.mock(MessageType.class);

  private static final TypeRef gapicConfigMetadataType = TypeRef.of(gapicConfigMetadataMessage);
  private static final TypeRef gapicConfigReturnType = TypeRef.of(gapicConfigReturnMessage);
  private static final TypeRef annotationsMetadataType = TypeRef.of(annotationsMetadataMessage);
  private static final TypeRef annotationsReturnType = TypeRef.of(annotationsReturnMessage);

  private static final LongRunningConfigProto baseLroConfigProto =
      LongRunningConfigProto.newBuilder()
          .setMetadataType(GAPIC_CONFIG_METADATA_TYPE)
          .setReturnType(GAPIC_CONFIG_RETURN_TYPE_NAME)
          .build();
  private static final LongRunningConfigProto lroConfigProtoWithPollSettings =
      baseLroConfigProto
          .toBuilder()
          .setImplementsCancel(TEST_IMPLEMENTS_CANCEL)
          .setImplementsDelete(TEST_IMPLEMENTS_DELETE)
          .setInitialPollDelayMillis(TEST_INITIAL_POLL_DELAY)
          .setPollDelayMultiplier(TEST_POLL_DELAY_MULTIPLIER)
          .setMaxPollDelayMillis(TEST_MAX_POLL_DELAY)
          .setTotalPollTimeoutMillis(TEST_TOTAL_POLL_TIMEOUT)
          .build();

  @BeforeClass
  public static void startUp() {
    Mockito.when(simpleMethod.getModel()).thenReturn(model);
    Mockito.when(lroAnnotatedMethod.getModel()).thenReturn(model);
    Mockito.when(model.getSymbolTable()).thenReturn(symbolTable);

    Mockito.when(protoParser.getLongRunningOperation(lroAnnotatedMethod))
        .thenReturn(
            OperationTypes.newBuilder()
                .setMetadata(ANNOTATIONS_METADATA_TYPE)
                .setResponse(ANNOTATIONS_RETURN_TYPE_NAME)
                .build());

    Mockito.when(symbolTable.lookupType(GAPIC_CONFIG_METADATA_TYPE))
        .thenReturn(gapicConfigMetadataType);
    Mockito.when(symbolTable.lookupType(GAPIC_CONFIG_RETURN_TYPE_NAME))
        .thenReturn(gapicConfigReturnType);
    Mockito.when(symbolTable.lookupType(ANNOTATIONS_METADATA_TYPE))
        .thenReturn(annotationsMetadataType);
    Mockito.when(symbolTable.lookupType(ANNOTATIONS_RETURN_TYPE_NAME))
        .thenReturn(annotationsReturnType);
  }

  @Test
  public void testCreateLROWithoutGapicConfig() {
    DiagCollector diagCollector = new BoundedDiagCollector();
    LongRunningConfig longRunningConfig =
        LongRunningConfig.createLongRunningConfig(
            lroAnnotatedMethod,
            diagCollector,
            LongRunningConfigProto.getDefaultInstance(),
            protoParser);

    assertThat(diagCollector.getErrorCount()).isEqualTo(0);
    assertThat(longRunningConfig).isNotNull();

    ProtoTypeRef metadataTypeModel = (ProtoTypeRef) longRunningConfig.getMetadataType();
    assertThat(metadataTypeModel.getProtoType()).isEqualTo(annotationsMetadataType);
    ProtoTypeRef returnTypeModel = (ProtoTypeRef) longRunningConfig.getReturnType();
    assertThat(returnTypeModel.getProtoType()).isEqualTo(annotationsReturnType);

    assertThat(longRunningConfig.getInitialPollDelay().toMillis())
        .isEqualTo(LongRunningConfig.LRO_INITIAL_POLL_DELAY_MILLIS);
    assertThat(longRunningConfig.getMaxPollDelay().toMillis())
        .isEqualTo(LongRunningConfig.LRO_MAX_POLL_DELAY_MILLIS);
    assertThat(longRunningConfig.getPollDelayMultiplier())
        .isEqualTo(LongRunningConfig.LRO_POLL_DELAY_MULTIPLIER);
    assertThat(longRunningConfig.getTotalPollTimeout().toMillis())
        .isEqualTo(LongRunningConfig.LRO_TOTAL_POLL_TIMEOUT_MILLS);
    assertThat(longRunningConfig.implementsCancel())
        .isEqualTo(LongRunningConfig.LRO_IMPLEMENTS_CANCEL);
    assertThat(longRunningConfig.implementsDelete())
        .isEqualTo(LongRunningConfig.LRO_IMPLEMENTS_DELETE);
  }

  @Test
  public void testCreateLROWithGapicConfigOnly() {
    DiagCollector diagCollector = new BoundedDiagCollector();

    // simpleMethod has no LRO proto annotations.
    // lroConfigProtoWithPollSettings contains LRO settings.
    LongRunningConfig longRunningConfig =
        LongRunningConfig.createLongRunningConfig(
            simpleMethod, diagCollector, lroConfigProtoWithPollSettings, protoParser);

    assertThat(diagCollector.getErrorCount()).isEqualTo(0);
    assertThat(longRunningConfig).isNotNull();

    ProtoTypeRef metadataTypeModel = (ProtoTypeRef) longRunningConfig.getMetadataType();
    assertThat(metadataTypeModel.getProtoType()).isEqualTo(gapicConfigMetadataType);
    ProtoTypeRef returnTypeModel = (ProtoTypeRef) longRunningConfig.getReturnType();
    assertThat(returnTypeModel.getProtoType()).isEqualTo(gapicConfigReturnType);

    // These are the values specified by lroConfigProtoWithPollSettings.
    assertThat(longRunningConfig.getInitialPollDelay().toMillis())
        .isEqualTo(TEST_INITIAL_POLL_DELAY);
    assertThat(longRunningConfig.getMaxPollDelay().toMillis()).isEqualTo(TEST_MAX_POLL_DELAY);
    assertThat(longRunningConfig.getPollDelayMultiplier()).isEqualTo(TEST_POLL_DELAY_MULTIPLIER);
    assertThat(longRunningConfig.getTotalPollTimeout().toMillis())
        .isEqualTo(TEST_TOTAL_POLL_TIMEOUT);
    assertThat(longRunningConfig.implementsCancel()).isEqualTo(TEST_IMPLEMENTS_CANCEL);
    assertThat(longRunningConfig.implementsDelete()).isEqualTo(TEST_IMPLEMENTS_DELETE);
  }

  @Test
  public void testCreateLROWithAnnotationsOverridingGapicConfig() {
    DiagCollector diagCollector = new BoundedDiagCollector();

    // lroAnnotatedMethod contains different settings than that in lroConfigProtoWithPollSettings.
    LongRunningConfig longRunningConfig =
        LongRunningConfig.createLongRunningConfig(
            lroAnnotatedMethod, diagCollector, lroConfigProtoWithPollSettings, protoParser);

    assertThat(diagCollector.getErrorCount()).isEqualTo(0);
    assertThat(longRunningConfig).isNotNull();

    // Assert that proto annotations settings take precendence over gapic config.
    ProtoTypeRef metadataTypeModel = (ProtoTypeRef) longRunningConfig.getMetadataType();
    assertThat(metadataTypeModel.getProtoType()).isEqualTo(annotationsMetadataType);
    ProtoTypeRef returnTypeModel = (ProtoTypeRef) longRunningConfig.getReturnType();
    assertThat(returnTypeModel.getProtoType()).isEqualTo(annotationsReturnType);

    assertThat(longRunningConfig.getInitialPollDelay().toMillis())
        .isEqualTo(LongRunningConfig.LRO_INITIAL_POLL_DELAY_MILLIS);
    assertThat(longRunningConfig.getMaxPollDelay().toMillis())
        .isEqualTo(LongRunningConfig.LRO_MAX_POLL_DELAY_MILLIS);
    assertThat(longRunningConfig.getPollDelayMultiplier())
        .isEqualTo(LongRunningConfig.LRO_POLL_DELAY_MULTIPLIER);
    assertThat(longRunningConfig.getTotalPollTimeout().toMillis())
        .isEqualTo(LongRunningConfig.LRO_TOTAL_POLL_TIMEOUT_MILLS);
    assertThat(longRunningConfig.implementsCancel())
        .isEqualTo(LongRunningConfig.LRO_IMPLEMENTS_CANCEL);
    assertThat(longRunningConfig.implementsDelete())
        .isEqualTo(LongRunningConfig.LRO_IMPLEMENTS_DELETE);
  }

  @Test
  public void testCreateSameLROFromProtoFileAndGapicConfig() {
    // Given a Protobuf LRO method annotated with the same Return and Metadata Type
    // as in the GAPIC config, use the GAPIC config settings.
    DiagCollector diagCollector = new BoundedDiagCollector();

    // Use a GAPIC config with the same return and metadata types as the LRO proto annotations.
    LongRunningConfigProto longRunningConfigProto =
        lroConfigProtoWithPollSettings
            .toBuilder()
            .setMetadataType(ANNOTATIONS_METADATA_TYPE)
            .setReturnType(ANNOTATIONS_RETURN_TYPE_NAME)
            .build();
    LongRunningConfig longRunningConfig =
        LongRunningConfig.createLongRunningConfig(
            lroAnnotatedMethod, diagCollector, longRunningConfigProto, protoParser);

    assertThat(diagCollector.getErrorCount()).isEqualTo(0);
    assertThat(longRunningConfig).isNotNull();

    // Assert that proto annotations settings take precendence over gapic config.
    ProtoTypeRef metadataTypeModel = (ProtoTypeRef) longRunningConfig.getMetadataType();
    assertThat(metadataTypeModel.getProtoType()).isEqualTo(annotationsMetadataType);
    ProtoTypeRef returnTypeModel = (ProtoTypeRef) longRunningConfig.getReturnType();
    assertThat(returnTypeModel.getProtoType()).isEqualTo(annotationsReturnType);

    assertThat(longRunningConfig.getInitialPollDelay().toMillis())
        .isEqualTo(TEST_INITIAL_POLL_DELAY);
    assertThat(longRunningConfig.getMaxPollDelay().toMillis()).isEqualTo(TEST_MAX_POLL_DELAY);
    assertThat(longRunningConfig.getPollDelayMultiplier()).isEqualTo(TEST_POLL_DELAY_MULTIPLIER);
    assertThat(longRunningConfig.getTotalPollTimeout().toMillis())
        .isEqualTo(TEST_TOTAL_POLL_TIMEOUT);
    assertThat(longRunningConfig.implementsCancel()).isEqualTo(TEST_IMPLEMENTS_CANCEL);
    assertThat(longRunningConfig.implementsDelete()).isEqualTo(TEST_IMPLEMENTS_DELETE);
  }

  @Test
  public void testCreateLROWithNonLROMethod() {
    DiagCollector diagCollector = new BoundedDiagCollector();

    LongRunningConfig longRunningConfig =
        LongRunningConfig.createLongRunningConfig(
            simpleMethod, diagCollector, LongRunningConfigProto.getDefaultInstance(), protoParser);
    assertThat(diagCollector.getErrorCount()).isEqualTo(0);
    assertThat(longRunningConfig).isNull();
  }
}

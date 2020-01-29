/* Copyright 2020 Google LLC
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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class FlatteningConfigTest {

  @Mock private ProtoParser protoParser;
  @Mock private DiagCollector diagCollector;
  // @Mock private Method createMigrationRoutesRpc;
  // @Mock private TypeRef createMigrationRoutesRequestTypeRef;
  // @Mock private MessageType createMigrationRoutesRequestMessage;
  @Mock private Field dummyField;
  // @Mock private TypeRef dummyTypeRef;
  @Mock private ProtoField source;
  @Mock private ProtoField destination;
  @Mock private ProtoField animals;

  @Mock private ProtoMethodModel createMigrationRoutes;
  private ResourceNameMessageConfig messageConfig;
  private ResourceNameMessageConfigs messageConfigs;
  private ImmutableMap<String, ResourceNameConfig> resourceNameConfigs;
  private ResourceNameConfig county;
  private ResourceNameConfig state;
  private ResourceNameConfig bird;
  private ResourceNameConfig fish;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(protoParser.hasResourceReference(any(Field.class))).thenReturn(true);
    doThrow(new IllegalStateException("expect no errors"))
        .when(diagCollector)
        .addDiag(any(Diag.class));

    // when(createMigrationRoutesRpc.getInputType()).thenReturn(createMigrationRoutesRequestTypeRef);
    // when(createMigrationRoutesRpc.getOutputType()).thenReturn(createMigrationRoutesRequestTypeRef);
    // when(createMigrationRoutesRequestTypeRef.getMessageType())
    //     .thenReturn(createMigrationRoutesRequestMessage);
    // when(createMigrationRoutesRequestMessage.lookupField(anyString())).thenReturn(dummyField);
    // when(dummyField.getType()).thenReturn(dummyTypeRef);
    // createMigrationRoutes = new ProtoMethodModel(createMigrationRoutesRpc);

    when(createMigrationRoutes.getInputField("source")).thenReturn(source);
    when(createMigrationRoutes.getInputField("destination")).thenReturn(destination);
    when(createMigrationRoutes.getInputField("animals")).thenReturn(animals);

    when(source.getSimpleName()).thenReturn("source");
    when(source.getParentFullName()).thenReturn("google.animal.CreateMigrationRoutesRequest");
    when(source.getProtoField()).thenReturn(dummyField);
    when(source.isRepeated()).thenReturn(false);
    when(destination.getSimpleName()).thenReturn("destination");
    when(destination.isRepeated()).thenReturn(false);
    when(destination.getProtoField()).thenReturn(dummyField);
    when(destination.getParentFullName()).thenReturn("google.animal.CreateMigrationRoutesRequest");
    when(animals.getSimpleName()).thenReturn("animals");
    when(animals.isRepeated()).thenReturn(true);
    when(animals.getProtoField()).thenReturn(dummyField);
    when(animals.getParentFullName()).thenReturn("google.animal.CreateMigrationRoutesRequest");

    messageConfig =
        new AutoValue_ResourceNameMessageConfig(
            "google.animal.CreateMigrationRoutesRequest",
            ImmutableListMultimap.<String, String>builder()
                .put("source", "County")
                .put("source", "State")
                .put("destination", "County")
                .put("destination", "State")
                .put("animals", "Fish")
                .put("animals", "Bird")
                .build());

    messageConfigs =
        new AutoValue_ResourceNameMessageConfigs(
            ImmutableMap.of("google.animal.CreateMigrationRoutesRequest", messageConfig),
            ImmutableListMultimap.<String, FieldModel>builder()
                .put("google.animal.CreateMigrationRoutesRequest", source)
                .put("google.animal.CreateMigrationRoutesRequest", destination)
                .put("google.animal.CreateMigrationRoutesRequest", animals)
                .build());

    state =
        SingleResourceNameConfig.newBuilder()
            .setNamePattern("states/{state}")
            .setEntityId("State")
            .setEntityName(Name.from("state"))
            .build();

    county =
        SingleResourceNameConfig.newBuilder()
            .setNamePattern("states/{state}/counties/{county}")
            .setEntityId("County")
            .setEntityName(Name.from("county"))
            .build();

    bird =
        SingleResourceNameConfig.newBuilder()
            .setNamePattern("birds/{bird}")
            .setEntityId("Bird")
            .setEntityName(Name.from("bird"))
            .build();

    fish =
        SingleResourceNameConfig.newBuilder()
            .setNamePattern("fish/{fish}")
            .setEntityId("Fish")
            .setEntityName(Name.from("fish"))
            .build();

    resourceNameConfigs =
        ImmutableMap.<String, ResourceNameConfig>builder()
            .put("State", state)
            .put("County", county)
            .put("Fish", fish)
            .put("Bird", bird)
            .build();
  }

  @Test
  public void testCreateFlatteningConfigsWithResourceNameCombination() {

    List<FlatteningConfig> flatteningConfigs =
        FlatteningConfig.createFlatteningsFromProtoFile(
            diagCollector,
            messageConfigs,
            resourceNameConfigs,
            ImmutableList.of("source", "destination", "animals"),
            createMigrationRoutes,
            protoParser);

    FieldConfig sourceFieldStateResource =
        FieldConfig.newBuilder()
            .setResourceNameConfig(state)
            .setExampleResourceNameConfig(state)
            .setResourceNameTreatment(ResourceNameTreatment.STATIC_TYPES)
            .setField(source)
            .build();

    FieldConfig sourceFieldCountyResource =
        FieldConfig.newBuilder()
            .setResourceNameConfig(county)
            .setExampleResourceNameConfig(county)
            .setResourceNameTreatment(ResourceNameTreatment.STATIC_TYPES)
            .setField(source)
            .build();

    FieldConfig sourceFieldSampleOnly =
        FieldConfig.newBuilder()
            .setResourceNameConfig(county)
            .setExampleResourceNameConfig(county)
            .setResourceNameTreatment(ResourceNameTreatment.SAMPLE_ONLY)
            .setField(source)
            .build();

    FieldConfig destinationFieldStateResource =
        FieldConfig.newBuilder()
            .setResourceNameConfig(state)
            .setExampleResourceNameConfig(state)
            .setResourceNameTreatment(ResourceNameTreatment.STATIC_TYPES)
            .setField(destination)
            .build();

    FieldConfig destinationFieldCountyResource =
        FieldConfig.newBuilder()
            .setResourceNameConfig(county)
            .setExampleResourceNameConfig(county)
            .setResourceNameTreatment(ResourceNameTreatment.STATIC_TYPES)
            .setField(destination)
            .build();

    FieldConfig destinationFieldSampleOnly =
        FieldConfig.newBuilder()
            .setResourceNameConfig(county)
            .setExampleResourceNameConfig(county)
            .setResourceNameTreatment(ResourceNameTreatment.SAMPLE_ONLY)
            .setField(destination)
            .build();

    FieldConfig animalsField =
        FieldConfig.newBuilder()
            .setResourceNameConfig(fish)
            .setExampleResourceNameConfig(fish)
            .setResourceNameTreatment(ResourceNameTreatment.SAMPLE_ONLY)
            .setField(animals)
            .build();

    List<FlatteningConfig> expectedFlatteningConfigs =
        ImmutableList.of(
            new AutoValue_FlatteningConfig(
                ImmutableMap.of(
                    "source",
                    sourceFieldStateResource,
                    "destination",
                    destinationFieldStateResource,
                    "animals",
                    animalsField)),
            new AutoValue_FlatteningConfig(
                ImmutableMap.of(
                    "source",
                    sourceFieldCountyResource,
                    "destination",
                    destinationFieldStateResource,
                    "animals",
                    animalsField)),
            new AutoValue_FlatteningConfig(
                ImmutableMap.of(
                    "source",
                    sourceFieldStateResource,
                    "destination",
                    destinationFieldCountyResource,
                    "animals",
                    animalsField)),
            new AutoValue_FlatteningConfig(
                ImmutableMap.of(
                    "source",
                    sourceFieldCountyResource,
                    "destination",
                    destinationFieldCountyResource,
                    "animals",
                    animalsField)),
            new AutoValue_FlatteningConfig(
                ImmutableMap.of(
                    "source",
                    sourceFieldSampleOnly,
                    "destination",
                    destinationFieldSampleOnly,
                    "animals",
                    animalsField)));

    assertThat(flatteningConfigs).containsExactlyElementsIn(expectedFlatteningConfigs);
  }
}

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
import static org.mockito.Mockito.when;

import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.codegen.util.Name;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class FieldConfigFactoryTest {

  @Mock private FieldModel parent;
  @Mock private FieldModel moreParents;

  private ResourceNameMessageConfig messageConfig;
  private ResourceNameMessageConfigs messageConfigs;
  private Map<String, ResourceNameConfig> resourceNameConfigs;
  private ResourceNameConfig archive;
  private ResourceNameConfig shelf;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(parent.getSimpleName()).thenReturn("parent");
    when(parent.getParentFullName()).thenReturn("google.cloud.library.ListBooksRequest");
    when(parent.isRepeated()).thenReturn(false);
    when(moreParents.getSimpleName()).thenReturn("more_parents");
    when(moreParents.isRepeated()).thenReturn(true);
    when(moreParents.getParentFullName()).thenReturn("google.cloud.library.ListBooksRequest");

    messageConfig =
        new AutoValue_ResourceNameMessageConfig(
            "google.cloud.library.ListBooksRequest",
            ImmutableListMultimap.of(
                "parent",
                "Shelf",
                "parent",
                "Archive",
                "more_parents",
                "Shelf",
                "more_parents",
                "Archive"));

    messageConfigs =
        new AutoValue_ResourceNameMessageConfigs(
            ImmutableMap.of("google.cloud.library.ListBooksRequest", messageConfig),
            ImmutableListMultimap.of(
                "google.cloud.library.ListBooksRequest",
                parent,
                "google.cloud.library.ListBooksRequest",
                moreParents));

    archive =
        SingleResourceNameConfig.newBuilder()
            .setNamePattern("")
            .setEntityId("Archive")
            .setEntityName(Name.from("archive"))
            .build();

    shelf =
        SingleResourceNameConfig.newBuilder()
            .setNamePattern("")
            .setEntityId("Shelf")
            .setEntityName(Name.from("shelf"))
            .build();

    resourceNameConfigs = ImmutableMap.of("Archive", archive, "Shelf", shelf);
  }

  @Test
  public void testCreateFlattenedFieldConfigsForSingularMultiResourceField() {

    List<FieldConfig> fieldConfigs =
        FieldConfigFactory.createFlattenedFieldConfigs(
            messageConfigs, resourceNameConfigs, parent, ResourceNameTreatment.STATIC_TYPES);
    FieldConfig parentConfig1 =
        FieldConfig.newBuilder()
            .setResourceNameConfig(archive)
            .setExampleResourceNameConfig(archive)
            .setResourceNameTreatment(ResourceNameTreatment.STATIC_TYPES)
            .setField(parent)
            .build();
    FieldConfig parentConfig2 =
        FieldConfig.newBuilder()
            .setResourceNameConfig(shelf)
            .setExampleResourceNameConfig(shelf)
            .setResourceNameTreatment(ResourceNameTreatment.STATIC_TYPES)
            .setField(parent)
            .build();
    assertThat(fieldConfigs).containsExactly(parentConfig1, parentConfig2);
  }

  @Test
  public void testCreateFlattenedFieldConfigsForRepeatedMultiResourceField() {
    List<FieldConfig> fieldConfigs =
        FieldConfigFactory.createFlattenedFieldConfigs(
            messageConfigs, resourceNameConfigs, moreParents, ResourceNameTreatment.STATIC_TYPES);
    FieldConfig moreParentConfig1 =
        FieldConfig.newBuilder()
            .setResourceNameConfig(archive)
            .setExampleResourceNameConfig(archive)
            .setResourceNameTreatment(ResourceNameTreatment.STATIC_TYPES)
            .setField(moreParents)
            .build();
    FieldConfig moreParentConfig2 =
        FieldConfig.newBuilder()
            .setResourceNameConfig(shelf)
            .setExampleResourceNameConfig(shelf)
            .setResourceNameTreatment(ResourceNameTreatment.STATIC_TYPES)
            .setField(moreParents)
            .build();
    assertThat(fieldConfigs).containsExactly(moreParentConfig1, moreParentConfig2);
  }
}

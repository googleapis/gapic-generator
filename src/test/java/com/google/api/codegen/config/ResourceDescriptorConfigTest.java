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

import com.google.api.ResourceDescriptor;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.mockito.Mockito;

public class ResourceDescriptorConfigTest {
  private static final ProtoFile protoFile = Mockito.mock(ProtoFile.class);

  @Test
  public void testFromResourceDescriptor() {
    ResourceDescriptor.Builder descriptorBuilder =
        ResourceDescriptor.newBuilder().setType("abc/Def");

    ResourceDescriptor descriptor = descriptorBuilder.addPattern("foos/{foo}").build();
    ResourceDescriptorConfig config = ResourceDescriptorConfig.from(descriptor, protoFile, true);
    assertThat(config.getRequiresOneofConfig()).isFalse();
    assertThat(config.getSinglePattern()).isEqualTo("foos/{foo}");

    descriptor =
        descriptorBuilder
            .clearPattern()
            .addPattern("foos/{foo}")
            .addPattern("foos/{foo}/bars/{bar}")
            .build();
    config = ResourceDescriptorConfig.from(descriptor, protoFile, true);
    assertThat(config.getRequiresOneofConfig()).isTrue();
    assertThat(config.getSinglePattern()).isEqualTo("");

    descriptor =
        descriptorBuilder
            .clearPattern()
            .addPattern("foos/{foo}")
            .setHistory(ResourceDescriptor.History.ORIGINALLY_SINGLE_PATTERN)
            .build();
    config = ResourceDescriptorConfig.from(descriptor, protoFile, true);
    assertThat(config.getRequiresOneofConfig()).isFalse();
    assertThat(config.getSinglePattern()).isEqualTo("foos/{foo}");

    descriptor =
        descriptorBuilder
            .clearPattern()
            .addPattern("foos/{foo}")
            .addPattern("foos/{foo}/bars/{bar}")
            .setHistory(ResourceDescriptor.History.ORIGINALLY_SINGLE_PATTERN)
            .build();
    config = ResourceDescriptorConfig.from(descriptor, protoFile, true);
    assertThat(config.getRequiresOneofConfig()).isTrue();
    assertThat(config.getSinglePattern()).isEqualTo("foos/{foo}");

    descriptor =
        descriptorBuilder
            .clearPattern()
            .addPattern("foos/{foo}")
            .setHistory(ResourceDescriptor.History.FUTURE_MULTI_PATTERN)
            .build();
    config = ResourceDescriptorConfig.from(descriptor, protoFile, true);
    assertThat(config.getRequiresOneofConfig()).isTrue();
    assertThat(config.getSinglePattern()).isEqualTo("");

    descriptor =
        descriptorBuilder
            .clearPattern()
            .addPattern("foos/{foo}")
            .addPattern("foos/{foo}/bars/{bar}")
            .setHistory(ResourceDescriptor.History.FUTURE_MULTI_PATTERN)
            .build();
    config = ResourceDescriptorConfig.from(descriptor, protoFile, true);
    assertThat(config.getRequiresOneofConfig()).isTrue();
    assertThat(config.getSinglePattern()).isEqualTo("");
  }

  @Test
  public void testGetParentPattern() {
    assertThat(ResourceDescriptorConfig.getParentPattern("foos/{foo}/bars/{bar}"))
        .isEqualTo("foos/{foo}");
    assertThat(ResourceDescriptorConfig.getParentPattern("foos/{foo}/busy/bars/{bar}"))
        .isEqualTo("foos/{foo}/busy");
    assertThat(ResourceDescriptorConfig.getParentPattern("foos/{foo}/bars/{bar}/bang"))
        .isEqualTo("foos/{foo}/bars/{bar}");
    assertThat(ResourceDescriptorConfig.getParentPattern("foos/{foo}")).isEqualTo("");
  }

  @Test
  public void testGetChildParentResourceMapFail_ParentHasExtraPattern() {
    ResourceDescriptorConfig childResource =
        ResourceDescriptorConfig.from(
            ResourceDescriptor.newBuilder()
                .setType("myservice.google.com/Child")
                .addPattern("projects/{project}/children/{child}")
                .build(),
            protoFile,
            true);

    ResourceDescriptorConfig parentResource =
        ResourceDescriptorConfig.from(
            ResourceDescriptor.newBuilder()
                .setType("myservice.google.com/Parent")
                .addPattern("projects/{project}")
                .addPattern("locations/{location}")
                .build(),
            protoFile,
            true);

    Map<String, ResourceDescriptorConfig> descriptorConfigMap =
        getDescriptorConfigMap(childResource, parentResource);
    Map<String, List<ResourceDescriptorConfig>> patternResourceDescriptorMap =
        ResourceDescriptorConfig.getPatternResourceMap(
            Arrays.asList(childResource, parentResource));

    Map<String, List<ResourceDescriptorConfig>> childParentResourceMap =
        ResourceDescriptorConfig.getChildParentResourceMap(
            descriptorConfigMap, patternResourceDescriptorMap);

    assertThat(childParentResourceMap).isEmpty();
  }

  @Test
  public void testGetChildParentResourceMapFail_ChildHasExtraPattern() {
    ResourceDescriptorConfig child =
        ResourceDescriptorConfig.from(
            ResourceDescriptor.newBuilder()
                .setType("myservice.google.com/Child")
                .addPattern("projects/{project}/children/{child}")
                .addPattern("locations/{location}/children/{child}")
                .build(),
            protoFile,
            true);

    ResourceDescriptorConfig parent =
        ResourceDescriptorConfig.from(
            ResourceDescriptor.newBuilder()
                .setType("myservice.google.com/Parent")
                .addPattern("projects/{project}")
                .build(),
            protoFile,
            true);

    Map<String, ResourceDescriptorConfig> descriptorConfigMap =
        getDescriptorConfigMap(child, parent);
    Map<String, List<ResourceDescriptorConfig>> patternResourceDescriptorMap =
        ResourceDescriptorConfig.getPatternResourceMap(Arrays.asList(child, parent));

    Map<String, List<ResourceDescriptorConfig>> childParentResourceMap =
        ResourceDescriptorConfig.getChildParentResourceMap(
            descriptorConfigMap, patternResourceDescriptorMap);

    assertThat(childParentResourceMap).isEmpty();
  }

  @Test
  public void testGetChildParentResourceMapFail_PatternsMismatch() {
    ResourceDescriptorConfig child =
        ResourceDescriptorConfig.from(
            ResourceDescriptor.newBuilder()
                .setType("myservice.google.com/Child")
                .addPattern("projects/{project}/children/{child}")
                .addPattern("locations/{location}/children/{child}")
                .build(),
            protoFile,
            true);

    ResourceDescriptorConfig parent =
        ResourceDescriptorConfig.from(
            ResourceDescriptor.newBuilder()
                .setType("myservice.google.com/Parent")
                .addPattern("projects/{project}")
                .addPattern("projects/{project}/locations/{location}")
                .build(),
            protoFile,
            true);

    Map<String, ResourceDescriptorConfig> descriptorConfigMap =
        getDescriptorConfigMap(child, parent);
    Map<String, List<ResourceDescriptorConfig>> patternResourceDescriptorMap =
        ResourceDescriptorConfig.getPatternResourceMap(Arrays.asList(child, parent));

    Map<String, List<ResourceDescriptorConfig>> childParentResourceMap =
        ResourceDescriptorConfig.getChildParentResourceMap(
            descriptorConfigMap, patternResourceDescriptorMap);

    assertThat(childParentResourceMap).isEmpty();
  }

  @Test
  public void testGetChildParentResourceMap_MultiParentsSinglePattern() {
    ResourceDescriptorConfig sauce =
        ResourceDescriptorConfig.from(
            ResourceDescriptor.newBuilder()
                .setType("food.google.com/Sauce")
                .addPattern("pizzas/{pizza}/sauces/{sauce}")
                .addPattern("pastas/{pasta}/sauces/{sauce}")
                .build(),
            protoFile,
            true);

    ResourceDescriptorConfig pizza =
        ResourceDescriptorConfig.from(
            ResourceDescriptor.newBuilder()
                .setType("foo.google.com/Pizza")
                .addPattern("pizzas/{pizza}")
                .build(),
            protoFile,
            true);

    ResourceDescriptorConfig pasta =
        ResourceDescriptorConfig.from(
            ResourceDescriptor.newBuilder()
                .setType("food.google.com/Pasta")
                .addPattern("pastas/{pasta}")
                .build(),
            protoFile,
            true);

    Map<String, ResourceDescriptorConfig> descriptorConfigMap =
        getDescriptorConfigMap(pizza, pasta, sauce);
    Map<String, List<ResourceDescriptorConfig>> patternResourceDescriptorMap =
        ResourceDescriptorConfig.getPatternResourceMap(Arrays.asList(pizza, pasta, sauce));

    Map<String, List<ResourceDescriptorConfig>> childParentResourceMap =
        ResourceDescriptorConfig.getChildParentResourceMap(
            descriptorConfigMap, patternResourceDescriptorMap);

    assertThat(childParentResourceMap.size()).isEqualTo(1);
    assertThat(childParentResourceMap.get("food.google.com/Sauce")).containsExactly(pizza, pasta);
  }

  @Test
  public void testGetChildParentResourceMap_MultiParentsMultiPatterns() {
    ResourceDescriptorConfig sauce =
        ResourceDescriptorConfig.from(
            ResourceDescriptor.newBuilder()
                .setType("food.google.com/Sauce")
                .addPattern("steaks/{steak}/sauces/{sauce}")
                .addPattern("barbeques/{barbeque}/sauces/{sauce}")
                .addPattern("tofus/{tofu}/sauces/{sauce}")
                .addPattern("salads/{salad}/sauces/{sauce}")
                .build(),
            protoFile,
            true);

    ResourceDescriptorConfig meat =
        ResourceDescriptorConfig.from(
            ResourceDescriptor.newBuilder()
                .setType("food.google.com/Meat")
                .addPattern("steaks/{steak}")
                .addPattern("barbeques/{barbeque}")
                .build(),
            protoFile,
            true);

    ResourceDescriptorConfig veggie =
        ResourceDescriptorConfig.from(
            ResourceDescriptor.newBuilder()
                .setType("food.google.com/Veggie")
                .addPattern("tofus/{tofu}")
                .addPattern("salads/{salad}")
                .build(),
            protoFile,
            true);

    Map<String, ResourceDescriptorConfig> descriptorConfigMap =
        getDescriptorConfigMap(sauce, meat, veggie);
    Map<String, List<ResourceDescriptorConfig>> patternResourceDescriptorMap =
        ResourceDescriptorConfig.getPatternResourceMap(Arrays.asList(sauce, meat, veggie));

    Map<String, List<ResourceDescriptorConfig>> childParentResourceMap =
        ResourceDescriptorConfig.getChildParentResourceMap(
            descriptorConfigMap, patternResourceDescriptorMap);

    assertThat(childParentResourceMap.size()).isEqualTo(1);
    assertThat(childParentResourceMap.get("food.google.com/Sauce")).containsExactly(veggie, meat);
  }

  private static Map<String, ResourceDescriptorConfig> getDescriptorConfigMap(
      ResourceDescriptorConfig... resources) {
    ImmutableMap.Builder<String, ResourceDescriptorConfig> builder = ImmutableMap.builder();
    for (ResourceDescriptorConfig resource : resources) {
      builder.put(resource.getUnifiedResourceType(), resource);
    }
    return builder.build();
  }
}

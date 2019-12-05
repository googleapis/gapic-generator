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
import org.junit.Test;
import org.mockito.Mockito;

public class ResourceDescriptorConfigTest {
  private static final ProtoFile protoFile = Mockito.mock(ProtoFile.class);

  @Test
  public void testFromResourceDescriptor() {
    ResourceDescriptor.Builder descriptorBuilder =
        ResourceDescriptor.newBuilder().setType("abc/Def");

    ResourceDescriptor descriptor = descriptorBuilder.addPattern("foos/{foo}").build();
    ResourceDescriptorConfig config = ResourceDescriptorConfig.from(descriptor, protoFile);
    assertThat(config.getRequiresOneofConfig()).isFalse();
    assertThat(config.getSinglePattern()).isEqualTo("foos/{foo}");

    descriptor =
        descriptorBuilder
            .clearPattern()
            .addPattern("foos/{foo}")
            .addPattern("foos/{foo}/bars/{bar}")
            .build();
    config = ResourceDescriptorConfig.from(descriptor, protoFile);
    assertThat(config.getRequiresOneofConfig()).isTrue();
    assertThat(config.getSinglePattern()).isEqualTo("");

    descriptor =
        descriptorBuilder
            .clearPattern()
            .addPattern("foos/{foo}")
            .setHistory(ResourceDescriptor.History.ORIGINALLY_SINGLE_PATTERN)
            .build();
    config = ResourceDescriptorConfig.from(descriptor, protoFile);
    assertThat(config.getRequiresOneofConfig()).isFalse();
    assertThat(config.getSinglePattern()).isEqualTo("foos/{foo}");

    descriptor =
        descriptorBuilder
            .clearPattern()
            .addPattern("foos/{foo}")
            .addPattern("foos/{foo}/bars/{bar}")
            .setHistory(ResourceDescriptor.History.ORIGINALLY_SINGLE_PATTERN)
            .build();
    config = ResourceDescriptorConfig.from(descriptor, protoFile);
    assertThat(config.getRequiresOneofConfig()).isTrue();
    assertThat(config.getSinglePattern()).isEqualTo("foos/{foo}");

    descriptor =
        descriptorBuilder
            .clearPattern()
            .addPattern("foos/{foo}")
            .setHistory(ResourceDescriptor.History.FUTURE_MULTI_PATTERN)
            .build();
    config = ResourceDescriptorConfig.from(descriptor, protoFile);
    assertThat(config.getRequiresOneofConfig()).isTrue();
    assertThat(config.getSinglePattern()).isEqualTo("");

    descriptor =
        descriptorBuilder
            .clearPattern()
            .addPattern("foos/{foo}")
            .addPattern("foos/{foo}/bars/{bar}")
            .setHistory(ResourceDescriptor.History.FUTURE_MULTI_PATTERN)
            .build();
    config = ResourceDescriptorConfig.from(descriptor, protoFile);
    assertThat(config.getRequiresOneofConfig()).isTrue();
    assertThat(config.getSinglePattern()).isEqualTo("");
  }

  @Test
  public void testGetParentPattern() {
    assertThat(ResourceDescriptorConfig.getParentPattern("foos/{foo}/bars/{bar}"))
        .isEqualTo("foos/{foo}");
    assertThat(ResourceDescriptorConfig.getParentPattern("foos/{foo}/busy/bars/{bar}"))
        .isEqualTo("foos/{foo}");
    assertThat(ResourceDescriptorConfig.getParentPattern("foos/{foo}/bars/{bar}/bang"))
        .isEqualTo("foos/{foo}/bars/{bar}");
    assertThat(ResourceDescriptorConfig.getParentPattern("foos/{foo}")).isEqualTo("");
  }
}

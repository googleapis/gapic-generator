/* Copyright 2016 Google LLC
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
package com.google.api.codegen.transformer.go;

import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.util.Name;
import com.google.common.truth.Truth;
import org.junit.Test;

public class GoSurfaceNamerTest {
  @Test
  public void testClientNamePrefix() {
    GoSurfaceNamer namer = new GoSurfaceNamer("cloud.google.com/go/gopher/apiv1");
    Truth.assertThat(namer.getPackageName()).isEqualTo("cloud.google.com/go/gopher/apiv1");
    Truth.assertThat(namer.getLocalPackageName()).isEqualTo("gopher");

    // Both the service name and the local package name are "gopher",
    // the client name prefix should be empty.
    Truth.assertThat(namer.getReducedServiceName("Gopher")).isEqualTo(Name.from("gopher"));
    Truth.assertThat(namer.clientNamePrefix("Gopher")).isEqualTo(Name.from());

    // The casing of the service name does not matter.
    // Elements of the package path are usually all lowercase, even if they are multi-worded.
    Truth.assertThat(namer.getReducedServiceName("GoPher")).isEqualTo(Name.from("go", "pher"));
    Truth.assertThat(namer.clientNamePrefix("GoPher")).isEqualTo(Name.from());

    // The service name is different from the local package name,
    // use the service name as the prefix.
    Truth.assertThat(namer.getReducedServiceName("Guru")).isEqualTo(Name.from("guru"));
    Truth.assertThat(namer.clientNamePrefix("Guru")).isEqualTo(Name.from("guru"));
  }

  @Test
  public void testOperationName() {
    GoSurfaceNamer namer = new GoSurfaceNamer("cloud.google.com/go/gopher/apiv1");
    Truth.assertThat(namer.getAndSaveOperationResponseTypeName("CreateStuff"))
        .isEqualTo("CreateStuffOperation");
    Truth.assertThat(namer.getAndSaveOperationResponseTypeName("CreateStuffOperation"))
        .isEqualTo("CreateStuffOperationHandle");
  }

  @Test
  public void testPutDocMethodName() {
    GoSurfaceNamer namer = new GoSurfaceNamer("cloud.google.com/go/gopher/apiv1");
    Truth.assertThat(namer.putDocMethodName("Create", "creates things", VisibilityConfig.PUBLIC))
        .isEqualTo("Create creates things");
    Truth.assertThat(namer.putDocMethodName("Create", "Creates things", VisibilityConfig.PUBLIC))
        .isEqualTo("Create creates things");
    Truth.assertThat(
            namer.putDocMethodName(
                "Create", "Create is used to create things", VisibilityConfig.PUBLIC))
        .isEqualTo("Create is used to create things");
    Truth.assertThat(
            namer.putDocMethodName(
                "Create", "creAte is used to create things", VisibilityConfig.PUBLIC))
        .isEqualTo("Create is used to create things");
    Truth.assertThat(
            namer.putDocMethodName(
                "CreateThing", "creatething is used to create things", VisibilityConfig.PUBLIC))
        .isEqualTo("CreateThing is used to create things");
  }
}

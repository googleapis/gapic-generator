/* Copyright 2017 Google LLC
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
package com.google.api.codegen.transformer.ruby;

import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.common.truth.Truth;
import org.junit.Test;
import org.mockito.Mockito;

public class RubySurfaceNamerTest {
  @Test
  public void getApiMethodName() {
    RubySurfaceNamer namer = new RubySurfaceNamer("Unused::Package::Name");
    MethodModel method = Mockito.mock(MethodModel.class);
    Mockito.when(method.getSimpleName()).thenReturn("PrintHTML");
    Truth.assertThat(namer.getApiMethodName(method, VisibilityConfig.PUBLIC))
        .isEqualTo("print_html");
    Mockito.when(method.getSimpleName()).thenReturn("AMethod");
    Truth.assertThat(namer.getApiMethodName(method, VisibilityConfig.PUBLIC)).isEqualTo("a_method");
    Mockito.when(method.getSimpleName()).thenReturn("AnRpc");
    Truth.assertThat(namer.getApiMethodName(method, VisibilityConfig.PUBLIC)).isEqualTo("an_rpc");
    Mockito.when(method.getSimpleName()).thenReturn("SeeHTMLBooks");
    Truth.assertThat(namer.getApiMethodName(method, VisibilityConfig.PUBLIC))
        .isEqualTo("see_html_books");
  }
}

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

import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.BoundedDiagCollector;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Method;
import org.junit.Test;
import org.mockito.Mockito;

public class FlatteningConfigTest {
  private static final ProtoParser protoParser = Mockito.mock(ProtoParser.class);
  private static final Method httpGetMethod = Mockito.mock(Method.class);
  private static final Method cancelledMethod = Mockito.mock(Method.class);

  @Test
  public void testCreateFlatteningFromProtoFile() {

    DiagCollector diagCollector = new BoundedDiagCollector();
    // FlatteningConfig flatteningConfig = FlatteningConfig.createFlattening(diagCollector)
  }

  @Test
  public void testCreateFlatteningFromProtoFileAndGapicConfig() {

    // MethodConfig.createFlattening();
    DiagCollector diagCollector = new BoundedDiagCollector();
    // FlatteningConfig flatteningConfig = FlatteningConfig.createFlattening(diagCollector)
  }
}

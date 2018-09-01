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
package com.google.api.codegen;

import com.google.api.AnnotationsProto;
import com.google.api.AuthProto;
import com.google.api.tools.framework.model.testing.TestConfig;
import com.google.api.tools.framework.model.testing.TestDataLocator;
import com.google.longrunning.OperationsProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.ExtensionRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class GapicTestConfig extends TestConfig {

  public GapicTestConfig(TestDataLocator testDataLocator, String tempDir, List<String> protoFiles) {
    super(testDataLocator, tempDir, protoFiles);
  }

  /** Returns the file descriptor set generated from the sources of this api. */
  @Override
  public FileDescriptorSet getDescriptor() throws IOException {
    ExtensionRegistry registry = ExtensionRegistry.newInstance();
    AnnotationsProto.registerAllExtensions(registry);
    OperationsProto.registerAllExtensions(registry);
    AuthProto.registerAllExtensions(registry);
    return FileDescriptorSet.parseFrom(Files.newInputStream(getDescriptorFile()), registry);
  }
}

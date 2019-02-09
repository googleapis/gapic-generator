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
package com.google.api.codegen.gapic;

import com.google.api.codegen.CodegenTestUtil;
import com.google.api.codegen.ProtocGeneratorMain;
import com.google.api.codegen.protoannotations.GapicCodeGeneratorAnnotationsTest;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.testing.TestDataLocator;
import com.google.common.truth.Truth;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ProtocGapicPluginGeneratorTest {

  private static String[] protoFiles = {"multiple_services.proto"};
  private static TestDataLocator testDataLocator;
  private static Model model;
  @ClassRule public static TemporaryFolder tempDir = new TemporaryFolder();

  @BeforeClass
  public static void startUp() {
    testDataLocator = TestDataLocator.create(GapicCodeGeneratorAnnotationsTest.class);
    testDataLocator.addTestDataSource(CodegenTestUtil.class, "testsrc/common");

    model = CodegenTestUtil.readModel(testDataLocator, tempDir, protoFiles, new String[] {});
  }

  @Test
  public void testGenerator() {
    CodeGeneratorRequest codeGeneratorRequest =
        CodeGeneratorRequest.newBuilder()
            // All proto files, including dependencies
            .addAllProtoFile(
                model.getFiles().stream().map(ProtoFile::getProto).collect(Collectors.toList()))
            // Only the file to generate a client for (don't generate dependencies)
            .addFileToGenerate("multiple_services.proto")
            .setParameter("language=java")
            .build();

    CodeGeneratorResponse response = ProtocGeneratorMain.generate(codeGeneratorRequest);

    // TODO(andrealin): Look into setting these up as baseline files.
    Truth.assertThat(response).isNotNull();
    Truth.assertThat(response.getFileCount()).isEqualTo(15);
    Truth.assertThat(response.getFile(0).getContent()).contains("DecrementerServiceClient");
    Truth.assertThat(response.getError()).isEmpty();
  }

  @Test
  public void testFailingGenerator() {
    CodeGeneratorRequest codeGeneratorRequest =
        CodeGeneratorRequest.newBuilder()
            .addAllProtoFile(
                model.getFiles().stream().map(ProtoFile::getProto).collect(Collectors.toList()))
            // File does not exist.
            .addFileToGenerate("fuuuuudge.proto")
            .build();

    CodeGeneratorResponse response = ProtocGeneratorMain.generate(codeGeneratorRequest);

    Truth.assertThat(response).isNotNull();
    Truth.assertThat(response.getError()).isNotEmpty();
  }
}

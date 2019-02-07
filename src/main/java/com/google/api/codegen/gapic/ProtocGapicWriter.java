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

import com.google.api.codegen.common.GeneratedResult;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.snippet.Doc;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import java.util.Map;
import javax.annotation.Nonnull;

public class ProtocGapicWriter implements GapicWriter {

  private boolean isDone = false;
  private CodeGeneratorResponse response;

  @Override
  public boolean isDone() {
    return isDone;
  }

  // If isDone() is true, then this returns the populated CodeGeneratorResponse object.
  public CodeGeneratorResponse getCodegenResponse() {
    return response;
  }

  @Override
  public void writeCodeGenOutput(
      @Nonnull Map<String, GeneratedResult<?>> generatedResults, DiagCollector diagCollector) {
    Map<String, Object> outputFiles = GeneratedResult.extractBodiesGeneric(generatedResults);
    this.response = writeCodeGenOutputToProtoc(outputFiles);
    this.isDone = true;
  }

  private CodeGeneratorResponse writeCodeGenOutputToProtoc(Map<String, ?> outputFiles) {
    CodeGeneratorResponse.Builder protocResponse = CodeGeneratorResponse.newBuilder();

    for (Map.Entry<String, ?> entry : outputFiles.entrySet()) {
      com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.File.Builder protoOutFile =
          com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.File.newBuilder();

      StringBuilder outputStream = new StringBuilder();

      Object value = entry.getValue();
      if (value instanceof Doc) {
        outputStream.append(((Doc) value).prettyPrint());
      } else if (value instanceof String) {
        outputStream.append((String) value);
      } else if (value instanceof byte[]) {
        outputStream.append((byte[]) value);
      } else {
        throw new IllegalArgumentException("Expected one of Doc, String, or byte[]");
      }

      protoOutFile.setContent(outputStream.toString());
      protoOutFile.setName(entry.getKey());
      protocResponse.addFile(protoOutFile.build());
    }

    return protocResponse.build();
  }
}

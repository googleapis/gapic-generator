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
package com.google.api.codegen;

import static com.google.api.codegen.ArtifactType.GAPIC_CODE;

import com.google.api.codegen.gapic.GapicGeneratorApp;
import com.google.api.codegen.gapic.ProtocGapicWriter;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.compiler.PluginProtos;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

/**
 * Entrypoint for protoc-plugin invoked generation. Protoc passes input via std.in as a serialized
 * CodeGeneratorRequest, and expects to read a CodeGeneratorResponse from std.out.
 */
public class ProtocGeneratorMain {

  private static final ArtifactType DEFAULT_ARTIFACT_TYPE = GAPIC_CODE;

  public static void main(String[] args) {
    CodeGeneratorResponse response;
    CodeGeneratorRequest request;
    int exitCode = 0;

    try {
      request = PluginProtos.CodeGeneratorRequest.parseFrom(System.in);
    } catch (IOException e) {
      System.err.println("Unable to parse CodeGeneraterRequest from stdin.");
      System.exit(1);
      return;
    }

    try {
      response = generate(request);
    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      pw.flush();
      response = PluginProtos.CodeGeneratorResponse.newBuilder().setError(sw.toString()).build();
      exitCode = 1;
    }

    try {
      response.writeTo(System.out);
    } catch (IOException e) {
      exitCode = 1;
      System.err.println("Failed to write out CodeGeneratorResponse.");
    }

    System.out.flush();
    System.exit(exitCode);
  }

  @VisibleForTesting
  // Parses the InputStream for a CodeGeneratorRequest and returns the generated output in a
  // CodeGeneratorResponse.
  public static CodeGeneratorResponse generate(CodeGeneratorRequest request) {
    try {
      ToolOptions toolOptions = parseOptions(request);

      ProtocGapicWriter gapicWriter = new ProtocGapicWriter();
      GapicGeneratorApp codeGen =
          new GapicGeneratorApp(toolOptions, DEFAULT_ARTIFACT_TYPE, gapicWriter);

      codeGen.run();
      CodeGeneratorResponse response = gapicWriter.getCodegenResponse();
      if (response == null) {
        throw new RuntimeException(collectDiags(codeGen));
      }
      return response;
    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      pw.flush();
      return PluginProtos.CodeGeneratorResponse.newBuilder().setError(sw.toString()).build();
    }
  }

  private static ToolOptions parseOptions(CodeGeneratorRequest request) throws Exception {
    List<FileDescriptorProto> fileDescriptorProtoList = request.getProtoFileList();
    FileDescriptorSet descriptorSet =
        FileDescriptorSet.newBuilder().addAllFile(fileDescriptorProtoList).build();

    // Write out DescriptorSet to temp file.
    File descriptorSetFile;

    descriptorSetFile = File.createTempFile("api", ".desc");
    FileOutputStream fileoutput = new FileOutputStream(descriptorSetFile);
    descriptorSet.writeTo(fileoutput);
    fileoutput.close();
    descriptorSetFile.deleteOnExit();

    List<String> parsedArgs = new LinkedList<>();
    parsedArgs.add("--descriptor_set");
    parsedArgs.add(descriptorSetFile.getAbsolutePath());

    List<String> protoPackages = getProtoPackageList(request);
    if (protoPackages.size() > 1) {
      // For now, assume there will be exactly one proto package to be generated. This behavior
      // can be changed when the use case arises.
      throw new IllegalStateException(
          String.format(
              "Not expecting more than one proto package."
                  + " Found proto packages for generation: %s",
              protoPackages.toString()));
    } else if (protoPackages.size() == 0) {
      throw new IllegalStateException("No proto files given to generate.");
    }
    parsedArgs.add("--package");
    parsedArgs.add(protoPackages.get(0));

    // Parse plugin params, ignoring unknown params.
    String[] requestArgs = request.getParameter().split(",");
    for (String arg : requestArgs) {
      if (Strings.isNullOrEmpty(arg)) continue;
      parsedArgs.add("--" + arg);
    }

    String[] argsArray = parsedArgs.toArray(new String[] {});

    return GeneratorMain.createCodeGeneratorOptionsFromProtoc(argsArray);
  }

  // Return the list of unique proto packages of the proto files to be generated.
  private static List<String> getProtoPackageList(CodeGeneratorRequest request) {
    // ImmutableSet both enforces uniqueness of elements and preserves insertion order.
    ImmutableSet.Builder<String> packageNames = ImmutableSet.builder();
    for (String fileName : request.getFileToGenerateList()) {
      String protoPackage =
          request
              .getProtoFileList()
              .stream()
              .filter(f -> f.getName().equals(fileName))
              .findAny()
              .get()
              .getPackage();
      packageNames.add(protoPackage);
    }
    return packageNames.build().asList();
  }

  private static String collectDiags(GapicGeneratorApp app) {
    StringBuilder stringBuilder = new StringBuilder();
    for (Diag diag : app.getDiags()) {
      stringBuilder.append(ToolUtil.diagToString(diag, true));
      stringBuilder.append("\n");
    }

    return stringBuilder.toString();
  }
}

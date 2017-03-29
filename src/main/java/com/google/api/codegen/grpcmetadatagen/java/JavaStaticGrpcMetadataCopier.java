/* Copyright 2017 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.grpcmetadatagen.java;

import com.google.api.tools.framework.snippet.Doc;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Responsible for copying static grpc meta-data files for Java */
public class JavaStaticGrpcMetadataCopier {
  private static final String RESOURCE_DIR = "/com/google/api/codegen/java/";

  private static final ImmutableList<String> GRPC_STATIC_FILES =
      ImmutableList.of(
          "gradlew",
          "gradle/wrapper/gradle-wrapper.jar",
          "gradle/wrapper/gradle-wrapper.properties",
          "gradlew.bat",
          "LICENSE",
          "PUBLISHING.md",
          "settings.gradle");

  public static ImmutableMap<String, Doc> run() throws IOException {
    ImmutableMap.Builder<String, Doc> docBuilder = new ImmutableMap.Builder<String, Doc>();
    for (String staticFilePath : GRPC_STATIC_FILES) {
      Path inputPath = Paths.get(RESOURCE_DIR, staticFilePath);
      InputStream stream =
          JavaStaticGrpcMetadataCopier.class.getResourceAsStream(inputPath.toString());
      BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
      StringBuilder output = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line);
      }
      docBuilder.put(staticFilePath, Doc.text(output.toString()));
    }
    return docBuilder.build();
  }
}

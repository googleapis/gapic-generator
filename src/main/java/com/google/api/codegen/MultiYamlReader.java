/* Copyright 2016 Google LLC
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
package com.google.api.codegen;

import com.google.api.tools.framework.model.ConfigSource;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.api.tools.framework.yaml.YamlReader;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.google.protobuf.Message;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public class MultiYamlReader {

  @Nullable
  public static ConfigSource read(
      DiagCollector collector,
      List<String> inputNames,
      List<String> inputs,
      Map<String, Message> supportedConfigTypes) {
    Preconditions.checkArgument(
        inputNames.size() == inputs.size(),
        "size() of inputNames and inputs not equal: %d != %d",
        inputNames.size(),
        inputs.size());
    ConfigSource.Builder sourceBuilder = null;
    for (int i = 0; i < inputs.size(); i++) {
      String inputName = inputNames.get(i);
      String input = inputs.get(i);

      ConfigSource source =
          YamlReader.readConfig(collector, inputName, input, supportedConfigTypes);

      if (source != null) {
        if (sourceBuilder == null) {
          sourceBuilder = source.toBuilder();
        } else {
          sourceBuilder.mergeFrom(source);
        }
      }
    }

    if (sourceBuilder == null) {
      return null;
    } else {
      return sourceBuilder.build();
    }
  }

  @Nullable
  public static ConfigSource read(
      DiagCollector collector, List<File> files, Map<String, Message> supportedConfigTypes) {
    List<String> inputNames = new ArrayList<>();
    List<String> inputs = new ArrayList<>();
    for (File file : files) {
      inputNames.add(file.getName());
      try {
        String fileContent = Files.toString(file, Charset.forName("UTF8"));
        inputs.add(fileContent);
      } catch (IOException e) {
        collector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "Cannot read configuration file '%s': %s",
                file.getName(),
                e.getMessage()));
      }
    }
    if (collector.getErrorCount() > 0) {
      return null;
    } else {
      return read(collector, inputNames, inputs, supportedConfigTypes);
    }
  }
}

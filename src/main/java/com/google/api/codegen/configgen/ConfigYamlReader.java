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
package com.google.api.codegen.configgen;

import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.common.base.Splitter;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.composer.ComposerException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;

/** Reads YAML into a FieldConfigNode. */
public class ConfigYamlReader {
  public FieldConfigNode generateConfigNode(URL file, ConfigHelper helper) {
    int initialErrorCount = helper.getErrorCount();
    String input;
    try (Reader fileReader = new InputStreamReader(file.openStream())) {
      input = CharStreams.toString(fileReader);
    } catch (IOException e) {
      helper.error("Cannot read configuration file: %s", e.getMessage());
      return null;
    }

    if (input.trim().isEmpty()) {
      helper.error("Empty YAML document");
      return null;
    }

    Node tree;
    try {
      tree = new Yaml().compose(new StringReader(input));
    } catch (ComposerException e) {
      helper.error(e.getProblemMark(), "Parsing error: %s", e.getMessage());
      return null;
    } catch (Exception e) {
      helper.error("Parsing error: %s", e.getMessage());
      return null;
    }

    if (tree == null) {
      helper.error("Parsing error");
      return null;
    }

    if (!(tree instanceof MappingNode)) {
      helper.error(tree, "Expected a map as a root object.");
      return null;
    }

    List<String> lines = Splitter.on(System.lineSeparator()).splitToList(input);
    ConfigNode configNode =
        new ConfigYamlNodeReader(lines, helper)
            .readMessageNode((MappingNode) tree, ConfigProto.getDescriptor());
    return helper.getErrorCount() == initialErrorCount
        ? new FieldConfigNode(0, "").setChild(configNode)
        : null;
  }
}

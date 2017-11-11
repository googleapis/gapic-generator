/* Copyright 2017 Google LLC
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
package com.google.api.codegen.configgen.mergers;

import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.configgen.ConfigHelper;
import com.google.api.codegen.configgen.ConfigYamlReader;
import com.google.api.codegen.configgen.InitialConfigLocationGenerator;
import com.google.api.codegen.configgen.MissingFieldTransformer;
import com.google.api.codegen.configgen.NodeFinder;
import com.google.api.codegen.configgen.RefreshConfigLocationGenerator;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.ScalarConfigNode;
import com.google.api.codegen.configgen.nodes.metadata.DefaultComment;
import com.google.api.codegen.configgen.nodes.metadata.FixmeComment;
import com.google.api.tools.framework.model.Model;
import java.io.File;

/** Merges the gapic config from a Model into a ConfigNode. */
public class ConfigMerger {
  private static final String CONFIG_DEFAULT_COPYRIGHT_FILE = "copyright-google.txt";
  private static final String CONFIG_DEFAULT_LICENSE_FILE = "license-header-apache-2.0.txt";
  private static final String CONFIG_PROTO_TYPE = ConfigProto.getDescriptor().getFullName();
  private static final String CONFIG_SCHEMA_VERSION = "1.0.0";
  private static final String CONFIG_COMMENT =
      "Address all the FIXMEs in this generated config before using it for client generation. "
          + "Remove this paragraph after you closed all the FIXMEs."
          + " The retry_codes_name, required_fields, flattening, and timeout properties cannot be "
          + "precisely decided by the tooling and may require some configuration.";

  private final LanguageSettingsMerger languageSettingsMerger = new LanguageSettingsMerger();
  private final InterfaceMerger interfaceMerger = new InterfaceMerger();

  public ConfigNode mergeConfig(Model model) {
    ConfigHelper helper =
        new ConfigHelper(model.getDiagCollector(), new InitialConfigLocationGenerator());
    FieldConfigNode configNode = mergeConfig(model, new FieldConfigNode(""), helper);
    if (configNode == null) {
      return null;
    }

    return configNode.setComment(new FixmeComment(CONFIG_COMMENT));
  }

  public ConfigNode mergeConfig(Model model, File file) {
    ConfigHelper helper =
        new ConfigHelper(
            model.getDiagCollector(), new RefreshConfigLocationGenerator(file.getName()));
    FieldConfigNode configNode = new ConfigYamlReader().generateConfigNode(file, helper);
    if (configNode == null) {
      return null;
    }

    return mergeConfig(model, configNode, helper);
  }

  private FieldConfigNode mergeConfig(
      Model model, FieldConfigNode configNode, ConfigHelper helper) {
    ConfigNode typeNode = mergeType(configNode, helper);
    if (typeNode == null) {
      return null;
    }

    ConfigNode versionNode = mergeVersion(configNode, typeNode, helper);
    if (versionNode == null) {
      return null;
    }

    ConfigNode languageSettingsNode =
        languageSettingsMerger.mergeLanguageSettings(model, configNode, versionNode, helper);
    if (languageSettingsNode == null) {
      return null;
    }

    mergeLicenseHeader(configNode, languageSettingsNode);
    interfaceMerger.mergeInterfaces(model, configNode, helper);

    return configNode;
  }

  private ConfigNode mergeType(ConfigNode configNode, ConfigHelper helper) {
    FieldConfigNode typeNode = MissingFieldTransformer.prepend("type", configNode).generate();
    if (!NodeFinder.hasChild(typeNode)) {
      return typeNode.setChild(new ScalarConfigNode(CONFIG_PROTO_TYPE));
    }

    String type = typeNode.getChild().getText();
    if (CONFIG_PROTO_TYPE.equals(type)) {
      return typeNode;
    }

    helper.error("The specified configuration type '%s' is unknown.", type);
    return null;
  }

  private ConfigNode mergeVersion(ConfigNode configNode, ConfigNode prevNode, ConfigHelper helper) {
    FieldConfigNode versionNode =
        MissingFieldTransformer.insert("config_schema_version", configNode, prevNode).generate();
    if (!NodeFinder.hasChild(versionNode)) {
      return versionNode.setChild(new ScalarConfigNode(CONFIG_SCHEMA_VERSION));
    }

    String version = versionNode.getChild().getText();
    if (CONFIG_SCHEMA_VERSION.equals(version)) {
      return versionNode;
    }

    helper.error("The specified configuration schema version '%s' is unsupported.", version);
    return null;
  }

  private void mergeLicenseHeader(ConfigNode configNode, ConfigNode prevNode) {
    FieldConfigNode licenseHeaderNode =
        MissingFieldTransformer.insert("license_header", configNode, prevNode).generate();

    if (NodeFinder.hasChild(licenseHeaderNode)) {
      return;
    }

    FieldConfigNode copyrightFileNode =
        FieldConfigNode.createStringPair("copyright_file", CONFIG_DEFAULT_COPYRIGHT_FILE)
            .setComment(new DefaultComment("The file containing the copyright line(s)."));
    FieldConfigNode licenseFileNode =
        FieldConfigNode.createStringPair("license_file", CONFIG_DEFAULT_LICENSE_FILE)
            .setComment(
                new DefaultComment(
                    "The file containing the raw license header without any copyright line(s)."));
    licenseHeaderNode
        .setChild(copyrightFileNode.insertNext(licenseFileNode))
        .setComment(
            new DefaultComment(
                "The configuration for the license header to put on generated files."));
  }
}

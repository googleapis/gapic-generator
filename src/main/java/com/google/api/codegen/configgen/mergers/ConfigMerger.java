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
package com.google.api.codegen.configgen.mergers;

import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.configgen.ConfigHelper;
import com.google.api.codegen.configgen.ConfigYamlReader;
import com.google.api.codegen.configgen.MissingFieldTransformer;
import com.google.api.codegen.configgen.NodeFinder;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.ScalarConfigNode;
import com.google.api.codegen.configgen.nodes.metadata.DefaultComment;
import com.google.api.codegen.configgen.nodes.metadata.FixmeComment;
import java.io.File;

/** Merges the gapic config from an ApiModel into a ConfigNode. */
public class ConfigMerger {
  private static final String CONFIG_DEFAULT_COPYRIGHT_FILE = "copyright-google.txt";
  private static final String CONFIG_DEFAULT_LICENSE_FILE = "license-header-apache-2.0.txt";
  private static final String CONFIG_PROTO_TYPE = ConfigProto.getDescriptor().getFullName();
  private static final String CONFIG_SCHEMA_VERSION = "1.0.0";
  private static final String CONFIG_COMMENT =
      "Adiddle fiddle middle liddle "
          + "Remove this paragraph after you closed all the FIXMEs."
          + " The retry_codes_name, required_fields, flattening, and timeout properties cannot be "
          + "precisely decided by the tooling and may require some configuration.";

  private final LanguageSettingsMerger languageSettingsMerger;
  private final InterfaceMerger interfaceMerger;
  private final String packageName;
  private final ConfigHelper helper;

  public ConfigMerger(
      LanguageSettingsMerger languageSettingsMerger,
      InterfaceMerger interfaceMerger,
      String packageName,
      ConfigHelper helper) {
    this.languageSettingsMerger = languageSettingsMerger;
    this.interfaceMerger = interfaceMerger;
    this.packageName = packageName;
    this.helper = helper;
  }

  public ConfigNode mergeConfig(ApiModel model) {
    FieldConfigNode configNode = mergeConfig(model, new FieldConfigNode(0, ""));
    if (configNode == null) {
      return null;
    }

    return configNode.setComment(new FixmeComment(CONFIG_COMMENT));
  }

  public ConfigNode mergeConfig(ApiModel model, File file) {
    FieldConfigNode configNode = new ConfigYamlReader().generateConfigNode(file, helper);
    if (configNode == null) {
      return null;
    }

    return mergeConfig(model, configNode);
  }

  private FieldConfigNode mergeConfig(ApiModel model, FieldConfigNode configNode) {
    ConfigNode typeNode = mergeType(configNode);
    if (typeNode == null) {
      return null;
    }

    ConfigNode versionNode = mergeVersion(configNode, typeNode);
    if (versionNode == null) {
      return null;
    }

    ConfigNode languageSettingsNode =
        languageSettingsMerger.mergeLanguageSettings(packageName, configNode, versionNode);

    mergeLicenseHeader(configNode, languageSettingsNode);
    interfaceMerger.mergeInterfaces(model, configNode);

    return configNode;
  }

  private ConfigNode mergeType(ConfigNode configNode) {
    FieldConfigNode typeNode = MissingFieldTransformer.prepend("type", configNode).generate();
    if (!NodeFinder.hasContent(typeNode.getChild())) {
      return typeNode.setChild(new ScalarConfigNode(typeNode.getStartLine(), CONFIG_PROTO_TYPE));
    }

    String type = typeNode.getChild().getText();
    if (CONFIG_PROTO_TYPE.equals(type)) {
      return typeNode;
    }

    helper.error(
        typeNode.getStartLine(), "The specified configuration type '%s' is unknown.", type);
    return null;
  }

  private ConfigNode mergeVersion(ConfigNode configNode, ConfigNode prevNode) {
    FieldConfigNode versionNode =
        MissingFieldTransformer.insert("config_schema_version", configNode, prevNode).generate();
    if (!NodeFinder.hasContent(versionNode.getChild())) {
      return versionNode.setChild(
          new ScalarConfigNode(versionNode.getStartLine(), CONFIG_SCHEMA_VERSION));
    }

    String version = versionNode.getChild().getText();
    if (CONFIG_SCHEMA_VERSION.equals(version)) {
      return versionNode;
    }

    helper.error(
        versionNode.getStartLine(),
        "The specified configuration schema version '%s' is unsupported.",
        version);
    return null;
  }

  private void mergeLicenseHeader(ConfigNode configNode, ConfigNode prevNode) {
    FieldConfigNode licenseHeaderNode =
        MissingFieldTransformer.insert("license_header", configNode, prevNode).generate();

    if (NodeFinder.hasContent(licenseHeaderNode.getChild())) {
      return;
    }

    FieldConfigNode copyrightFileNode =
        FieldConfigNode.createStringPair(
                NodeFinder.getNextLine(licenseHeaderNode),
                "copyright_file",
                CONFIG_DEFAULT_COPYRIGHT_FILE)
            .setComment(new DefaultComment("The file containing the copyright line(s)."));
    FieldConfigNode licenseFileNode =
        FieldConfigNode.createStringPair(
                NodeFinder.getNextLine(copyrightFileNode),
                "license_file",
                CONFIG_DEFAULT_LICENSE_FILE)
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

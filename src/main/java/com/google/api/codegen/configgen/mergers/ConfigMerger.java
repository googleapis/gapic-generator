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
package com.google.api.codegen.configgen.mergers;

import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.configgen.ConfigHelper;
import com.google.api.codegen.configgen.StringPairTransformer;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.ScalarConfigNode;
import com.google.api.codegen.configgen.nodes.metadata.DefaultComment;
import com.google.api.codegen.configgen.nodes.metadata.FixmeComment;
import com.google.api.tools.framework.model.Model;

public class ConfigMerger {
  private static final String CONFIG_DEFAULT_COPYRIGHT_FILE = "copyright-google.txt";
  private static final String CONFIG_DEFAULT_LICENSE_FILE = "license-header-apache-2.0.txt";
  private static final String CONFIG_PROTO_TYPE = ConfigProto.getDescriptor().getFullName();
  private static final String CONFIG_COMMENT =
      "Address all the FIXMEs in this generated config before using it for client generation. "
          + "Remove this paragraph after you closed all the FIXMEs."
          + " The retry_codes_name, required_fields, flattening, and timeout properties cannot be "
          + "precisely decided by the tooling and may require some configuration.";

  private final LanguageSettingsMerger languageSettingsMerger = new LanguageSettingsMerger();
  private final InterfaceMerger interfaceMerger = new InterfaceMerger();

  public ConfigNode mergeConfig(Model model) {
    ConfigHelper helper = new ConfigHelper(model.getDiagCollector());
    FieldConfigNode configNode = mergeConfig(model, new FieldConfigNode(""), helper);
    if (configNode == null) {
      return null;
    }

    return configNode.setComment(new FixmeComment(CONFIG_COMMENT));
  }

  private FieldConfigNode mergeConfig(
      Model model, FieldConfigNode configNode, ConfigHelper helper) {
    ConfigNode typeNode = mergeType(configNode);
    if (typeNode == null) {
      return null;
    }

    ConfigNode languageSettingsNode =
        languageSettingsMerger.mergeLanguageSettings(model, configNode, typeNode, helper);
    if (languageSettingsNode == null) {
      return null;
    }

    mergeLicenseHeader(configNode, languageSettingsNode);
    interfaceMerger.mergeInterfaces(model, configNode, helper);

    return configNode;
  }

  private ConfigNode mergeType(ConfigNode configNode) {
    FieldConfigNode typeNode =
        new FieldConfigNode("type").setChild(new ScalarConfigNode(CONFIG_PROTO_TYPE));
    configNode.setChild(typeNode);
    return typeNode;
  }

  private void mergeLicenseHeader(ConfigNode configNode, ConfigNode prevNode) {
    FieldConfigNode licenseHeaderNode = new FieldConfigNode("license_header");
    prevNode.insertNext(licenseHeaderNode);
    FieldConfigNode copyrightFileNode =
        StringPairTransformer.generateStringPair("copyright_file", CONFIG_DEFAULT_COPYRIGHT_FILE)
            .setComment(new DefaultComment("The file containing the copyright line(s)."));
    FieldConfigNode licenseFileNode =
        StringPairTransformer.generateStringPair("license_file", CONFIG_DEFAULT_LICENSE_FILE)
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

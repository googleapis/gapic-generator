/* Copyright 2016 Google Inc
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
package io.gapi.vgen;

import com.google.api.tools.framework.aspects.context.ContextConfigAspect;
import com.google.api.tools.framework.aspects.documentation.DocumentationConfigAspect;
import com.google.api.tools.framework.aspects.http.HttpConfigAspect;
import com.google.api.tools.framework.aspects.naming.NamingConfigAspect;
import com.google.api.tools.framework.aspects.system.SystemConfigAspect;
import com.google.api.tools.framework.aspects.versioning.VersionConfigAspect;
import com.google.api.tools.framework.aspects.visibility.VisibilityConfigAspect;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.api.tools.framework.processors.linter.Linter;
import com.google.api.tools.framework.processors.merger.Merger;
import com.google.api.tools.framework.processors.normalizer.Normalizer;
import com.google.api.tools.framework.processors.resolver.Resolver;
import com.google.api.tools.framework.tools.ToolDriverBase;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.api.tools.framework.tools.ToolOptions.Option;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.inject.TypeLiteral;
import com.google.protobuf.Message;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main class for the code generator.
 */
public class CodeGeneratorApi extends ToolDriverBase {

  public static final Option<String> OUTPUT_FILE =
      ToolOptions.createOption(
          String.class,
          "output_file",
          "The name of the output file or folder to put generated code.",
          "");

  public static final Option<List<String>> GENERATOR_CONFIG_FILES =
      ToolOptions.createOption(
          new TypeLiteral<List<String>>() {},
          "config_files",
          "The list of YAML configuration files for the code generator.",
          ImmutableList.<String>of());

  /**
   * Constructs a code generator api based on given options.
   */
  public CodeGeneratorApi(ToolOptions options) {
    super(options);
  }

  @Override
  protected void registerProcessors() {
    model.registerProcessor(new Resolver());
    model.registerProcessor(new Merger());
    model.registerProcessor(new Normalizer());
    model.registerProcessor(new Linter());
  }

  @Override
  protected void registerAspects() {
    model.registerConfigAspect(DocumentationConfigAspect.create(model));
    model.registerConfigAspect(ContextConfigAspect.create(model));
    model.registerConfigAspect(HttpConfigAspect.create(model));
    model.registerConfigAspect(VisibilityConfigAspect.create(model));
    model.registerConfigAspect(VersionConfigAspect.create(model));
    model.registerConfigAspect(NamingConfigAspect.create(model));
    model.registerConfigAspect(SystemConfigAspect.create(model));
  }

  @Override
  protected void process() throws Exception {

    // Read the YAML config and convert it to proto.
    List<String> configFileNames = options.get(GENERATOR_CONFIG_FILES);
    if (configFileNames.size() == 0) {
      error(String.format("--%s must be provided", GENERATOR_CONFIG_FILES.name()));
      return;
    }

    ConfigProto configProto = loadConfigFromFiles(configFileNames);
    if (configProto == null) {
      return;
    }

    for (TemplateProto template : configProto.getTemplatesList()) {
      CodeGenerator generator = CodeGenerator.create(configProto, template, model);
      if (generator == null) {
        return;
      }
      Multimap<ProtoElement, GeneratedResult> docs = ArrayListMultimap.create();
      for (String snippetInputName : template.getSnippetFilesList()) {
        SnippetDescriptor snippetDescriptor = new SnippetDescriptor(snippetInputName);
        Map<ProtoElement, GeneratedResult> code = generator.generate(snippetDescriptor);
        if (code == null) {
          continue;
        }
        for (Map.Entry<ProtoElement, GeneratedResult> entry : code.entrySet()) {
          docs.put(entry.getKey(), entry.getValue());
        }
      }
      generator.output(options.get(OUTPUT_FILE), docs);
    }
  }

  private ConfigProto loadConfigFromFiles(List<String> configFileNames) {
    List<File> configFiles = pathsToFiles(configFileNames);
    if (model.getErrorCount() > 0) {
      return null;
    }
    ImmutableMap<String, Message> supportedConfigTypes =
        ImmutableMap.<String, Message>of(
            ConfigProto.getDescriptor().getFullName(), ConfigProto.getDefaultInstance());
    ConfigProto configProto =
        (ConfigProto) MultiYamlReader.read(model, configFiles, supportedConfigTypes);
    return configProto;
  }

  private List<File> pathsToFiles(List<String> configFileNames) {
    List<File> files = new ArrayList<>();

    for (String configFileName : configFileNames) {
      File file = model.findDataFile(configFileName);
      if (file == null) {
        error("Cannot find configuration file '%s'.", configFileName);
        continue;
      }
      files.add(file);
    }

    return files;
  }

  private void error(String message, Object... args) {
    model.addDiag(Diag.error(SimpleLocation.TOPLEVEL, message, args));
  }
}

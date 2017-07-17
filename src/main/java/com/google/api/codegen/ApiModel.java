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
package com.google.api.codegen;

import com.google.api.Service;
import com.google.api.codegen.discovery.Document;
import com.google.api.tools.framework.model.BoundedDiagCollector;
import com.google.api.tools.framework.model.ConfigSource;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.SymbolTable;
import com.google.api.tools.framework.tools.FileWrapper;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.api.tools.framework.yaml.YamlReader;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Contains exactly one of: Document, Model.
 *
 * <p>This wrapper class abstracts the MVVM model source from the model itself.
 */
public class ApiModel {
  @Nonnull private final ModelType modelType;

  /**
   * TODO(andrealin): Consider moving document to seperate encapsulating class, and then putting
   * that encapsulating class here.
   */
  // Elements present when this ApiModel is based on a Document.
  private final Document document;

  private final SymbolTable docSymbolTable;
  private final DiagCollector docDiagCollector;
  private ConfigSource serviceConfig;

  // Elements present when this ApiModel is based on a proto Model.
  private final Model model;

  /* The source type of the API definition. */
  public enum ModelType {
    DISCOVERY,
    PROTO
  }

  // Constructors enforce the exactly-one-source precondition of this class.

  /** Create an ApiModel from a Discovery document. */
  public ApiModel(Document document, SymbolTable docSymbolTable) {
    Preconditions.checkNotNull(document);
    this.modelType = ModelType.DISCOVERY;
    this.document = document;
    this.docSymbolTable = docSymbolTable;
    this.docDiagCollector = new BoundedDiagCollector();
    this.model = null;
  }

  /** Create an ApiModel from a protobuf that defines an API model. */
  public ApiModel(Model model) {
    Preconditions.checkNotNull(model);
    this.modelType = ModelType.PROTO;
    this.model = model;
    this.docSymbolTable = null;
    this.document = null;
    this.docDiagCollector = null;
  }

  /** @return the type of model that this ApiModel wrapper is based on. */
  public ModelType getModelType() {
    return modelType;
  }

  /** @return the underlying Document model. */
  @Nullable
  public Document getDocument() {
    return document;
  }

  /** @return the underlying protobuf Model model. */
  @Nullable
  public Model getProtoModel() {
    return model;
  }

  public SymbolTable getSymbolTable() {
    switch (getModelType()) {
      case PROTO:
        return model.getSymbolTable();
      case DISCOVERY:
        return docSymbolTable;
      default:
        return null;
    }
  }

  public DiagCollector getDiagCollector() {
    switch (getModelType()) {
      case PROTO:
        return model.getDiagCollector();
      case DISCOVERY:
        return docDiagCollector;
      default:
        return null;
    }
  }

  public ConfigSource getServiceConfigSource() {
    return serviceConfig;
  }

  /** Returns the associated service config raw value. */
  public Service getServiceConfig() {
    return (Service) serviceConfig.getConfig();
  }

  /** Sets the service config from a config source. */
  public void setServiceConfig(ConfigSource source) {
    switch (getModelType()) {
      case PROTO:
        model.setServiceConfig(source);
        break;
      case DISCOVERY:
        this.serviceConfig = source;
        break;
    }
  }

  /** Parse config files. This is a copy of ModelBuilder.parseConfigFiles(). */
  public static Set<FileWrapper> parseConfigFiles(
      ToolOptions options, String builtDataPath, DiagCollector diagCollector) {
    List<FileWrapper> unsanitizedFiles;
    if (!options.get(ToolOptions.CONFIG_FILES).isEmpty()) {
      unsanitizedFiles =
          ToolUtil.readModelConfigs(
              builtDataPath, options.get(ToolOptions.CONFIG_FILES), diagCollector);
    } else {
      unsanitizedFiles = options.get(ToolOptions.CONFIG_FILE_CONTENTS);
    }
    return ToolUtil.sanitizeSourceFiles(unsanitizedFiles);
  }

  /**
   * Sets the service config based on a sequence of sources of heterogeneous types. Sources of the
   * same type will be merged together, and those applicable to the framework will be attached to
   * it.
   */
  public void setConfigSources(List<String> configFileNames, ToolOptions options) {
    // TODO(andrealin): Fill in builtDataPath.
    Set<FileWrapper> configFiles = ApiModel.parseConfigFiles(options, "", getDiagCollector());

    if (modelType == ModelType.PROTO) {
      ToolUtil.setupModelConfigs(model, configFiles);
      return;
    } else if (modelType == ModelType.DISCOVERY) {
      // This is cribbed from Model.setConfigSources().

      ImmutableList.Builder<ConfigSource> builder = ImmutableList.builder();

      for (FileWrapper file : configFiles) {
        ConfigSource message =
            YamlReader.readConfig(
                model.getDiagCollector(),
                file.getFilename(),
                file.getFileContents().toStringUtf8());
        if (message != null) {
          builder.add(message);
        }
      }
      if (getDiagCollector().hasErrors()) {
        return;
      }
      model.setConfigSources(builder.build());
    }
  }
}

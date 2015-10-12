package io.gapi.fx.tools;

import com.google.api.AnnotationsProto;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.ExtensionRegistry;

import io.gapi.fx.model.Diag;
import io.gapi.fx.model.ExtensionPool;
import io.gapi.fx.model.Model;
import io.gapi.fx.model.SimpleLocation;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * Abstract base class for tools run via an API.
 */
public abstract class ToolBase {

  protected final ToolOptions options;
  protected Model model;

  protected ToolBase(ToolOptions options) {
    this.options = options;
  }

  /**
   * Returns the model.
   */
  @Nullable
  public Model getModel() {
    return model;
  }

  /**
   * Check if there are any errors.
   */
  public boolean hasErrors() {
    return model.getErrorCount() > 0;
  }

  /**
   * Returns diagnosis, including errors and warnings.
   */
  public List<Diag> getDiags() {
    return model.getDiags();
  }

  /**
   * Method implementing the actual tool processing.
   */
  protected abstract void process() throws Exception;

  /**
   * Runs the tool.
   */
  public void run() {
    setupModel();

    // Run tool specific code.
    try {
      process();
    } catch (Exception e) {
      model.addDiag(Diag.error(SimpleLocation.TOPLEVEL,
          "Unexpected exception:%n%s", Throwables.getStackTraceAsString(e)));
    }
  }

  /**
   * Initializes the model.
   */
  private void setupModel() {

    // Prevent INFO messages from polluting the log.
    Logger.getLogger("").setLevel(Level.WARNING);

    // Any remaining files considered proto sources.
    ImmutableList<String> protoFiles =
        ToolUtil.sanitizeSourceFiles(options.get(ToolOptions.PROTO_SOURCES));

    // Parse file descriptor set.
    String descriptorName = options.get(ToolOptions.DESCRIPTOR_SET);
    FileDescriptorSet descriptor;
    try {
      descriptor =
          FileDescriptorSet.parseFrom(new FileInputStream(descriptorName), getExtensionRegistry());
      // Create model.
      model = Model.create(descriptor, protoFiles, options.get(ToolOptions.EXPERIMENTS),
          ExtensionPool.EMPTY);
    } catch (IOException e) {
      // Create a dummy model for error reporting
      model = Model.create(FileDescriptorSet.getDefaultInstance(),
          ImmutableList.of(), ImmutableList.of(),
          ExtensionPool.EMPTY);
      model.addDiag(Diag.error(SimpleLocation.TOPLEVEL,
          "Cannot open input file '%s': %s", descriptorName, e.getMessage()));
    }

    if (hasErrors()) {
      return;
    }

    // Register processors.
    registerProcessors();

    // Set data path.
    model.setDataPath(options.get(ToolOptions.DATA_PATH));

    ToolUtil.setupModelConfigs(model, options.get(ToolOptions.CONFIG_FILES));
    if (hasErrors()) {
      return;
    }

    // Register config aspects.
    registerAspects();
  }

  /**
   * Get the extension registry to use. By default, registers service config extensions.
   * Can be overridden by sub-classes.
   */
  protected ExtensionRegistry getExtensionRegistry() {
    ExtensionRegistry registry = ExtensionRegistry.newInstance();
    AnnotationsProto.registerAllExtensions(registry);
    return registry;
  }

  /**
   * Registers processors for this tool.
   */
  protected abstract void registerProcessors();

  /**
   * Registers aspects for this tool.
   */
  protected abstract void registerAspects();
}

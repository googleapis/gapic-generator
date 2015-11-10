package io.gapi.tools.gradle.java

import com.google.api.tools.framework.tools.ToolOptions

import io.gapi.tools.gradle.java.GapiPlugin.ApiServiceConfigurator
import io.gapi.vgen.CodeGeneratorApi
import io.gapi.vsync.Synchronizer

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Task which handles veneer synchronization. Calls the generator and then the synchronizer.
 */
class SynchronizationTask extends DefaultTask {
  private ApiServiceConfigurator apiService

  def initialize(ApiServiceConfigurator apiService) {
    this.apiService = apiService
    this.description =
        "Synchronizes veneer for api service '${apiService.name}' " +
        "in source set '${apiService.sourceSet.name}'"

    // Mark dependency from proto generation
    def sourceSetTag = Util.getSourceSetSubstringForTaskNames(apiService.sourceSet.name)
    dependsOn "generate${sourceSetTag}Proto"

    // Declare inputs
    inputs.files apiService.serviceConfigs
    inputs.files apiService.veneerConfigs
    inputs.file Util.descriptorFile(project, apiService.sourceSet.name)
    inputs.file "${project.projectDir}/build.gradle"
  }

  @TaskAction
  def generate() {
    // Get paths to generated, baseline, and source tree. Ensure that all directories exist
    // and that the generated tree is empty.
    def generatedPath = apiService.sourceSet.generatedBase()
    new File(generatedPath).deleteDir()
    Util.ensurePathExists(generatedPath)
    def baselinePath = Util.ensurePathExists(apiService.sourceSet.baselineBase())
    def sourcePath = Util.ensurePathExists(apiService.sourceSet.sourceBase())

    // Run the code generator.
    ToolOptions options = new ToolOptions()
    options.set(ToolOptions.DESCRIPTOR_SET,
      Util.descriptorFile(project, apiService.sourceSet.name).toString())
    options.set(ToolOptions.CONFIG_FILES, apiService.serviceConfigs*.toString())
    options.set(CodeGeneratorApi.GENERATOR_CONFIG_FILES, apiService.veneerConfigs)
    options.set(CodeGeneratorApi.OUTPUT_FILE, generatedPath.toString())
    logger.debug "Executing veneer generator with: ${options}"
    def codeGen = new CodeGeneratorApi(options)
    codeGen.run()
    Util.handleDiag(codeGen.diags)

    // Run the synchronizer.
    options = new ToolOptions()
    options.set(Synchronizer.SOURCE_PATH, sourcePath.toString())
    options.set(Synchronizer.GENERATED_PATH, generatedPath.toString())
    options.set(Synchronizer.BASELINE_PATH, baselinePath.toString())
    options.set(Synchronizer.AUTO_MERGE, apiService.autoMerge)
    options.set(Synchronizer.AUTO_RESOLUTION, apiService.autoResolution)
    options.set(Synchronizer.IGNORE_BASE, apiService.ignoreBase)
    logger.debug "Executing veneer synchronizer with: ${options}"
    def synchronizer = new Synchronizer(options)
    def diags = synchronizer.run()
    Util.handleDiag(diags)
  }
}


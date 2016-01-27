package io.gapi.tools.gradle.java

import com.google.api.tools.framework.tools.ToolOptions

import io.gapi.tools.gradle.java.GapiPlugin.ApiServiceConfigurator
import io.gapi.vgen.CodeGeneratorApi

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Task which handles veneer generation. Calls the generator.
 */
class VeneerGeneratorTask extends DefaultTask {
  private ApiServiceConfigurator apiService

  private def generatedPath() {
    return apiService.sourceSet.generatedBase()
  }

  def initialize(ApiServiceConfigurator apiService) {
    this.apiService = apiService
    this.group = "Veneer"
    this.description =
        "Generates a veneer for the api service '${apiService.name}' " +
        "in source set '${apiService.sourceSet.name}'"

    // Mark dependency from proto generation
    def sourceSetTag = Util.getSourceSetSubstringForTaskNames(apiService.sourceSet.name)
    dependsOn "generate${sourceSetTag}Proto"

    // Declare inputs and outputs
    inputs.files apiService.serviceConfigs
    inputs.files apiService.veneerConfigs
    inputs.file Util.descriptorFile(project, apiService.sourceSet.name)
    inputs.file "${project.projectDir}/build.gradle"
    outputs.dir generatedPath()
  }

  @TaskAction
  def gen() {
    // Get paths to generated, baseline, and source tree. Ensure that all directories exist
    // and that the generated tree is empty.
    new File(generatedPath()).deleteDir()
    Util.ensurePathExists(generatedPath())

    // Run the code generator.
    ToolOptions options = new ToolOptions()
    options.set(ToolOptions.DESCRIPTOR_SET,
      Util.descriptorFile(project, apiService.sourceSet.name).toString())
    options.set(ToolOptions.CONFIG_FILES, apiService.serviceConfigs*.toString())
    options.set(CodeGeneratorApi.GENERATOR_CONFIG_FILES, apiService.veneerConfigs)
    options.set(CodeGeneratorApi.OUTPUT_FILE, generatedPath().toString())
    logger.debug "Executing veneer generator with: ${options}"
    def codeGen = new CodeGeneratorApi(options)
    codeGen.run()
    Util.handleDiag(codeGen.diags)
    logger.info "${apiService.name} veneer generated in: ${generatedPath().toString()}"
  }
}


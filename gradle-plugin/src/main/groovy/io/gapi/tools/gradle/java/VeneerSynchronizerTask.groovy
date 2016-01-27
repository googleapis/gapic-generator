package io.gapi.tools.gradle.java

import com.google.api.tools.framework.tools.ToolOptions

import io.gapi.tools.gradle.java.GapiPlugin.ApiServiceConfigurator
import io.gapi.vsync.Synchronizer

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Task which handles veneer synchronization. Calls the synchronizer.
 */
class VeneerSynchronizerTask extends DefaultTask {
  private ApiServiceConfigurator apiService

  private def generatedPath() {
    return apiService.sourceSet.generatedBase()
  }
  private def sourcePath() {
    return apiService.sourceSet.sourceBase()
  }
  private def baselinePath() {
    return apiService.sourceSet.baselineBase()
  }

  def initialize(ApiServiceConfigurator apiService) {
    this.apiService = apiService
    this.group = "Veneer"
    this.description =
        "Synchronize the final veneer with the generated veneer " +
        "baseline for the api service '${apiService.name}' " +
        "in source set '${apiService.sourceSet.name}'"

    // Mark dependency on veneer gen
    def sourceSetTag = Util.getSourceSetSubstringForTaskNames(apiService.sourceSet.name)
    dependsOn "generate${sourceSetTag}${apiService.name.capitalize()}"

    // Declare inputs and outputs
    inputs.dir generatedPath()
    inputs.dir baselinePath()
    inputs.dir sourcePath()
    outputs.dir sourcePath()
  }

  @TaskAction
  def syncOnly() {
    // Get paths to generated, baseline, and source tree.
    def baselinePath = Util.ensurePathExists(apiService.sourceSet.baselineBase())
    def sourcePath = Util.ensurePathExists(apiService.sourceSet.sourceBase())

    // Run the synchronizer.
    ToolOptions options = new ToolOptions()
    options.set(Synchronizer.SOURCE_PATH, sourcePath.toString())
    options.set(Synchronizer.GENERATED_PATH, generatedPath().toString())
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

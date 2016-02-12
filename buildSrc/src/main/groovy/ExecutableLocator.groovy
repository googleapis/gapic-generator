import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency


/**
 * Fetch and find the artifact location of an executable dependency.
 */
class ExecutableLocator {

  private final Project project

  ExecutableLocator(Project project) {
    this.project = project
  }

  public String resolve(String dependency) {
    Configuration config = project.configurations.create("ExecutableLocator") {
      visible = false
      transitive = false
      extendsFrom = []
    }
    def groupId, artifact, version
    (groupId, artifact, version) = dependency.split(":")
    def notation = [group: groupId,
                    name: artifact,
                    version: version,
                    classifier: project.osdetector.classifier,
                    ext: 'exe']
    Dependency dep = project.dependencies.add(config.name, notation)
    File file = config.fileCollection(dep).singleFile
    if (!file.canExecute() && !file.setExecutable(true)) {
      throw new GradleException("Cannot set ${file} as executable")
    }
    return file.path
  }
}
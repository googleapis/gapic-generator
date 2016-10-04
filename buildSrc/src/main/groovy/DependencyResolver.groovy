import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency


/**
 * Fetch and resolve dependencies.
 */
class DependencyResolver {

  private final Project project

  DependencyResolver(Project project) {
    this.project = project
  }

  // Fetch and locate archives
  public String locateArchive(String dependency) {
    Configuration config = setupConfig();
    Dependency dep = project.dependencies.add(config.name, dependency)
    return config.fileCollection(dep).singleFile.toString();
  }

  // Fetch and locate executables
  public String resolveExecutable(String dependency) {
    Configuration config = setupConfig();
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

  // Fetch and unzip archives
  public String extractArchive(String dependency) {
    File archive = new File(locateArchive(dependency));

    // Unzip using Ant
    def ant = new AntBuilder()
    def output = archive.getParent().toString() + '/output'
    ant.unzip(  src: archive.toString(),
                dest: output,
                overwrite: "true" )
    return output
  }

  private Configuration setupConfig() {
    Configuration config = project.configurations.create("DependencyResolver") {
      visible = false
      transitive = false
      extendsFrom = []
    }
    return config;
  }
}
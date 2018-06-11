/* Copyright 2016 Google LLC
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
    Configuration config = setupConfig(dependency);
    Dependency dep = project.dependencies.add(config.name, dependency)
    return config.fileCollection(dep).singleFile.toString();
  }

  // Fetch and locate executables
  public String resolveExecutable(String dependency) {
    Configuration config = setupConfig(dependency);
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

  private Configuration setupConfig(String uniqueSuffix) {
    Configuration config = project.configurations.create("DependencyResolver-" + uniqueSuffix) {
      visible = false
      transitive = false
      extendsFrom = []
    }
    return config;
  }
}
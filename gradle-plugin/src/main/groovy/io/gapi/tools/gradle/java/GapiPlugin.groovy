package io.gapi.tools.gradle.java

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.util.ConfigureUtil


/**
 * Gradle plugin for gapi Java development.
 */
class GapiPlugin implements Plugin<Project> {

  private static final String PROTOC_DEFAULT = 'com.google.protobuf:protoc:3.0.0-beta-1'
  private static final String GRPC_PLUGIN_DEFAULT = 'io.grpc:protoc-gen-grpc-java:0.9.0'

  private Project project
  private GapiConfigurator config
  private String generatedSrcDir

  // Installation
  // ============

  @Override
  void apply(final Project project) {
    this.project = project
    this.config = new GapiConfigurator()
    this.generatedSrcDir = "${project.projectDir}/generated/src"

    // Install root configurator.
    project.convention.plugins.gapi = new GapiConvention(gapi: config)

    // Apply plugins and configure.
    project.apply plugin: 'java'
    project.apply plugin: "com.google.protobuf"
    project.apply plugin: 'maven'
    project.apply plugin: 'eclipse'

    project.protobuf {
      generatedFilesBaseDir = generatedSrcDir
      protoc {
        artifact = PROTOC_DEFAULT
      }
      plugins {
        grpc {
          artifact = GRPC_PLUGIN_DEFAULT
        }
      }
      generateProtoTasks {
        all().each { task ->
          task.generateDescriptorSet = true
          task.descriptorSetOptions.path = "${Util.descriptorFile(project, task.sourceSet.name)}"
          task.descriptorSetOptions.includeSourceInfo = true
          task.descriptorSetOptions.includeImports = true
        }
        all()*.plugins {
          grpc {
            outputSubDir = 'java'
          }
        }
        all()*.builtins {
          java {
            outputSubDir = 'java'
          }
        }
      }
    }

    project.afterEvaluate {
      // Register synchronization task for each api service in each source set.
      // We need to do that after project evaluation so all gapi source sets are
      // properly configured.
      config.sourceSets.each { sourceSet ->
        def sourceSetTag = "${Util.getSourceSetSubstringForTaskNames(sourceSet.name)}"
        sourceSet.apiServices.each { apiService ->
          def taskName = "sync${sourceSetTag}${apiService.name.capitalize()}"
          def task = project.tasks.create(taskName, SynchronizationTask) {
            description = "Generates and synchronizes veneer for ${apiService.name}"
            initialize apiService
          }
        }
      }

      // Extend clean task to remove generated proto files
      project.tasks.maybeCreate('clean') << {
        new File(generatedSrcDir).deleteDir()
      }
    }
  }

  // Configurators
  // =============

  /**
   * Convention to register top-level configurator.
   */
  class GapiConvention {
    def GapiConfigurator gapi

    def gapi(Closure configClosure) {
      ConfigureUtil.configure(configClosure, gapi)
    }
  }

  /**
   * Top-level configurator. Used as in:
   * <pre>
   *   gapi {
   *     // Define one or more source sets.
   *     sourceSet { ... }
   *     sourceSet { ... }
   *     ...
   *
   *     // Configure protobuf compiler as needed
   *     protoCompiler { ... }
   *
   *     // Configure grpc compiler plugin as needed
   *     grpcCompilerPlugin { ... }
   *   }
   * </pre>
   */
  class GapiConfigurator {
    private List<SourceSetConfigurator> sourceSets = []

    def sourceSet(Closure configClosure) {
      def configurator = new SourceSetConfigurator()
      ConfigureUtil.configure(configClosure, configurator)
      sourceSets += configurator
    }

    def protoCompiler(Closure configClosure) {
      // TODO(wrwg): find way to avoid access to internal tools property of protobuf plugin
      ConfigureUtil.configure(configClosure, project.protobuf.tools.protoc)
    }

    def grpcCompilerPlugin(Closure configClosure) {
      ConfigureUtil.configure(configClosure, project.protobuf.tools.plugins.grpc)
    }
  }

  /*
   * Configurator for a source set. Used as in:
   *
   * <pre>
   *   sourceSet {
   *     // The name of the source set. Defaults to main.
   *     name "main"
   *
   *     // Register the path(s) where to find proto files.
   *     protoPath "${projectDir}/modules/gapi-example-library/src/proto"
   *
   *     // Define one or more api services
   *     apiService { ... }
   *     apiService { ... }
   *     ...
   *   }
     * </pre>
   */
  class SourceSetConfigurator {
    private String name = SourceSet.MAIN_SOURCE_SET_NAME
    private String protoPath
    private List<List<String>> apiServices = []
    private boolean javaInitialized

    def protoPath(String path) {
      this.protoPath = path
      def javaPath = "${generatedSrcDir}/${name}/java"

      // Add path to proto source set. This makes the protobuf plugin aware of it.
      project.sourceSets."${name}" {
        proto {
          srcDir protoPath
        }
        if (!javaInitialized) {
          javaInitialized = true
          // Ensure the path for the generated java is registered,
          java {
            srcDir javaPath
          }
          // Make eclipse plugin aware.
          // TODO(wrwg): this should happen automatically, but there seems to be an issue with
          // initialization order, so we do it manually right now.
          //project.configurations.eclipse.classpath.file.whenMerged { classpath ->
          //    classpath.entries.add(new SourceFolder(javaPath, null))
          //}
        }
      }
    }

    def apiService(Closure configClosure) {
      def configurator = new ApiServiceConfigurator(sourceSet: this)
      ConfigureUtil.configure(configClosure, configurator)
      apiServices += [configurator]
    }

    private sourceBase() {
      return "${project.projectDir}/src/${name}/java"
    }

    private generatedBase() {
      return "${project.buildDir}/vgen/${name}/java"
    }

    private baselineBase() {
      return "${project.projectDir}/baseline/${name}/java"
    }
  }


  /*
   * Configurator for an api service. Used as in:
   *
   * <pre>
   *   apiService {
   *
   *     // Name of the API service as used in related tasks.
   *     name "library"
   *
   *     // Service configuration file(s)
   *     service_config "google/example/library/library.yaml"
   *
   *     // Veneer configuration file(s)
   *     veneer_config "google/example/library/library_veneer.yaml"
   *
   *     // Optional synchronization settings (showing defaults)
   *     autoMerge = true
   *     autoResolution = true
   *     ignoreBase = false
   *   }
   * </pre>
   */
  class ApiServiceConfigurator {
    private SourceSetConfigurator sourceSet
    private String name = "api"
    private List<String> serviceConfigs = []
    private List<String> veneerConfigs = []

    def autoMerge = true
    def autoResolution = true
    def ignoreBase = false

    def name(String name) {
      this.name = name
    }

    def serviceConfig(String... serviceConfigsInput) {
      serviceConfigsInput.each {
        serviceConfigs += [ Util.relativePath(sourceSet.protoPath, it) ]
      }
    }

    def veneerConfig(String... veneerConfigsInput) {
      veneerConfigsInput.each {
        veneerConfigs += [ Util.relativePath(sourceSet.protoPath, it) ]
      }
    }
  }
}
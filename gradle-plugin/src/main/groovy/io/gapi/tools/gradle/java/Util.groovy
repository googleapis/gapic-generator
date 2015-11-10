package io.gapi.tools.gradle.java

import com.google.api.tools.framework.model.Diag

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet

/**
 * Some static utilities.
 */
class Util {

  /**
   * Returns descriptor file for given source set.
   */
  static String descriptorFile(Project project, String sourceSetName) {
    return "${project.buildDir}/vgen/${sourceSetName}.dsc"
  }

  /**
   * Handles diagnosis, throwing on error and printing warnings.
   */
  static handleDiag(Iterable<Diag> diags) {
    def report = describeDiag(diags)
    if (diags.find { it.getKind() == Diag.Kind.ERROR } != null) {
      // Contains an error, stop and report via exception.
      throw new InvalidUserDataException(report)
    }
    if (!report.isEmpty()) {
      // Contains warnings, report to console
      println report
    }
  }

  /**
   * Returns a string reporting the diagnosis.
   */
  static String describeDiag(Iterable<Diag> diags) {
    def report = ""
    diags.each { diag ->
      if (!report.isEmpty()) {
        report += "\n"
      }
      def message = "${diag.getLocation().getDisplayString()}: ${diag.getMessage()}"
      if (diag.getKind() == Diag.Kind.ERROR) {
        report += "ERROR: ${message}"
      } else {
        report += "WARNING: ${message}"
      }
    }
    return report
  }

  /**
   * Ensure that a path exists, and return it.
   */
  static String ensurePathExists(String path) {
    def folder = new File(path)
    if (!folder.exists()) {
      folder.mkdirs()
    }
    return path
  }

  /**
   * Returns the conventional name of a configuration for a sourceSet
   */
  static String getConfigName(String sourceSetName, String type) {
    return sourceSetName == SourceSet.MAIN_SOURCE_SET_NAME
        ? type : (sourceSetName + type.capitalize())
  }

  /**
   * Returns the conventional substring that represents the sourceSet in task names,
   * e.g., "generate<sourceSetSubstring>Proto"
   */
  static String getSourceSetSubstringForTaskNames(String sourceSetName) {
    return sourceSetName == SourceSet.MAIN_SOURCE_SET_NAME
        ? '' : sourceSetName.capitalize()
  }

  /**
   * Roots a path if it is not absolute.
   */
  static String relativePath(String root, String path) {
    return new File(path).absolute ? path : "${root}/${path}"
  }
}


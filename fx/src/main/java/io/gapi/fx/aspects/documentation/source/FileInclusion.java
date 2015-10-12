package io.gapi.fx.aspects.documentation.source;

import io.gapi.fx.model.DiagCollector;
import io.gapi.fx.model.Location;
import com.google.common.base.Splitter;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nullable;

/**
 * Represents Docgen file inclusion instruction:
 * (== include google3-path ==)
 */
public class FileInclusion extends ContentElement {
  private static final String SUPPORTED_FILE_EXTENSION = "md";

  private final String relativeFilePath;
  private final String resolvedFilePath;
  private final int sectionLevel;
  private final String content;

  public FileInclusion(String docPath, String relativeFilePath, int sectionLevel, int startIndex,
      int endIndex, DiagCollector diagCollector, Location sourceLocation) {
    super(startIndex, endIndex, diagCollector, sourceLocation);
    this.relativeFilePath = relativeFilePath;
    this.sectionLevel = sectionLevel;
    if (!Files.getFileExtension(relativeFilePath).equals(SUPPORTED_FILE_EXTENSION)) {
      error("Not supported file extension: '%s'.", relativeFilePath);
      this.resolvedFilePath = null;
      this.content = null;
    } else {
      this.resolvedFilePath = resolveFilePath(docPath, relativeFilePath);
      if (resolvedFilePath == null) {
        error("File not found by google3 relative path: '%s'. If this was ran through BUILD rule,"
            + " make sure the file was specified in 'documentation_deps' attribute in api_service "
            + "rule", relativeFilePath);
        this.content = null;
      } else {
        this.content = readFileContent();
      }
    }
  }

  /**
   * Returns the included file path relative to google3.
   */
  public String getRelativeFilePath() {
    return relativeFilePath;
  }

  /**
   * Returns the included file name.
   */
  public String getFileName() {
    int index = relativeFilePath.lastIndexOf("/");
    return index < 0 ? relativeFilePath
        : relativeFilePath.substring(index + 1, relativeFilePath.length());
  }

  /**
   * Returns the content of included file if file path is valid.
   * Otherwise, returns null.
   */
  @Override
  @Nullable
  public String getContent() {
    return content;
  }

  /**
   * Returns the section level the file should be included.
   */
  public int getSectionLevel() {
    return sectionLevel;
  }

  private String readFileContent() {
    try {
      return Files.toString(new File(resolvedFilePath), StandardCharsets.UTF_8);
    } catch (IOException e) {
      error("Failed to read file: '%s'.", resolvedFilePath);
      return null;
    }
  }

  private static String resolveFilePath(String docPath, String relativeFilePath) {
    for (String base : Splitter.on(File.pathSeparator).split(docPath)) {
      File file = new File(base, relativeFilePath);
      if (file.canRead()) {
        return file.getPath();
      }
    }
    return null;
  }
}

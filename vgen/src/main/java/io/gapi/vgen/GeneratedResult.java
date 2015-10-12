package io.gapi.vgen;

import io.gapi.fx.snippet.Doc;
import com.google.auto.value.AutoValue;

/**
 * Represents a generated document plus the filename for the document.
 */
@AutoValue
public abstract class GeneratedResult {

  public static GeneratedResult create(Doc doc, String filename) {
    return new AutoValue_GeneratedResult(doc, filename);
  }

  public abstract Doc getDoc();
  public abstract String getFilename();

}

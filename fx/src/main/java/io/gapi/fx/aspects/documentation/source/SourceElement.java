package io.gapi.fx.aspects.documentation.source;

import io.gapi.fx.model.Diag;
import io.gapi.fx.model.DiagCollector;
import io.gapi.fx.model.Location;

/**
 * Represent an abstract element of Markdown source.
 */
abstract class SourceElement {

  private final int startIndex;
  private final int endIndex;
  private final DiagCollector diagCollector;
  private final Location sourceLocation;

  public SourceElement(int startIndex, int endIndex, DiagCollector diagCollector,
      Location sourceLocation) {
    this.startIndex = startIndex;
    this.endIndex = endIndex;
    this.diagCollector = diagCollector;
    this.sourceLocation = sourceLocation;
  }

  /**
   * The start index (inclusive) of the content the node represents from the source.
   */
  public int getStartIndex() {
    return startIndex;
  }

  /**
   * The end index (exclusive) of the content the node represents from the source.
   */
  public int getEndIndex() {
    return endIndex;
  }

  /**
   * Reports error message.
   */
  public void error(String message, Object... params) {
    diagCollector.addDiag(Diag.error(sourceLocation, message, params));
  }
}

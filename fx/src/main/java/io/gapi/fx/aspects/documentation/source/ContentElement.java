package io.gapi.fx.aspects.documentation.source;

import io.gapi.fx.model.DiagCollector;
import io.gapi.fx.model.Location;

/**
 * Represents the content in a document that could be at either file level or section level.
 */
public abstract class ContentElement extends SourceElement {

  public ContentElement(int startIndex, int endIndex, DiagCollector diagCollector,
      Location sourceLocation) {
    super(startIndex, endIndex, diagCollector, sourceLocation);
  }

  /**
   * Returns the content.
   */
  public abstract String getContent();
}

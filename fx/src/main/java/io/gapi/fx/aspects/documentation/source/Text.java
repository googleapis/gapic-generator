package io.gapi.fx.aspects.documentation.source;

import io.gapi.fx.model.DiagCollector;
import io.gapi.fx.model.Location;

/**
 * Represents text content.
 */
public class Text extends ContentElement {
  private final String text;

  public Text(String text, int startIndex, int endIndex, DiagCollector diagCollector,
      Location sourceLocation) {
    super(startIndex, endIndex, diagCollector, sourceLocation);
    this.text = text;
  }

  @Override
  public String getContent() {
    return text;
  }
}

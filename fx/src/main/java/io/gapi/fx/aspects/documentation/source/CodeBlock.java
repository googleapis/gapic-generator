package io.gapi.fx.aspects.documentation.source;

import io.gapi.fx.model.DiagCollector;
import io.gapi.fx.model.Location;

/**
 * Represents code block content.
 */
public class CodeBlock extends ContentElement {

  private final String source;

  public CodeBlock(String source, int startIndex, int endIndex, DiagCollector diagCollector,
      Location sourceLocation) {
    super(startIndex, endIndex, diagCollector, sourceLocation);
    this.source = source;
  }

  @Override
  public String getContent() {
    return source;
  }
}

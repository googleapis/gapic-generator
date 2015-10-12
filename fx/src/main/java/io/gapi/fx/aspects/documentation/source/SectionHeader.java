package io.gapi.fx.aspects.documentation.source;

import io.gapi.fx.model.DiagCollector;
import io.gapi.fx.model.Location;

/**
 * Represent the header of a section.
 */
public class SectionHeader extends SourceElement {

  private final int level;
  private final String text;

  public SectionHeader(int level, String text, int startIndex, int endIndex,
      DiagCollector diagCollector, Location sourceLocation) {
    super(startIndex, endIndex, diagCollector, sourceLocation);
    this.level = level;
    this.text = text;
  }

  /**
   * Returns the heading level.
   */
  public int getLevel() {
    return level;
  }

  /**
   * Returns the header text.
   */
  public String getText() {
    return text;
  }
}

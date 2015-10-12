package io.gapi.fx.aspects.documentation.source;

import io.gapi.fx.model.DiagCollector;
import io.gapi.fx.model.Location;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;

/**
 * Represents section in Markdown content based on Markdown header boundary.
 */
public class SourceSection extends SourceElement {

  private final SectionHeader header;
  private final List<ContentElement> contents = Lists.newArrayList();

  public SourceSection(SectionHeader header, int startIndex, int endIndex,
      DiagCollector diagCollector, Location sourceLocation) {
    super(startIndex, endIndex, diagCollector, sourceLocation);
    this.header = header;
  }

  /**
   * Adds content to the section.
   */
  public void addContents(Collection<ContentElement> contents) {
    this.contents.addAll(contents);
  }

  /**
   * Returns header of the section.
   */
  public SectionHeader getHeader() {
    return header;
  }

  /**
   * Returns contents of the section.
   */
  public Iterable<ContentElement> getContents() {
    return contents;
  }
}

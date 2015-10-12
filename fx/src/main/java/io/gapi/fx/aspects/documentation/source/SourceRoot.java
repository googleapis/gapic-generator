package io.gapi.fx.aspects.documentation.source;

import io.gapi.fx.model.DiagCollector;
import io.gapi.fx.model.Location;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;

/**
 * Root of the Markdown source structure based on Markdown heading levels.
 * Header and associated contents compose a {@link SourceSection}. A document
 * root can contain top level contents that are not enclosed by any headers. For example:
 * <pre>
 *   Top Level content.
 *   # Header1
 * </pre>
 */
public class SourceRoot extends SourceElement {

  private final List<ContentElement> topLevelContents = Lists.newArrayList();
  private final List<SourceSection> sections = Lists.newArrayList();

  public SourceRoot(int startIndex, int endIndex, DiagCollector diagCollector,
      Location sourceLocation) {
    super(startIndex, endIndex, diagCollector, sourceLocation);
  }

  /**
   * Add top level contents to the document.
   */
  public void addTopLevelContents(Collection<ContentElement> contents) {
    topLevelContents.addAll(contents);
  }

  /**
   * Add a section to the document.
   */
  public void addSection(SourceSection section) {
    sections.add(section);
  }

  /**
   * Returns top level contents of the document.
   */
  public Iterable<ContentElement> getTopLevelContents() {
    return topLevelContents;
  }

  /**
   * Returns sections of the document.
   */
  public Iterable<SourceSection> getSections() {
    return sections;
  }
}

package io.gapi.fx.aspects.documentation.source;

import io.gapi.fx.util.Accepts;
import io.gapi.fx.util.GenericVisitor;

/**
 * Base visitor which defines the logic to visit {@link SourceElement}.
 */
public abstract class SourceVisitor extends GenericVisitor<SourceElement> {

  protected SourceVisitor() {
    super(SourceElement.class);
  }

  @Accepts
  public void accept(SourceRoot root) {
    for (ContentElement content : root.getTopLevelContents()) {
      visit(content);
    }
    for (SourceSection section : root.getSections()) {
      visit(section);
    }
  }

  @Accepts
  public void accept(SourceSection section) {
    visit(section.getHeader());
    for (ContentElement content : section.getContents()) {
      visit(content);
    }
  }
}

package io.gapi.fx.aspects.documentation;

import io.gapi.fx.aspects.documentation.model.DocumentationUtil;
import io.gapi.fx.model.DiagCollector;
import io.gapi.fx.model.Element;
import io.gapi.fx.model.Location;
import com.google.common.base.Preconditions;

import javax.annotation.Nullable;

/**
 * Runs the comment filter in order to diagnose issues. Does not actually do comment filtering.
 */
final class CommentChecker implements DocumentationProcessor {
  // TODO(wgg): comments should be treated like regular source elements.

  private final DiagCollector diagCollector;

  /**
   * Creates an instance of {@link CommentChecker}
   */
  public CommentChecker(DiagCollector diagCollector) {
    this.diagCollector = Preconditions.checkNotNull(diagCollector,
        "diagCollector should not be null.");
  }

  /**
   * See {@link DocumentationUtil#filter}.
   */
  @Override
  public String process(@Nullable String source, Location location,
      @Nullable Element element) {
    DocumentationUtil.filter(diagCollector, null, location, source);
    return source;
  }
}

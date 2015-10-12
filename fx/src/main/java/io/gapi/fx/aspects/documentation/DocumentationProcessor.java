package io.gapi.fx.aspects.documentation;

import io.gapi.fx.model.Element;
import io.gapi.fx.model.Location;

/**
 * Interface defines processor that processes documentation source.
 */
interface DocumentationProcessor {

  /**
   * Runs the processor for the given source. Returns the processed source string if no error found.
   * Otherwise, the original string should be returned.
   *
   * @param source the documentation source to be processed.
   * @param sourceLocation the location of the documentation source.
   * @param element the element for which the documentation source is being processed
   */
  String process(String source, Location sourceLocation, Element element);
}

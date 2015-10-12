package io.gapi.fx.aspects.documentation;

import io.gapi.fx.model.Element;
import io.gapi.fx.model.Location;
import io.gapi.fx.model.Model;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;

/**
 * Processor set that contains a set of registered {@link DocumentationProcessor}s to process
 * given documentation in a chained way.
 */
public class DocumentationProcessorSet {

  private final List<DocumentationProcessor> processors;

  /**
   * Returns {@link DocumentationProcessorSet} with standard processors registered.
   */
  public static DocumentationProcessorSet standardSetup(Model model) {
    return new DocumentationProcessorSet(Lists.newArrayList(
        new CommentReferenceResolver(model),
        new SourceNormalizer(model, model.getDataPath()),
        new CommentChecker(model)));
  }

  public DocumentationProcessorSet(Collection<DocumentationProcessor> processors) {
    Preconditions.checkNotNull(processors, "processors should not be null");
    this.processors = Lists.newArrayList(processors);
  }

  /**
   * Processes given documentation source by the registered processor chain. Returns processed
   * documentation string.
   */
  public String process(String source, Location location, Element element) {
    if (Strings.isNullOrEmpty(source)) {
      return source;
    }

    String result = source;
    for (DocumentationProcessor processor : processors) {
      result = processor.process(result, location, element);
    }
    return result;
  }
}

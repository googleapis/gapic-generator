package io.gapi.fx.aspects.documentation;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import io.gapi.fx.aspects.documentation.source.CodeBlock;
import io.gapi.fx.aspects.documentation.source.FileInclusion;
import io.gapi.fx.aspects.documentation.source.Instruction;
import io.gapi.fx.aspects.documentation.source.SectionHeader;
import io.gapi.fx.aspects.documentation.source.SourceParser;
import io.gapi.fx.aspects.documentation.source.SourceRoot;
import io.gapi.fx.aspects.documentation.source.SourceVisitor;
import io.gapi.fx.aspects.documentation.source.Text;
import io.gapi.fx.model.DiagCollector;
import io.gapi.fx.model.Element;
import io.gapi.fx.model.Location;
import io.gapi.fx.model.SimpleLocation;
import io.gapi.fx.util.VisitsAfter;
import io.gapi.fx.util.VisitsBefore;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Set;

/**
 * Normalizes documentation source by substituting file inclusion instructions with external
 * content.
 */
class SourceNormalizer implements DocumentationProcessor {

  private static final Joiner ARROW_JOINER = Joiner.on(" -> ");

  private final DiagCollector diagCollector;
  private final String docPath;

  public SourceNormalizer(DiagCollector diagCollector, String docPath) {
    Preconditions.checkNotNull(diagCollector, "diagCollector should not be null.");
    this.diagCollector = diagCollector;
    this.docPath = docPath;
  }

  /**
   * Normalizes documentation source by substituting file inclusion instructions with external
   * content. The heading levels of external content will be adjusted based on parent's heading
   * level so that they will be nested headings of parent. For example:
   * <pre>
   *   docset1.md file:
   *   # Docset 1
   *    (== include docset2.md ==)
   *
   *   docset2.md file:
   *   # Docset2
   *
   *   After the normalization, the result would be:
   *   # Docset1
   *   ## Docset2
   * </pre>
   *
   * @return normalized source, or source if errors detected
   */
  @Override
  public String process(String source, Location sourceLocation, Element element) {
    if (Strings.isNullOrEmpty(source)) {
      return source;
    }
    Normalizer normalizer = new Normalizer();
    String result = normalizer.normalize(source, sourceLocation, element);
    return result;
  }

  /**
   * Helper class to do actual normalization.
   */
  private class Normalizer extends SourceVisitor {

    /**
     * Tracks the file inclusion chain to detect cyclic inclusion.
     */
    private final Set<String> fileInclusionPath = Sets.newLinkedHashSet();

    /**
     * Stack of base section levels used for adjusting section levels for included content.
     */
    private final Deque<Integer> baseSectionLevels = new LinkedList<>(ImmutableList.of(0));

    private final StringBuilder builder = new StringBuilder();
    private Location location = SimpleLocation.UNKNOWN;
    private Element element;

    private String normalize(String source, Location location, Element element) {
      Preconditions.checkNotNull(source, "source should not be null.");
      Preconditions.checkNotNull(location, "location should not be null.");
      this.element = element;
      int errorCount = diagCollector.getErrorCount();
      SourceParser parser = new SourceParser(source, location, diagCollector, docPath);
      SourceRoot root = parser.parse();
      Location savedLocation = this.location;
      this.location = location;
      visit(root);
      this.location = savedLocation;
      return diagCollector.getErrorCount() > errorCount ? source : builder.toString();
    }

    /**
     * Visits {@link Text} element to append its content directly to the normalized result.
     */
    @VisitsBefore
    void normalize(Text text) {
      builder.append(text.getContent());
    }

    /**
     * Visits {@link CodeBlock} element to append its content directly to the normalized result.
     */
    @VisitsBefore
    void normalize(CodeBlock codeBlock) {
      builder.append(codeBlock.getContent());
    }

    /**
     * Visits {@link Instruction} element, evaluating it and appending content (usually empty).
     */
    @VisitsBefore
    void normalize(Instruction instruction) {
      instruction.evalute(element);
      builder.append(instruction.getContent());
    }

    /**
     * Visits {@link FileInclusion} element to recursively resolve the file reference and append
     * the external content to the normalized result.
     */
    @VisitsBefore
    boolean normalize(FileInclusion inclusion) {
      String filePath = inclusion.getRelativeFilePath();

      // Stop visiting if cyclic file inclusion is detected.
      if (fileInclusionPath.contains(filePath)) {
        String path = ARROW_JOINER.join(fileInclusionPath);
        inclusion.error("Cyclic file inclusion detected for '%s' via %s", filePath, path);
        return false;
      }
      String content = inclusion.getContent();
      if (Strings.isNullOrEmpty(content)) {
        return false;
      }

      // Save state before normalizing included content.
      fileInclusionPath.add(filePath);
      baseSectionLevels.addLast(baseSectionLevels.peekLast() + inclusion.getSectionLevel());
      normalize(content, new SimpleLocation(inclusion.getFileName()), element);
      builder.append('\n');
      return true;
    }

    /**
     * Cleans up state after visiting the {@link FileInclusion} elements.
     */
    @VisitsAfter
    void afterNormalize(FileInclusion inclusion) {
      fileInclusionPath.remove(inclusion.getRelativeFilePath());
      baseSectionLevels.removeLast();
    }

    /**
     * Visits the {@link SectionHeader} element to adjust the heading level according to the
     * current base heading level.
     */
    @VisitsBefore
    void normalize(SectionHeader header) {
      int sectionLevel = baseSectionLevels.peekLast() + header.getLevel();
      builder.append(String.format(
          "%s %s\n\n", Strings.repeat("#", sectionLevel), header.getText()));
    }
  }
}

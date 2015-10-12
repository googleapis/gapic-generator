package io.gapi.fx.aspects.documentation.model;

import com.google.api.Page;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import io.gapi.fx.model.Diag;
import io.gapi.fx.model.DiagCollector;
import io.gapi.fx.model.Location;
import io.gapi.fx.model.Model;
import io.gapi.fx.model.ProtoElement;
import io.gapi.fx.model.SimpleLocation;

import org.apache.commons.lang3.StringEscapeUtils;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Static utilities for dealing with documentation.
 */
public class DocumentationUtil {
  private DocumentationUtil() {}

  /**
   * Get the documentation pages from given model.
   */
  public static List<Page> getToplevelPages(Model model) {
    return model.hasAttribute(DocumentationPagesAttribute.KEY)
        ? model.getAttribute(DocumentationPagesAttribute.KEY).toplevelPages()
        : ImmutableList.<Page>of();
  }

  /**
   * Get the documentation pages scoped to the visibility as currently set in the model.
   */
  public static List<Page> getScopedToplevelPages(Model model) {
    ImmutableList.Builder<Page> scopedPages = ImmutableList.builder();
    for (Page page : getToplevelPages(model)) {
      scopedPages.add(doPageScoping(model, page));
    }
    return scopedPages.build();
  }

  private static Page doPageScoping(Model model, Page page) {
    Page.Builder scopedPage = page.toBuilder().clearSubpages();
    scopedPage.setContent(
        new CommentFilter(model, SimpleLocation.TOPLEVEL, model.getVisibilityLabels())
            .process(page.getContent()));
    for (Page subpage : page.getSubpagesList()) {
      scopedPage.addSubpages(doPageScoping(model, subpage));
    }
    return scopedPage.build();
  }

  /**
   * Given an ProtoElement, returns its associated description.
   * Returns the empty string if no description is available.
   */
  public static String getDescription(ProtoElement element) {
    return getDescription(element, "");
  }

  /**
   * Get the description of the element scoped to the visibility as currently set in the model.
   *
   */
  public static String getScopedDescription(ProtoElement element) {
    Model model = element.getModel();
    Location location = element.getLocation();
    return new CommentFilter(model, location, model.getVisibilityLabels())
        .process(getDescription(element));
  }

  /**
   * Given a proto element, returns its associated description. Returns {@code defaultText}
   * if no description is available.
   */
  public static String getDescription(ProtoElement element, String defaultText) {
    return element.hasAttribute(ElementDocumentationAttribute.KEY)
        ? element.getAttribute(ElementDocumentationAttribute.KEY).documentation()
        : defaultText;
  }

  /**
   * Given a model, returns its associated documentation root url based on documentation and
   * legacy configuration. Returns empty string if not available.
   */
  public static String getDocumentationRootUrl(Model model) {
    if (model.getServiceConfig() == null) {
      return "";
    }
    if (model.getServiceConfig().hasDocumentation() && !Strings.isNullOrEmpty(
        model.getServiceConfig().getDocumentation().getDocumentationRootUrl())) {
      return model.getServiceConfig().getDocumentation().getDocumentationRootUrl();
    }
    // TODO: MIGRATION
    //else if (model.getServiceConfig().hasLegacy()
    //    && model.getServiceConfig().getLegacy().hasDevconsole()) {
    //  return model.getServiceConfig().getLegacy().getDevconsole().getLearnmoreUrl();
    //}
    return "";
  }

  /**
   * Given a documentation string, escape it such that it can be represented as a JSON string.
   */
  public static String asJsonString(String text) {
    if (text == null) {
      return "";
    }
    return StringEscapeUtils.ESCAPE_JSON.translate(text);
  }

  /**
   * Given a documentation string, replace the cross reference links with reference text.
   */
  public static String removeCrossReference(String text) {
    if (Strings.isNullOrEmpty(text)) {
      return "";
    }
    Pattern pattern = Pattern.compile("\\[(?<name>[^\\]]+?)\\]( |\\n)*"
        + "\\[(?<link>[^\\]]*?)\\]");
    Matcher matcher = pattern.matcher(text);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      String replacementText = matcher.group("name");
      replacementText = Matcher.quoteReplacement(replacementText);
      matcher.appendReplacement(result, replacementText);
    }
    matcher.appendTail(result);
    return result.toString();
  }

  /**
   * Performs comment filtering. Parse the text to remove non visible comments enclosed by
   * "(-- --)" tags. It works as follows:
   * <ul>
   * <li>All leading and trailing white spaces surrounding tags will be replaced with one space.
   * For example:
   * <pre>
   *   "Foo (-- internal --) foo"
   *   will be returned as
   *   "Foo foo"
   * </pre>
   * Except, if internal comment is at the beginning of the line, no space will be
   * inserted. For example:
   * <pre>
   *   "(-- internal --) foo"
   *   will be returned as
   *   "foo"
   * </pre>
   *
   * <li>Lines with only internal comments (all text enclosed by tags) will be removed.
   * For example:
   * <pre>
   *   "Foo (-- internal --)
   *   (-- internal --) (-- internal2 --)
   *   Foo"
   *   will be returned as
   *   "Foo
   *   Foo"
   * <pre>
   *
   * <li> If visibility label "(--LABEL: ... --) is specified, the comments will be kept if the
   * label is found in the provided visibility labels. If no label was specified, the comments will
   * be filtered.
   * <li> Nested tags are valid, but should appear in pair.
   * <li> It only reports the first encountered error if multiple ones exist.
   * </ul>
   */
  public static String filter(DiagCollector collector, @Nullable Set<String> visibilityLabels,
      Location location, @Nullable String source) {
    return new CommentFilter(collector, location, visibilityLabels).process(source);
  }

  /**
   * Helper class to filter comments.
   */
  private static class CommentFilter {

    private static final String NEW_LINE = "\n";

    private final DiagCollector diagCollector;
    @Nullable private final Set<String> labels;
    private final Location location;

    /**
     * Creates an instance of {@link CommentFilter}
     */
    private CommentFilter(DiagCollector diagCollector, Location location,
        @Nullable Set<String> labels) {
      this.diagCollector = Preconditions.checkNotNull(diagCollector,
          "diagCollector should not be null.");
      this.labels = labels;
      this.location = Preconditions.checkNotNull(location, "location should not be null.");
    }

    public String process(@Nullable String source) {
      if (Strings.isNullOrEmpty(source)) {
        return source;
      }

      CommentTokenizer tokenizer = new CommentTokenizer(source);
      StringBuilder builder = new StringBuilder();
      while (tokenizer.hasNext()) {
        Token token = tokenizer.peekNext();
        switch (token.kind) {
          case BEGIN_INTERNAL_COMMENT:
            if (!handleInternalComment(tokenizer, builder, location)) {
              return source;
            }
            break;
          case TEXT:
            appendText(tokenizer, builder);
            break;
          default:
            collectError(location, token.lineNum,
                "Unexpected end tag '--)' with missing begin tag.");
            return source;
        }
      }
      String result = builder.toString();
      // Remove last newline.
      return result.endsWith(NEW_LINE) ? result.substring(0, result.length() - 1) : result;
    }

    /**
     * Handles internal comments based on visibility label.
     *
     * @param tokenizer tokens of original comment source
     * @param builder the builder that builds processed comment strings to the result
     * @param location the location of the comment source
     * @return true if no error found. Otherwise returns false
     */
    private boolean handleInternalComment(CommentTokenizer tokenizer, StringBuilder builder,
        Location location) {
      Token beginTag = tokenizer.pollNext();
      boolean shouldFilter = Strings.isNullOrEmpty(beginTag.label)
          || labels != null && !labels.contains(beginTag.label);
      while (tokenizer.hasNext()) {
        switch (tokenizer.peekNext().kind) {
          case BEGIN_INTERNAL_COMMENT:
            if (!handleInternalComment(tokenizer, builder, location)) {
              return false;
            }
            break;
          case TEXT:
            if (shouldFilter) {
              skipText(tokenizer);
            } else {
              appendText(tokenizer, builder);
            }
            break;
          default:
            // Found closing tag.
            Token endTag = tokenizer.pollNext();
            // If the internal text should be preserved, and the end tag ends with new line,
            // the new line should be kept.
            if (!shouldFilter && endTag.text.endsWith(NEW_LINE)) {
              builder.append(NEW_LINE);
            }
            return true;
        }
      }

      collectError(location, beginTag.lineNum,
          "Did not find associated end tag for the begin tag '(--'");
      return false;
    }

    /**
     * Consumes consecutive text tokens and append them to builder.
     */
    private void appendText(CommentTokenizer tokenizer, StringBuilder builder) {
      while (tokenizer.hasNext() && tokenizer.peekNext().kind == TokenKind.TEXT) {
        String text = tokenizer.pollNext().text;
        // Append a whitespace, if the position the text to be appended is not the
        // beginning of the line and text to be appended is not newline.
        if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '\n'
            && !text.equals(NEW_LINE)) {
          builder.append(' ');
        }
        builder.append(text);
      }
    }

    /**
     * Consumes consecutive text tokens.
     */
    private void skipText(CommentTokenizer tokenizer) {
      while (tokenizer.hasNext() && tokenizer.peekNext().kind == TokenKind.TEXT) {
        tokenizer.pollNext();
      }
    }

    /**
     * Collects error messages.
     *
     * @param location the location of the original comment source
     * @param lineNum the line number where the error is detected
     * @param message the message describes the error
     */
    private void collectError(Location location, int lineNum, String message) {
      diagCollector.addDiag(Diag.error(new SimpleLocation(
          String.format("%s (at docment line %d)", location.getDisplayString(), lineNum)),
          message));
    }

    /**
     * Tokenizes given comment source by internal comment tags "(--LABEL" and "--)".
     */
    private static class CommentTokenizer {
      private static final Pattern ACL_LABEL = Pattern.compile("[A-Z_]+:");
      private static final Pattern BEGIN_TAG = Pattern.compile(String.format(
          " *\\(--(%s)? *", ACL_LABEL));
      private static final Pattern END_TAG = Pattern.compile(" *--\\) *\\n?");

      private static final Pattern TOKEN = Pattern.compile(String.format(
          "(%s)|(%s)|(\n)", BEGIN_TAG, END_TAG));

      private static final int BEGIN_TAG_GROUP = 1;
      private static final int ACL_LABEL_GROUP = 2;
      private static final int END_TAG_GROUP = 3;

      private final Matcher matcher;
      private final String source;
      private Token currentToken;

      /**
       * The line number of the current token inside the source.
       */
      private int lineNum = 1;

      /**
       * The index of source string.
       */
      private int index = 0;

      /**
       * Creates an instance of {@link CommentTokenizer} for given comment source.
       */
      private CommentTokenizer(String source) {
        this.source = Preconditions.checkNotNull(source, "source should not be null.");
        this.matcher = TOKEN.matcher(source);
        pollNext();
      }

      /**
       * Returns next token without consuming it.
       */
      private Token peekNext() {
        return currentToken;
      }

      /**
       * Determines if it has more tokens.
       */
      private boolean hasNext() {
        return currentToken != null;
      }

      /**
       * Returns next token. The matcher will move forward.
       */
      private Token pollNext() {
        Token result = currentToken;
        if (matcher.find()) {
          if (index < matcher.start()) {
            // There is text between current position and next matching.
            currentToken = new Token(TokenKind.TEXT, source.substring(index, matcher.start()),
                lineNum);
            index = matcher.start();
            matcher.region(index, matcher.regionEnd());
          } else {
            currentToken = createTokenFromMatcher();
            index = matcher.end();
          }
        } else {
          if (index < source.length()) {
            // Add trailing text.
            currentToken = new Token(TokenKind.TEXT, source.substring(index), lineNum);
            index = source.length();
          } else {
            // Reaches the end of source.
            currentToken = null;
          }
        }
        return result;
      }

      private Token createTokenFromMatcher() {
        Token result;
        if (matcher.group(BEGIN_TAG_GROUP) != null) {
          result = new Token(TokenKind.BEGIN_INTERNAL_COMMENT, matcher.group(BEGIN_TAG_GROUP),
              matcher.group(ACL_LABEL_GROUP), lineNum);
        } else if (matcher.group(END_TAG_GROUP) != null) {
          String endTag = matcher.group(END_TAG_GROUP);
          if (endTag.endsWith(NEW_LINE)) {
            lineNum++;
          }
          result = new Token(TokenKind.END_INTERNAL_COMMENT, endTag, lineNum);
        } else {
          // Matches newline.
          result = new Token(TokenKind.TEXT, NEW_LINE, lineNum);
          lineNum++;
        }
        return result;
      }
    }

    /**
     * Represents matched token.
     */
    private static class Token {
      private final TokenKind kind;
      private final String text;
      private final int lineNum;

      /**
       * ACL label name. Could be set for BEGIN_INTERNAL_COMMENT token.
       */
      @Nullable private final String label;

      private Token(TokenKind kind, String text, int lineNum) {
        this(kind, text, null, lineNum);
      }

      private Token(TokenKind kind, String text, @Nullable String label, int lineNum) {
        this.kind = Preconditions.checkNotNull(kind, "kind should not be null.");
        this.text = Preconditions.checkNotNull(text, "text should not be null.");
        this.label = label == null ? null : label.substring(0, label.length() - 1);
        this.lineNum = lineNum;
      }
    }

    /**
     * Represent token kind.
     */
    private static enum TokenKind {
      BEGIN_INTERNAL_COMMENT,
      END_INTERNAL_COMMENT,
      TEXT;
    }
  }
}

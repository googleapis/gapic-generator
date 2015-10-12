package io.gapi.fx.aspects.documentation.source;

import io.gapi.fx.model.DiagCollector;
import io.gapi.fx.model.Location;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser that parses given Markdown source into {@link SourceElement} structure.
 */
public class SourceParser {

  /**
   * <pre>Setext-style headers:
   *     Header 1
   *     ========
   *
   *     Header 2
   *     -------- </pre>
   * Based on standard Markdown.pl
   */
  private static final Pattern SETEXT_HEADING = Pattern.compile(
      "(?<=^|\n)(.+)"      // Header text
      + "[ \t]*"
      + "\n(=+|-+)"        // Header level
      + "[ \t]*"
      + "\n+");

  /**
   * <pre>atx-style headers:
   *   # Header 1
   *   ## Header 2
   *   ## Header 2 with closing hashes ##
   *   ...
   *   ###### Header 6 </pre>
   * We will match any number of '#'s for normalized subsections.
   */
  private static final Pattern ATX_HEADING = Pattern.compile(
      "(?<=^|\n)(#+)"      // Header level
      + "[ \t]*"
      + "(.+?)"             // Header text
      + "[ \t]*"
      + "#*"
      + "\n+");

  private static final Pattern HEADING = Pattern.compile(String.format(
      "%s|%s", SETEXT_HEADING, ATX_HEADING));

  private static final int SEXEXT_HEADING_TEXT = 1;
  private static final int SEXEXT_HEADING_LEVEL = 2;
  private static final int ATX_HEADING_LEVEL = 3;
  private static final int ATX_HEADING_TEXT = 4;

  /**
   * Instruction. Syntax:
   *     (== code arg ==)
   */
  private static final Pattern INSTRUCTION = Pattern.compile(
      "\\(=="       // Begin tag
      + "\\s*"
      + "(?<instrcode>[\\w-]+)"  // Instruction code
      + "\\s+"
      + "(?<instrarg>(.|\\n)+?)"    // Instruction arg
      + "\\s*"
      + "==\\)(?:\n|\\Z)?");  // End tag

  private static final Pattern CODE_BLOCK = Pattern.compile(
      "(?<=\\n\\n|\\A)"
      + "(?<codeblocksource>" // the code block -- one or more lines, starting with a space/tab
      + "(?:"
      + "(?:\\s{4}|\\t)"      // # Lines must start with a tab or a tab-width of spaces
      + ".*\n*"
      + ")+"
      + ")"
      + "((?=\\s{0,3}\\S)|\\Z)");   // Lookahead for non-space at line-start, or end of doc

  private static final Pattern HTML_CODE_BLOCK = Pattern.compile(
      // If the HTML code block is preceeded by two newlines, we strip one. This does not affect
      // DocGen's final, rendered HTML output. But it ensures that G3doc, which adds an extra
      // newline around code blocks, does not end up surrounding codeblocks with three newlines
      // rather than the expected two.
      "((?<=\\n)(?:(\\s*\\n\\s*)?)|)"
      + "(?<htmlcodeblocksource>"
      + "<pre(|\\s.*)>(.|\n)*?"
      + "</pre>)"
      + "((?:\\s*\\n)?(?=\\s*\\n)|)"); // Ignore possible following newline to prevent duplication
                                       // by G3doc; see preceeding comment.

  private static final Pattern CONTENT_PARSING_PATTERNS =
      Pattern.compile(String.format("(?<instr>%s)|(?<codeblock>%s)|(?<htmlcodeblock>%s)",
                      INSTRUCTION,
                      CODE_BLOCK,
                      HTML_CODE_BLOCK));

  private static final String INSTRUCTION_GROUP = "instr";
  private static final String INSTRUCTION_CODE = "instrcode";
  private static final String INSTRUCTION_ARG = "instrarg";
  private static final String CODE_BLOCK_SOURCE_GROUP = "codeblocksource";
  private static final String HTML_CODE_BLOCK_SOURCE_GROUP = "htmlcodeblocksource";
  private static final String INCLUSION_CODE = "include";

  private final DiagCollector diagCollector;
  private final Location sourceLocation;
  private final String source;
  private final String docPath;

  public SourceParser(String source, Location sourceLocation, DiagCollector diagCollector,
      String docPath) {
    this.source = source;
    this.sourceLocation = sourceLocation;
    this.diagCollector = diagCollector;
    this.docPath = docPath;
  }

  /**
   * Parses given Markdown source and generates model of {@link SourceRoot}. The generated model
   * is based on Markdown header sections.
   */
  public SourceRoot parse() {
    SourceRoot root = new SourceRoot(0, source.length(), diagCollector, sourceLocation);
    SectionHeader curHeader = null;
    Matcher headerMatcher = HEADING.matcher(source);
    while (headerMatcher.find()) {
      SectionHeader nextHeader = createHeader(headerMatcher);
      fillContents(source, root, curHeader, nextHeader);
      curHeader = nextHeader;
    }
    fillContents(source, root, curHeader, null);
    return root;
  }

  /**
   * Fills {@link ContentElement}s into model from region between given header boundaries in the
   * source. If content elements are top level, they will be filled into
   * {@link SourceRoot} directly. Otherwise a {@link SourceSection} will be created and filled with
   * those content elements.
   */
  private void fillContents(String source, SourceRoot root, SectionHeader curHeader,
      SectionHeader nextHeader) {
    List<ContentElement> contents = parseContents(source, curHeader, nextHeader);
    if (curHeader == null) {
      // Add top level contents to source root.
      root.addTopLevelContents(contents);
    } else {
      // Create a section with curHeader as header and fill parsed content elements into
      // the section
      int sectionEnd = nextHeader == null ? source.length() : nextHeader.getStartIndex();
      SourceSection section = new SourceSection(curHeader, curHeader.getStartIndex(),
          sectionEnd, diagCollector, sourceLocation);
      section.addContents(contents);
      root.addSection(section);
    }
  }

  /**
   * Parse the source region between given section headers boundary to generate
   * {@link ContentElement}s.
   */
  private List<ContentElement> parseContents(String source, SectionHeader curHeader,
      SectionHeader nextHeader) {
    List<ContentElement> contents = Lists.newArrayList();
    // Decide the source region for the content based on two header boundaries.
    int curIndex = curHeader == null ? 0 : curHeader.getEndIndex();
    int end = nextHeader == null ? source.length() : nextHeader.getStartIndex();
    if (curIndex >= end) {
      return contents;
    }

    Matcher matcher = CONTENT_PARSING_PATTERNS.matcher(source).region(curIndex, end);
    while (matcher.find()) {
      if (matcher.start() > curIndex) {
        // Extract text content between current index and start of found inclusion instruction.
        String text = source.substring(curIndex, matcher.start());
        contents.add(new Text(text, curIndex, matcher.start(),
            diagCollector, sourceLocation));
      }

      ContentElement newElement;
      if (matcher.group(INSTRUCTION_GROUP) != null) {
        int headingLevel = curHeader == null ? 0 : curHeader.getLevel();
        String code = matcher.group(INSTRUCTION_CODE);
        if (INCLUSION_CODE.equals(code)) {
          // Create content element for found file inclusion instruction.
          newElement = new FileInclusion(docPath, matcher.group(INSTRUCTION_ARG).trim(),
              headingLevel, matcher.start(), matcher.end(), diagCollector, sourceLocation);
        } else {
          // Create content element for other instruction.
          newElement = new Instruction(code, matcher.group(INSTRUCTION_ARG),
              matcher.start(), matcher.end(), diagCollector, sourceLocation);
        }
      } else {
        // Create content element for code block.
        if (matcher.group(CODE_BLOCK_SOURCE_GROUP) != null) {
          newElement = new CodeBlock(matcher.group(CODE_BLOCK_SOURCE_GROUP),
                                     matcher.start(),
                                     matcher.end(),
                                     diagCollector,
                                     sourceLocation);
        } else {
          newElement = new CodeBlock(matcher.group(HTML_CODE_BLOCK_SOURCE_GROUP),
                                     matcher.start(),
                                     matcher.end(),
                                     diagCollector,
                                     sourceLocation);
        }
      }

      contents.add(newElement);
      curIndex = matcher.end();
    }

    // Extract trailing text content.
    if (curIndex < end) {
      contents.add(new Text(source.substring(curIndex, end), curIndex, end,
          diagCollector, sourceLocation));
    }
    return contents;
  }

  /**
   * Create {@link SectionHeader} instance based on matching result.
   */
  private SectionHeader createHeader(Matcher matcher) {
    int level;
    String text;
    if (!Strings.isNullOrEmpty(matcher.group(ATX_HEADING_LEVEL))) {
      level = matcher.group(ATX_HEADING_LEVEL).length();
      text = matcher.group(ATX_HEADING_TEXT);
    } else {
      level = matcher.group(SEXEXT_HEADING_LEVEL).startsWith("=") ? 1 : 2;
      text = matcher.group(SEXEXT_HEADING_TEXT);
    }
    return new SectionHeader(level, text, matcher.start(), matcher.end(),
        diagCollector, sourceLocation);
  }
}

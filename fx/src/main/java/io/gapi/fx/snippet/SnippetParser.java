package io.gapi.fx.snippet;

import io.gapi.fx.snippet.Elem.Block;
import io.gapi.fx.snippet.Elem.Case;
import io.gapi.fx.snippet.Elem.Cond;
import io.gapi.fx.snippet.Elem.Lit;
import io.gapi.fx.snippet.Elem.Operator;
import io.gapi.fx.snippet.Elem.Switch;
import io.gapi.fx.snippet.Snippet.SnippetKind;
import io.gapi.fx.snippet.SnippetSet.EvalException;
import io.gapi.fx.snippet.SnippetSet.InputSupplier;
import io.gapi.fx.snippet.SnippetSet.Issue;
import io.gapi.fx.snippet.SnippetSet.SnippetKey;
import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Internal class for parsing snippets.
 */
class SnippetParser {

  private static final Pattern FIND_INDENT = Pattern.compile("^\\s*");
  private static final Pattern SKIP_LINE = Pattern.compile("\\s*#.*");
  private static final Pattern COMMAND_LINE = Pattern.compile("^\\s*@(\\w+)");
  private static final int COMMAND_NAME_GROUP = 1;

  private static final Pattern IDENTIFIER = Pattern.compile("\\w+");
  private static final Pattern STRING_LITERAL = Pattern.compile("\"(([^\"]|\\[\"])*)\"");
  private static final Pattern INT_LITERAL = Pattern.compile("[0-9]+");
  private static final Pattern SEPARATOR = Pattern.compile("[)(.,:=!<>]");
  private static final Pattern LITERAL = Pattern.compile(String.format(
      "%s|%s",
      STRING_LITERAL.pattern(),
      INT_LITERAL.pattern()));
  private static final Pattern EXPR_TOKEN = Pattern.compile(String.format(
      "\\s*(%s|%s|%s)",
      LITERAL.pattern(),
      IDENTIFIER.pattern(),
      SEPARATOR.pattern()));
  private static final int EXPR_TOKEN_GROUP = 1;

  private static final String SNIPPET_COMMAND = "snippet";
  private static final String OVERRIDE_COMMAND = "override";
  private static final String ABSTRACT_COMMAND = "abstract";
  private static final String PRIVATE_COMMAND = "private";
  private static final String END_COMMAND = "end";
  private static final String IF_COMMAND = "if";
  private static final String ELSE_COMMAND = "else";
  private static final String JOIN_COMMAND = "join";
  private static final String LET_COMMAND = "let";
  private static final String SWITCH_COMMAND = "switch";
  private static final String CASE_COMMAND = "case";
  private static final String DEFAULT_COMMAND = "default";
  private static final String EXTENDS_COMMAND = "extends";
  private static final String JOIN_SEPARATOR = "on";

  private static class Input {
    private final String name;
    private final Iterator<String> lines;
    @Nullable private final Input parent;
    private int lineNo;
    private boolean noExtendsAllowed;

    private Input(String name, Iterator<String> lines, Input parent) {
      this.name = name;
      this.lines = lines;
      this.parent = parent;
      this.lineNo = 0;
    }

    private String getPath() {
      if (parent != null) {
        return parent.getPath() + File.pathSeparator + name;
      }
      return name;
    }

    private Location location() {
      return Location.create(name, lineNo);
    }
  }

  private final InputSupplier inputSupplier;
  private final List<Issue> errors = Lists.newArrayList();
  private final SnippetSet snippetSet = new SnippetSet();
  private final Set<String> inputsIncluded = Sets.newHashSet();
  private Input input;
  private String lastTerminator;
  private String lastTerminatorHeader;

  /**
   * Constructs a snippet parser for the given input.
   */
  SnippetParser(InputSupplier inputSupplier, String inputName) {
    this.inputSupplier = inputSupplier;
    this.input = openInput(inputName);
  }

  /**
   * Open input.
   */
  private Input openInput(String name) {
    Iterable<String> lines;
    try {
      lines = inputSupplier.readInput(name);
      if (lines == null) {
        error("cannot open source '%s'", name);
        lines = ImmutableList.of();
      }
    } catch (IOException e) {
      error("cannot open source '%s': %s", name, e.getMessage());
      lines = ImmutableList.of();
    }
    return new Input(name, lines.iterator(), input);
  }

  /**
   * Runs the parser on the configured input.
   */
  void parse() {
    String line;
    while ((line = getNextLine()) != null) {

      // See if it is a command.
      Matcher matcher = COMMAND_LINE.matcher(line);
      if (matcher.find()) {

        // Extract command name and rest of command line.
        String command = matcher.group(COMMAND_NAME_GROUP);
        String rest = line.substring(matcher.end());

        switch (command) {
          case EXTENDS_COMMAND:
            parseExtends(rest);
            break;
          case SNIPPET_COMMAND:
            input.noExtendsAllowed = true;
            parseSnippet(SnippetKind.REGULAR, rest);
            break;
          case OVERRIDE_COMMAND:
            input.noExtendsAllowed = true;
            parseSnippet(SnippetKind.OVERRIDE, rest);
            break;
          case ABSTRACT_COMMAND:
            input.noExtendsAllowed = true;
            parseSnippet(SnippetKind.ABSTRACT, rest);
            break;
          case PRIVATE_COMMAND:
            input.noExtendsAllowed = true;
            parseSnippet(SnippetKind.PRIVATE, rest);
            break;
          default:
            unexpectedCommandError(command);
            break;
        }
      } else if (!CharMatcher.WHITESPACE.matchesAllOf(line)) {

        // Report that the input line is unrecognized.
        error("unrecognized input line on top level: '%s'", line);
      }
    }
  }

  /**
   * Returns accumulated parsing errors.
   */
  List<Issue> errors() {
    return errors;
  }

  /**
   * Returns the parsing result as a snippet set.
   */
  SnippetSet result() {
    return snippetSet;
  }

  private void parseExtends(String header) {

    // Parse header.
    TokenStream tokens = new TokenStream(header);
    String inputName = tokens.expect(STRING_LITERAL);
    tokens.checkAtEnd();
    if (input.noExtendsAllowed) {
      error("'@extends' commands only allowed at the beginning of the source");
    } else if (inputName != null && inputsIncluded.add(inputName)) {

      // Open the input. It will be closed automatically when the last line is read.
      input = openInput(trimLiteral(inputName));
    }
  }

  /**
   * Parses a snippet.
   */
  private void parseSnippet(SnippetKind snippetKind, String header) {

    // Parse header.
    TokenStream tokens = new TokenStream(header);

    // Expect the snippet name.
    String name = tokens.expect(IDENTIFIER);
    List<String> paramList = Lists.newArrayList();

    // Remember the parameter names used, so we can report errors for duplicate params.
    Set<String> usedParamNames = Sets.newHashSet();

    // Must have a parameter list.
    tokens.expect("(");

    if (!tokens.has(")")) {
      for (;;) {
        String param = tokens.expect(IDENTIFIER);
        if (!usedParamNames.add(param)) {
          error("duplicate parameter '%s'", param);
        }
        if (param != null) {
          paramList.add(param);
        }

        // See whether there are more parameters.
        if (tokens.has(",")) {
          tokens.next();
        } else {
          break;
        }
      }
    }
    tokens.expect(")");

    // Parse modifiers
    List<Elem> elems = ImmutableList.of();
    Layout layout = Layout.DEFAULT;

    if (snippetKind != SnippetKind.ABSTRACT) {
      layout = parseLayout(tokens);
      tokens.checkAtEnd();
      elems = parseUntil(layout.groupKind() == Doc.GroupKind.VERTICAL ? 0 : -1,
          layout, END_COMMAND);
    } else {
      tokens.checkAtEnd();
    }

    // Add snippet definition.
    Location location = Location.create(input.getPath(), input.lineNo);
    String fullName = snippetKind == SnippetKind.PRIVATE ?
        Context.makePrivateSnippetName(location, name) : name;
    Snippet old = snippetSet.get(fullName, paramList.size());
    Snippet snippet = Snippet.create(location, fullName,
        snippetKind, old, layout, paramList, elems);
    snippetSet.add(snippet);
    if (snippetKind == SnippetKind.OVERRIDE && old == null) {
      error("no previous snippet '%s' to override", snippet.displayName());
    }
    if (old != null) {
      if (snippetKind != SnippetKind.OVERRIDE) {
        error("must use '@override name(...) ...' to override snippet '%s'",
            old.displayName());
      } else if (old.location().inputName().equals(snippet.location().inputName())) {
        error("cannot override snippet '%s' defined in the same source at line %s",
            old.displayName(), old.location().lineNo());
      } else if (!old.location().inputName().startsWith(snippet.location().inputName())) {
        error("snippet '%s' cannot be overridden from here since it is not in the extension "
            + "path.%n Current path: %s%n Original path: %s, line %s",
            snippet.displayName(), snippet.location().inputName(), old.location().inputName(),
            old.location().lineNo());
      }
    }
  }

  /**
   * Parse a layout specification.
   */
  private Layout parseLayout(TokenStream tokens) {
    Doc.GroupKind kind = Doc.GroupKind.VERTICAL;
    Doc separator = Doc.BREAK;
    int nest = 0;

    if (tokens.has("vertical")) {
      tokens.next();
      kind = Doc.GroupKind.VERTICAL;
    } else if (tokens.has("horizontal")) {
      tokens.next();
      kind = Doc.GroupKind.HORIZONTAL;
    } else if (tokens.has("auto")) {
      tokens.next();
      kind = Doc.GroupKind.AUTO;
    } else if (tokens.has("fill")) {
      tokens.next();
      kind = Doc.GroupKind.FILL;
    }
    if (tokens.has(INT_LITERAL)) {
      nest = Integer.parseInt(tokens.next());
    }

    if (tokens.has(JOIN_SEPARATOR)) {
      tokens.next();
      Elem expr = parseExpr(tokens);
      if (expr != null) {
        separator = evalParsingTime(expr);
      }
    }
    return Layout.create(separator, kind, nest);
  }


  /**
   * Eval an element at parsing time, based solely on the builtin context.
   */
  private Doc evalParsingTime(Elem elem) {
    try {
      return Values.convertToDoc(elem.eval(new Context(ImmutableMap.<SnippetKey, Snippet>of())));
    } catch (EvalException e) {
      error("parsing time evaluation error: %s", e.getMessage());
      return Doc.BREAK;
    }
  }

  /**
   * Trims a literal, removing enclosing {@code "} delimiters if present.
   */
  private String trimLiteral(String lit) {
    if (lit.startsWith("\"")) {
      lit = lit.substring(1);
    }
    if (lit.endsWith("\"")) {
      lit = lit.substring(0, lit.length() - 1);
    }
    return lit;
  }

  /**
   * Parses lines for elements until one of the terminator commands is hit. This will leave
   * the found terminator and terminator parameter in {@link #lastTerminator} and
   * {@link #lastTerminatorHeader}, respectively. Uses the indentation of the first line in
   * sequence to trim indentation of remaining lines. Thus if we have:
   * <pre>
   *   &#064;someuntil
   *     line1
   *       line2
   *    line3
   * </pre>
   * .. all lines lines after line1 will have trimmed indentation by 2 or less (line3 will have
   * only trimmed indentation by 1 because it has less then 2 spaces to work on).
   */
  private List<Elem> parseUntil(int indent, Layout layout, String... terminators) {
    lastTerminator = null;
    lastTerminatorHeader = null;
    List<Elem> elems = Lists.newArrayList();
    String line;
    int firstLineIndent = -1;
    boolean firstContent = true;
    while ((line = getNextLine()) != null) {

      int lineIndent = findIndent(line);

      // Remember the first lines indentation, as it is an anchor for the remaining lines.
      if (firstLineIndent < 0 && lineIndent < line.length()) {
        firstLineIndent = lineIndent;
      }

      // Compute the effective indent based on block indent and first line's indent, and trim
      // the line. Indent < 0 here means horizontal or auto mode where we trim all indent.
      int effectiveIndent = indent < 0 ? indent : indent + (lineIndent - firstLineIndent);
      if (lineIndent > effectiveIndent) {
        // Trim superfluous indent.
        if (effectiveIndent < 0) {
          line = line.substring(lineIndent);
        } else {
          line = line.substring(lineIndent - effectiveIndent);
        }
      }

      // Check for command.
      Matcher matcher = COMMAND_LINE.matcher(line);
      if (matcher.find()) {
        String command = matcher.group(COMMAND_NAME_GROUP);
        String rest = line.substring(matcher.end());

        // Check whether command is one of the expected terminators.
        for (String terminator : terminators) {
          if (terminator.equals(command)) {
            // Remember the seen terminator, as well as the rest of its line.
            lastTerminator = terminator;
            lastTerminatorHeader = rest;
            return elems;
          }
        }

        // Check for other commands
        switch (command) {
          case IF_COMMAND:
            parseIf(effectiveIndent, firstContent, rest, layout, elems);
            break;
          case JOIN_COMMAND:
            parseJoin(effectiveIndent, firstContent, rest, layout, elems);
            break;
          case LET_COMMAND:
            parseLet(effectiveIndent, firstContent, rest, layout, elems);
            break;
          case SWITCH_COMMAND:
            parseSwitch(effectiveIndent, firstContent, rest, layout, elems);
            break;
          default:
            unexpectedCommandError(command);
        }
      } else {

        // Parse the line as content.
        if (firstContent) {
          firstContent = false;
        } else {
          elems.add(Lit.create(input.location(), layout.separator()));
        }
        parseLine(elems, line);
      }
    }
    error("missing '@end' terminator");
    return elems;
  }

  private int findIndent(String line) {
    Matcher matcher = FIND_INDENT.matcher(line);
    matcher.find();
    return matcher.group().length();
  }

  /**
   * Checks whether a header (rest of a command line) is empty and report error if not.
   */
  private void checkHeaderEmpty(String command, String header) {
    if (!Strings.isNullOrEmpty(header)) {
      error("command '@%s' has unexpected arguments", command);
    }
  }

  /**
   * Parse an if command.
   */
  private void parseIf(int indent, boolean firstContent, String header, Layout layout,
      List<Elem> elems) {
    TokenStream tokens = new TokenStream(header);
    Elem cond = parseExpr(tokens);
    tokens.checkAtEnd();
    List<Elem> thenElems = parseUntil(indent, layout, END_COMMAND, ELSE_COMMAND);
    List<Elem> elseElems = null;
    if (ELSE_COMMAND.equals(lastTerminator)) {
      elseElems = parseUntil(indent, layout, END_COMMAND);
    }
    if (cond != null && thenElems != null) {
      elems.add(
          Block.create(!firstContent, Cond.create(input.location(), cond,
              thenElems, elseElems)));
    }
  }

  /**
   * Parse a for command.
   */
  private void parseJoin(int indent, boolean firstContent, String header, Layout layout,
      List<Elem> elems) {
    TokenStream tokens = new TokenStream(header);
    String var = tokens.expect(IDENTIFIER);
    tokens.expect(":");
    Elem generator = parseExpr(tokens);
    Layout joinLayout = parseLayout(tokens);
    tokens.checkAtEnd();
    List<Elem> bodyElems = parseUntil(indent, layout, END_COMMAND);
    if (var != null && generator != null && bodyElems != null && layout != null) {
      elems.add(
          Block.create(!firstContent,
              Elem.Join.create(input.location(), var, generator, joinLayout,
                  bodyElems)));
    }
  }

  /**
   * Parse a let command.
   */
  private void parseLet(int indent, boolean firstContent, String header, Layout layout,
      List<Elem> elems) {
    TokenStream tokens = new TokenStream(header);
    Elem let = parseLetBindingsThenBody(indent, firstContent, tokens, layout);
    if (let != null) {
      elems.add(Block.create(!firstContent, let));
    }
  }

  /**
   * Recursively parse bindings and let body. The form let x = e, y = d ... end
   * is reduced to let x = e let y = d ... end end.
   */
  private Elem parseLetBindingsThenBody(int indent, boolean firstContent, TokenStream tokens,
      Layout layout) {
    String var = tokens.expect(IDENTIFIER);
    tokens.expect("=");
    Elem value = parseExpr(tokens);
    List<Elem> bodyElems;
    if (tokens.has(",")) {
      // Parse more bindings.
      tokens.next();
      Elem let = parseLetBindingsThenBody(indent, firstContent, tokens, layout);
      bodyElems = let != null ? ImmutableList.of(let) : ImmutableList.<Elem>of();
    } else {
      // End of bindings.
      tokens.checkAtEnd();
      bodyElems = parseUntil(indent, layout, END_COMMAND);
    }
    if (var != null && value != null && bodyElems != null) {
      return Elem.Let.create(input.location(), var, value, bodyElems);
    }
    return null;
  }

  /**
   * Parse a switch command.
   */
  private void parseSwitch(int indent, boolean firstContent, String header, Layout layout,
      List<Elem> elems) {
    TokenStream tokens = new TokenStream(header);
    Elem selector = parseExpr(tokens);
    tokens.checkAtEnd();

    ImmutableList.Builder<Case> cases = ImmutableList.builder();
    List<Elem> defaultElems = null;
    boolean done;
    String line = getNextLine();
    Matcher matcher = COMMAND_LINE.matcher(line);
    String command;
    String rest;
    if (matcher.find()) {
      command = matcher.group(COMMAND_NAME_GROUP);
      rest = line.substring(matcher.end());
      done = false;
    } else {
      error("expected '@end', '@case' or '@default' command after 'switch'");
      done = true;
      command = null;
      rest = null;
    }

    while (!done) {
      switch (command) {
        case END_COMMAND:
          checkHeaderEmpty(command, rest);
          done = true;
          break;
        case DEFAULT_COMMAND:
          checkHeaderEmpty(command, rest);
          if (defaultElems != null) {
            error("duplicate '@default' in @switch");
          }
          defaultElems = parseUntil(indent, layout, END_COMMAND);
          command = lastTerminator;
          rest = lastTerminatorHeader;
          done = lastTerminator == null;
          break;
        case CASE_COMMAND:
          tokens = new TokenStream(rest);
          Elem value = parseExpr(tokens);
          tokens.checkAtEnd();
          List<Elem> caseElems = parseUntil(indent, layout,
              END_COMMAND, CASE_COMMAND, DEFAULT_COMMAND);
          if (value != null) {
            cases.add(Case.create(value, caseElems));
          }
          command = lastTerminator;
          rest = lastTerminatorHeader;
          done = lastTerminator == null;
          break;
        default:
          unexpectedCommandError(command);
          done = true;
          break;
      }
    }
    if (selector != null) {
      elems.add(Block.create(!firstContent,
          Switch.create(input.location(), selector, cases.build(), defaultElems)));
    }
  }

  /**
   * Parse a line into elements.
   */
  private void parseLine(List<Elem> elems, String line) {
    int i = 0;
    boolean lastWasOpenBrace = false;
    StringBuilder sb = new StringBuilder();
    while (i < line.length()) {
      char ch = line.charAt(i++);
      if (ch == '@') {
        if (i < line.length() && line.charAt(i) == '@') {
          // Looking at @@ escape of @
          sb.append('@');
          i++;
        } else if (i < line.length() && line.charAt(i) == '\\') {
          // Looking at @\ escape of \
          sb.append('\\');
          i++;
        } else if (i < line.length() && line.charAt(i) == '#') {
          // Looking at @# escape of #
          sb.append('#');
          i++;
        }  else if (lastWasOpenBrace) {
          // Looking at embedded expression {@...
          sb.deleteCharAt(sb.length() - 1);
          flushLiteral(elems, sb);
          i = parseExpr(elems, line, i);
        } else {
          sb.append('@');
          i++;
        }
        lastWasOpenBrace = false;
      } else {
        lastWasOpenBrace = ch == '{';
        sb.append(ch);
      }
    }
    flushLiteral(elems, sb);
  }

  /**
   * Flushes a literal read so far.
   */
  private void flushLiteral(List<Elem> elems, StringBuilder sb) {
    if (sb.length() > 0) {
      elems.add(Lit.create(input.location(), Doc.text(sb.toString())));
      sb.delete(0, sb.length());
    }
  }

  /**
   * Parses a expression.
   */
  private int parseExpr(List<Elem> elems, String line, int i) {
    int braceLevel = 1;
    StringBuilder sb = new StringBuilder();
    while (i < line.length()) {
      char ch = line.charAt(i++);
      switch (ch) {
        case '{':
          sb.append('{');
          braceLevel++;
          break;
        case '}':
          if (--braceLevel == 0) {
            TokenStream tokens = new TokenStream(sb.toString());
            Elem elem = parseExpr(tokens);
            tokens.checkAtEnd();
            if (elem != null) {
              elems.add(elem);
            }
            return i;
          }
          // Fall through
        default:
          sb.append(ch);
          break;
      }
    }
    new TokenStream(sb.toString()).syntaxError("expected '}' to close expression");
    return i;
  }

  /**
   * Parse an expression.
   */
  private Elem parseExpr(TokenStream tokens) {
    Elem expr = parsePrimaryExpr(tokens);
    Operator.Kind operator = parseOperator(tokens);
    if (operator != null) {
      Elem right = parsePrimaryExpr(tokens);
      if (expr != null && right != null) {
        expr = Elem.Operator.create(input.location(), operator, expr, right);
      }
    }
    return expr;
  }

  /**
   * Check for and get an operator kind.
   */
  @Nullable private Operator.Kind parseOperator(TokenStream tokens) {
    if (tokens.has("=")) {
      tokens.next();
      tokens.expect("=");
      return Operator.Kind.EQUALS;
    }
    if (tokens.has("!")) {
      tokens.next();
      tokens.expect("=");
      return Operator.Kind.NOT_EQUALS;
    }
    if (tokens.has("<")) {
      tokens.next();
      if (tokens.has("=")) {
        tokens.next();
        return Operator.Kind.LESS_EQUAL;
      }
      return Operator.Kind.LESS;
    }
    if (tokens.has(">")) {
      tokens.next();
      if (tokens.has("=")) {
        tokens.next();
        return Operator.Kind.GREATER_EQUAL;
      }
      return Operator.Kind.GREATER;
    }
    return null;
  }

  /**
   * Parse a primary expression.
   */
  private Elem parsePrimaryExpr(TokenStream tokens) {
    Elem expr = null;

    if (tokens.has(LITERAL)) {
      String lit = tokens.next();
      expr = Elem.Lit.create(input.location(), Doc.text(trimLiteral(lit)));
    } else if (tokens.has(IDENTIFIER)) {
      String var = tokens.expect(IDENTIFIER);
      if (tokens.has("(")) {

        // Snippet call: name(arg1, ...)
        List<Elem> args = parseOptionalArgs(tokens);
        if (var != null) {
          expr = Elem.Call.create(input.location(), var, args);
        }
      } else if (var != null) {
        expr = Elem.Ref.create(input.location(), var);
      }
    } else {
      tokens.syntaxError("expected identifier or literal");
    }

    // Repeated selection
    while (tokens.has(".")) {
      tokens.next();
      String member = tokens.expect(IDENTIFIER);
      List<Elem> args = parseOptionalArgs(tokens);
      if (member != null && expr != null) {
        expr = Elem.Reflect.create(input.location(), expr, member, args);
      }
    }
    return expr;
  }

  /**
   * Parse an optional list of arguments.
   */
  private List<Elem> parseOptionalArgs(TokenStream tokens) {
    ImmutableList.Builder<Elem> args = ImmutableList.builder();
    if (tokens.has("(")) {
      tokens.next();
      if (!tokens.has(")")) {
        for (;;) {
          Elem arg = parseExpr(tokens);
          if (arg != null) {
            args.add(arg);
          }
          if (tokens.has(",")) {
            tokens.next();
          } else {
            break;
          }
        }
      }
      tokens.expect(")");
    }
    return args.build();
  }

  /**
   * Emit error for an unexpected command.
   */
  private void unexpectedCommandError(String command) {
    error("command '@%s' is unexpected in this context", command);
  }

  /**
   * Emit error.
   */
  private void error(String message, Object...args) {
    if (input == null) {
      errors.add(Issue.create(Location.TOP_LEVEL, message, args));
    } else {
      errors.add(Issue.create(input.location(), message, args));
    }
  }

  /**
   * Get the next line from the input, skipping comments, and merging multiple lines
   * separated by '\'.
   */
  private String getNextLine() {
    String result = "";
    while (input.lines.hasNext()) {
      String line = input.lines.next();
      input.lineNo++;
      if (SKIP_LINE.matcher(line).matches()) {
        continue;
      }
      line = CharMatcher.WHITESPACE.trimTrailingFrom(line);
      if (!Strings.isNullOrEmpty(result)) {
        // If appended to previous line, trim leading space.
        line = CharMatcher.WHITESPACE.trimLeadingFrom(line);
      }
      if (line.endsWith("\\") && !line.endsWith("@\\")) {
        line = line.substring(0, line.length() - 1);
        result += line;
      } else {
        return result + line;
      }
    }
    if (input.parent != null) {
      // Close this input and return to the outer one.
      input = input.parent;
      return getNextLine();
    }
    return null;
  }

  /**
   * A class representing a token stream.
   */
  private class TokenStream {

    private final Matcher matcher;
    private final String expr;
    private boolean matched;
    private boolean hasSynErrors;

    private TokenStream(String expr) {
      this.expr = expr.trim();
      this.matcher = EXPR_TOKEN.matcher(expr);
      this.matched = this.matcher.find();
    }

    /**
     * Get the next token from the stream.
     */
    private String next() {
      String current = matcher.group(EXPR_TOKEN_GROUP);
      matched = matcher.find();
      return current;
    }

    /**
     * Check whether the next token matches the spec, which can either be a string or
     * a pattern.
     */
    private boolean has(Object spec) {
      if (!matched) {
        return false;
      }
      String match = matcher.group(EXPR_TOKEN_GROUP);
      if (spec instanceof Pattern) {
        return ((Pattern) spec).matcher(match).matches();
      }
      return spec.equals(match);
    }

    /**
     * Expect the specified token, and move next.
     */
    private String expect(Object spec) {
      if (!has(spec)) {
        syntaxError(String.format("expected '%s' looking at: ", spec));
        return null;
      }
      return next();
    }

    /**
     * Check whether all tokens have been consumed, and report error if not.
     */
    private void checkAtEnd() {
      if (!hasSynErrors && matched) {
        syntaxError("unrecognized input: ");
      }
    }

    /**
     * Report an error.
     */
    private void syntaxError(String message) {
      hasSynErrors = true;
      int at = matched ? matcher.start(EXPR_TOKEN_GROUP) : expr.length();
      error("%s%n  %s%n  %s", message.trim(), expr, Strings.padStart("^", at + 1, ' '));
    }
  }
}

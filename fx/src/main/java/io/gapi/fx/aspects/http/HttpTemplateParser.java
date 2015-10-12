package io.gapi.fx.aspects.http;

import io.gapi.fx.aspects.http.model.HttpAttribute.FieldSegment;
import io.gapi.fx.aspects.http.model.HttpAttribute.LiteralSegment;
import io.gapi.fx.aspects.http.model.HttpAttribute.PathSegment;
import io.gapi.fx.aspects.http.model.HttpAttribute.WildcardSegment;
import io.gapi.fx.model.Diag;
import io.gapi.fx.model.DiagCollector;
import io.gapi.fx.model.Location;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP path template parser.
 */
public class HttpTemplateParser {

  private static final Pattern LITERAL_PATTERN = Pattern.compile("[^/*}{=]+");
  private static final Pattern TOKEN_PATTERN =
      Pattern.compile(String.format("%s|[/*}{=]", LITERAL_PATTERN));
  private static final Pattern CUSTOM_VERB_PATTERN =
      Pattern.compile(String.format("(?<!/):(%s)$", LITERAL_PATTERN));
  private static final Pattern CUSTOM_VERB_PATTERN_ILLEGAL =
      Pattern.compile(String.format("/:(%s)$", LITERAL_PATTERN));

  private final DiagCollector diagCollector;
  private final Location location;
  private final String template;
  private final String customVerb;
  private final Matcher tokens;
  private String current;
  private boolean pathStartedWithSlash;
  private boolean hadErrors;
  private int configVersion;

  /**
   * Constructs a template parser. Errors are reported to the diag collector at the given
   * location.
   */
  public HttpTemplateParser(DiagCollector diagCollector, Location location, String template,
      int configVersion) {
    this.diagCollector = diagCollector;
    this.location = location;
    this.template = template;
    this.configVersion = configVersion;
    Matcher matcher = CUSTOM_VERB_PATTERN.matcher(template);
    if (matcher.find()) {
      this.customVerb = matcher.group(1);
      this.tokens = TOKEN_PATTERN.matcher(template.substring(0, matcher.start()));
    } else {
      this.customVerb = null;
      this.tokens = TOKEN_PATTERN.matcher(template);
    }
    this.current = tokens.find() ? tokens.group().trim() : null;
  }

  /**
   * Runs the parser.
   */
  public ImmutableList<PathSegment> parse() {
    ImmutableList<PathSegment> path = parse(true, false);
    if (!pathStartedWithSlash) {
      addError("effective path must start with leading '/'.");
    }
    if (current != null) {
      addError("unrecognized input at '%s'.", current);
    }
    if (configVersion > 0 && CUSTOM_VERB_PATTERN_ILLEGAL.matcher(template).find()) {
      addError("invalid token '/:' before the custom verb.");
    }
    if (hadErrors) {
      return null;
    }
    if (customVerb != null) {
      return FluentIterable.from(path).append(new LiteralSegment(customVerb, true)).toList();
    }
    return path;
  }

  private ImmutableList<PathSegment> parse(boolean firstSegment, boolean subPath) {
    if (lookingAt("/")) {
      getCurrentAndShift();
      if (firstSegment) {
        firstSegment = false;
        pathStartedWithSlash = true;
      } else {
        addError("leading '/' only allowed for first segment of path.");
      }
    }
    ImmutableList.Builder<PathSegment> segments = ImmutableList.builder();
    while (true) {
      String token = getCurrentAndShift();
      if (token == null) {
        break;
      }
      switch (token) {
        case "*":
          if (lookingAt("*")) {
            getCurrentAndShift();
            segments.add(new WildcardSegment(true));
          } else {
            segments.add(new WildcardSegment(false));
          }
          break;
        case "{":
          segments.add(parseField(firstSegment, subPath));
          break;
        default:
          segments.add(new LiteralSegment(token));
          break;
      }
      // No longer processing the first segment.
      firstSegment = false;

      if (!lookingAt("/")) {
        break;
      } else {
        getCurrentAndShift();
      }
    }
    return segments.build();
  }

  private FieldSegment parseField(boolean firstSegment, boolean subPath) {
    String fieldPath = getCurrentAndShift();
    if (lookingAt("=")) {
      getCurrentAndShift();
      if (subPath) {
        addError("cannot have fields in nested paths.");
      }
      ImmutableList<PathSegment> segments = parse(firstSegment, true);
      expectAndShift("}");
      return new FieldSegment(fieldPath, segments);
    } else {
      expectAndShift("}");
      return new FieldSegment(fieldPath, ImmutableList.<PathSegment>of());
    }
  }

  private boolean lookingAt(String token) {
    return token.equals(current);
  }

  private String getCurrentAndShift() {
    if (current == null) {
      addError("unexpected end of input.");
    }
    String last = current;
    current = tokens.find() ? tokens.group().trim() : null;
    return last;
  }

  private void expectAndShift(String token) {
    if (!lookingAt(token)) {
      addError("expected '%s', looking at %s.", token,
          current == null ? "end of input" : "'" + current + "'");
    } else {
      getCurrentAndShift();
    }
  }

  private void addError(String message, Object... params) {
    diagCollector.addDiag(Diag.error(location, "In path template '" + template + "': " + message,
        params));
    hadErrors = true;
  }
}

package io.gapi.fx.aspects.http.model;

import com.google.api.HttpRule;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.inject.Key;

import io.gapi.fx.model.Field;
import io.gapi.fx.model.FieldSelector;
import io.gapi.fx.model.MessageType;

import java.util.List;

/**
 * Describes the mapping of of a method to HTTP. Attached by the aspects to methods
 * which do have an http configuration.
 */
public class HttpAttribute {

  /**
   * Key used to access this attribute.
   */
  public static final Key<HttpAttribute> KEY = Key.get(HttpAttribute.class);

  /**
   * The http method kind.
   */
  public enum MethodKind {
    GET(true),
    PUT(true),
    POST(false),
    DELETE(true),
    PATCH(false),
    NONE(false);

    private final boolean isIdempotent;

    private MethodKind(boolean isIdempotent) {
      this.isIdempotent = isIdempotent;
    }

    public boolean isIdempotent() {
      return isIdempotent;
    }
  }


  /**
   * Base class for path segments.
   */
  public abstract static class PathSegment {

    /**
     * Returns the syntax of this segment.
     */
    public abstract String syntax();

    @Override
    public String toString() {
      return syntax();
    }

    /**
     * Returns the syntax for a full path given as a list of segments.
     */
    public static String toSyntax(Iterable<PathSegment> parts) {
      return toSyntax(parts, true);
    }

    static String toSyntax(Iterable<PathSegment> parts, boolean leadingSeparator) {
      StringBuilder result = new StringBuilder();
      for (PathSegment part : parts) {
        if (leadingSeparator || result.length() > 0) {
          result.append(part.separator());
        }
        result.append(part.syntax());
      }
      return result.toString();
    }

    /**
     * Returns the separator to be used before this path segment. Defaults to '/'.
     */
    String separator() {
      return "/";
    }
  }

  /**
   * A path segment representing a wildcard, which is bounded (matches 1 segment) or unbounded
   * (matches 1 or more segments).
   */
  public static class WildcardSegment extends PathSegment {

    private final boolean unbounded;

    public WildcardSegment(boolean unbounded) {
      this.unbounded = unbounded;
    }

    public boolean isUnbounded() {
      return unbounded;
    }

    @Override
    public String syntax() {
      return unbounded ? "**" : "*";
    }
  }

  /**
   * A path segment representing a literal.
   */
  public static class LiteralSegment extends PathSegment {

    private final String literal;
    private final boolean isTrailingCustomVerb;

    public LiteralSegment(String literal) {
      this.literal = literal;
      this.isTrailingCustomVerb = false;
    }

    public LiteralSegment(String literal, boolean isTrailingCustomVerb) {
      this.literal = literal;
      this.isTrailingCustomVerb = isTrailingCustomVerb;
    }

    public String getLiteral() {
      return literal;
    }

    public boolean isTrailingCustomVerb() {
      return isTrailingCustomVerb;
    }

    @Override
    public String syntax() {
      return literal;
    }

    @Override
    String separator() {
      return isTrailingCustomVerb ? ":" : "/";
    }
  }

  /**
   * A path segment representing a field reference.
   */
  public static class FieldSegment extends PathSegment {

    private final String fieldPath;
    private final ImmutableList<PathSegment> subPath;

    private FieldSelector selector;


    public FieldSegment(String fieldPath, ImmutableList<PathSegment> subPath) {
      this.fieldPath = Preconditions.checkNotNull(fieldPath);
      this.subPath = Preconditions.checkNotNull(subPath);
    }

    public String getFieldPath() {
      return fieldPath;
    }

    public ImmutableList<PathSegment> getSubPath() {
      return subPath;
    }

    /**
     * Gets the field this segment links to.
     */
    public FieldSelector getFieldSelector() {
      return selector;
    }

    /**
     * Sets the field this segment links to.
     */
    public void setFieldSelector(FieldSelector selector) {
      this.selector = selector;
    }

    @Override
    public String syntax() {
      StringBuilder result = new StringBuilder();
      result.append('{');
      result.append(fieldPath);
      if (subPath != null) {
        result.append('=');
        result.append(PathSegment.toSyntax(subPath, false));
      }
      result.append('}');
      return result.toString();
    }
  }

  private final HttpRule rule;
  private final MethodKind methodKind;
  private final MessageType message;
  private final ImmutableList<PathSegment> path;
  private final String body;
  private final boolean isFromIdl;
  private final ImmutableList<HttpAttribute> additionalBindings;
  private final boolean isPrimary;
  private RestMethod restMethod;

  private ImmutableList<PathSegment> flattenedPath;

  /**
   * Constructs an http binding for the given method kind, message, path, and body. See
   * documentation of the getters for the meaning of those values.
   */
  public HttpAttribute(HttpRule rule, MethodKind methodKind, MessageType message,
      ImmutableList<PathSegment> path, String body, boolean isFromIdl,
      ImmutableList<HttpAttribute> additionalBindings, boolean isPrimary) {

    this.rule = Preconditions.checkNotNull(rule);
    this.methodKind = Preconditions.checkNotNull(methodKind);
    this.message = Preconditions.checkNotNull(message);
    this.path = Preconditions.checkNotNull(path);
    this.body = body;
    this.isFromIdl = isFromIdl;
    this.additionalBindings = Preconditions.checkNotNull(additionalBindings);
    this.isPrimary = isPrimary;
  }

  /**
   * Creates an empty binding for a method which is not exposed via http.
   */
  public static HttpAttribute noConfig(MessageType message) {
    HttpAttribute config =
        new HttpAttribute(HttpRule.getDefaultInstance(), MethodKind.NONE, message,
            ImmutableList.<PathSegment>of(), null, false, ImmutableList.<HttpAttribute>of(), true);
    config.setFields(ImmutableList.<FieldSelector>of(), ImmutableList.<FieldSelector>of());
    return config;
  }

  /**
   * Returns true if this is the primary binding.
   */
  public boolean isPrimary() {
    return isPrimary;
  }

  /**
   * Returns the associated rest method. Each HTTP binding has one.
   */
  public RestMethod getRestMethod() {
    return restMethod;
  }

  /**
   * Sets the rest method.
   */
  public void setRestMethod(RestMethod method) {
    this.restMethod = method;
  }

  /**
   * Create a new HTTP binding where the HTTP paths are rooted underneath the provided path.
   * replaces the first segment in the path by the list of literal segments obtained from the
   * newRoot parameter.
   */
  public HttpAttribute reroot(final String newRoot) {
    // Compute new path.
    ImmutableList.Builder<PathSegment> changedPath = ImmutableList.builder();
    for (String comp : Splitter.on('/').omitEmptyStrings().trimResults().split(newRoot)) {
      changedPath.add(new LiteralSegment(comp));
    }
    changedPath.addAll(path.subList(1, path.size()));

    // Change rule.
    HttpRule.Builder changedRule = rule.toBuilder();
    String changedPathPattern = PathSegment.toSyntax(changedPath.build());
    switch (rule.getPatternCase()) {
      case GET:
        changedRule.setGet(changedPathPattern);
        break;
      case PUT:
        changedRule.setPut(changedPathPattern);
        break;
      case POST:
        changedRule.setPost(changedPathPattern);
        break;
      case PATCH:
        changedRule.setPatch(changedPathPattern);
        break;
      case DELETE:
        changedRule.setDelete(changedPathPattern);
        break;
      default:
        break;
    }

    // Change additional bindings.
    ImmutableList<HttpAttribute> changedAdditionalBindings =
        FluentIterable.from(additionalBindings).transform(
            new Function<HttpAttribute, HttpAttribute>() {
              @Override public HttpAttribute apply(HttpAttribute attrib) {
                return attrib.reroot(newRoot);
              }
        }).toList();

    // Return new binding.
    HttpAttribute attrib = new HttpAttribute(changedRule.build(), methodKind, message,
        changedPath.build(), body, isFromIdl, changedAdditionalBindings, isPrimary);
    attrib.pathSelectors = pathSelectors;
    attrib.bodySelectors = bodySelectors;
    attrib.paramSelectors = paramSelectors;
    return attrib;
  }

  /**
   * Returns an {@link Iterable} that includes the primary binding (the current one) and all
   * additional bindings. The primary binding is always the first item in the {@link Iterable}.
   */
  public Iterable<HttpAttribute> getAllBindings() {
    return FluentIterable.of(new HttpAttribute[]{ this }).append(additionalBindings);
  }

  //-------------------------------------------------------------------------
  // Syntax

  /**
   * Gets the underlying http rule.
   */
  public HttpRule getHttpRule() {
    return rule;
  }

  /**
   * Gets the http method kind.
   */
  public MethodKind getMethodKind() {
    return methodKind;
  }

  /**
   * Gets the request message this configuration is associated with.
   */
  public MessageType getMessage() {
    return message;
  }

  /**
   * Gets the path as a list of segments.
   */
  public ImmutableList<PathSegment> getPath() {
    return path;
  }

  /**
   * Gets the flattened path, where all FieldSegments have been replaced by their sub-paths.
   */
  public ImmutableList<PathSegment> getFlatPath() {
    if (flattenedPath != null) {
      return flattenedPath;
    }
    ImmutableList.Builder<PathSegment> builder = ImmutableList.builder();
    flatten(builder, path);
    return flattenedPath = builder.build();
  }

  private void flatten(Builder<PathSegment> builder, ImmutableList<PathSegment> path) {
    for (PathSegment segm : path) {
      if (segm instanceof FieldSegment) {
        FieldSegment fieldSegm = (FieldSegment) segm;
        if (fieldSegm.subPath.isEmpty()) {
          // looking at {name}, will be replaced by '*'.
          builder.add(new WildcardSegment(false));
        } else {
          flatten(builder, ((FieldSegment) segm).getSubPath());
        }
      } else {
        builder.add(segm);
      }
    }
  }

  /**
   * Gets the body or null if none specified.
   */
  public String getBody() {
    return body;
  }

  /**
   * Return true if the http binding information is from IDL.
   */
  public boolean isFromIdl() {
    return isFromIdl;
  }

  /**
   * Returns true if the body is configured to include unbound fields.
   */
  public boolean bodyCapturesUnboundFields() {
    return "*".equals(body);
  }

  //-------------------------------------------------------------------------
  // Attributes belonging to merged stage

  private ImmutableList<FieldSelector> pathSelectors;
  private ImmutableList<FieldSelector> paramSelectors;
  private ImmutableList<FieldSelector> bodySelectors;

  /**
   * Gets the fields which are bound via the path.
   */
  public ImmutableList<FieldSelector> getPathSelectors() {
    return pathSelectors;
  }

  /**
   * Gets fields which are bound via parameters.
   */
  public ImmutableList<FieldSelector> getParamSelectors() {
    return paramSelectors;
  }

  /**
   * Get fields which are bound via the body.
   */
  public ImmutableList<FieldSelector> getBodySelectors() {
    return bodySelectors;
  }

  /**
   * Sets the parameter and body fields. Also derives the path fields from the path,
   * assuming they have been resolved.
   */
  public void setFields(ImmutableList<FieldSelector> paramFields,
      ImmutableList<FieldSelector> bodyFields) {
    this.paramSelectors = paramFields;
    this.bodySelectors = bodyFields;
    this.pathSelectors = FluentIterable.from(path).filter(FieldSegment.class)
        .filter(new Predicate<FieldSegment>() {
          @Override public boolean apply(FieldSegment seg) {
            return seg.getFieldSelector() != null;
          }
        })
        .transform(new Function<FieldSegment, FieldSelector>() {
          @Override public FieldSelector apply(FieldSegment seg) {
            return seg.getFieldSelector();
          }
        }).toList();
  }

  //-------------------------------------------------------------------------
  // Attributes belonging to scoped stage

  private ImmutableList<FieldSelector> visiblePathSelectors;
  private ImmutableList<FieldSelector> visibleParamSelectors;
  private ImmutableList<FieldSelector> visibleBodySelectors;

  /**
   * Gets visible fields which are bound via the path.
   */
  public ImmutableList<FieldSelector> getVisiblePathSelectors() {
    if (visiblePathSelectors == null) {
      visiblePathSelectors = buildVisibleSelectors(pathSelectors);
    }
    return visiblePathSelectors;
  }

  /**
   * Gets visible fields which are bound via parameters.
   */
  public ImmutableList<FieldSelector> getVisibleParamSelectors() {
    if (visibleParamSelectors == null) {
      visibleParamSelectors = buildVisibleSelectors(paramSelectors);
    }
    return visibleParamSelectors;
  }

  /**
   * Gets visible fields which are bound via body.
   */
  public ImmutableList<FieldSelector> getVisibleBodySelectors() {
    if (visibleBodySelectors == null) {
      visibleBodySelectors = buildVisibleSelectors(bodySelectors);
    }
    return visibleBodySelectors;
  }

  private ImmutableList<FieldSelector> buildVisibleSelectors(
      List<FieldSelector> selectors) {
    ImmutableList.Builder<FieldSelector> listBuilder = ImmutableList.builder();
    for (FieldSelector selector : selectors) {
      boolean hasInvisibleField = false;
      for (Field field : selector.getFields()) {
        if (!field.isReachable()) {
          hasInvisibleField = true;
          break;
        }
      }
      // Only include FieldSelector that has no invisible field.
      if (!hasInvisibleField) {
        listBuilder.add(selector);
      }
    }
    return listBuilder.build();
  }

  public ImmutableList<HttpAttribute> getAdditionalBindings() {
    return additionalBindings;
  }
}

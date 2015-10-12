package io.gapi.fx.aspects.http;

import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.gapi.fx.aspects.documentation.model.ResourceAttribute;
import io.gapi.fx.aspects.http.model.CollectionAttribute;
import io.gapi.fx.aspects.http.model.HttpAttribute;
import io.gapi.fx.aspects.http.model.HttpAttribute.LiteralSegment;
import io.gapi.fx.aspects.http.model.HttpAttribute.MethodKind;
import io.gapi.fx.aspects.http.model.HttpAttribute.PathSegment;
import io.gapi.fx.aspects.http.model.HttpAttribute.WildcardSegment;
import io.gapi.fx.aspects.http.model.RestKind;
import io.gapi.fx.aspects.http.model.RestMethod;
import io.gapi.fx.model.MessageType;
import io.gapi.fx.model.Method;
import io.gapi.fx.model.TypeRef;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Rest analyzer. Determines {@link RestMethod} associated with method and http binding. Also
 * computes collections and estimates their resource types.
 *
 * <p>The analyzer reports warnings regards rest conformance, but never produces an error and always
 * well-defined output for each method.
 */
class RestAnalyzer {

  private static final String REST_STYLE_RULE_NAME = "rest";
  private static final String METHOD_SHADOWED_RULE_NAME = "rest-shadowed";

  // Represents a rest method pattern.
  @AutoValue
  abstract static class MethodPattern {

    // The http method.
    abstract MethodKind httpMethod();

    // A regular expression which the rpc name must match.
    abstract Pattern nameRegexp();

    // A pattern which the last segment of the path must match.
    @Nullable abstract SegmentPattern lastSegmentPattern();

    // The implied rest kind.
    @Nullable abstract RestKind restKind();

    // The implied prefix to use for custom methods.
    abstract String customPrefix();

    // Documentation of the pattern.
    abstract String description();

    private static MethodPattern create(MethodKind methodKind, String nameRegexp,
        SegmentPattern lastSegment, RestKind restKind, String customPrefix, String description) {
      return new AutoValue_RestAnalyzer_MethodPattern(methodKind, Pattern.compile(nameRegexp),
          lastSegment, restKind, customPrefix, description);
    }

    private static MethodPattern create(MethodKind methodKind, String nameRegexp,
          SegmentPattern lastSegment, RestKind restKind, String description) {
      return create(methodKind, nameRegexp, lastSegment, restKind, "", description);
    }

    // Produce readable output for this pattern. This is used in error messages, as in
    // 'rpc Get.* as HTTP GET <prefix>/<wildcard>'. We don't print the rest kind.
    @Override
    public String toString() {
      StringBuilder result = new StringBuilder();
      result.append("rpc ");
      result.append(nameRegexp().pattern());
      result.append(" as HTTP ");
      result.append(httpMethod().toString());
      if (lastSegmentPattern() != null) {
        result.append(" ");
        result.append(pathDisplay());
      }
      return result.toString();
    }

    private String pathDisplay() {
      if (lastSegmentPattern() != null) {
        switch (lastSegmentPattern()) {
          case VARIABLE:
            return "<prefix>/<wildcard>";
          case CUSTOM_VERB:
            return "<prefix>:<literal>";
          case LITERAL:
            return "<prefix>/<literal>";
        }
      }
      return "";
    }
  }

  // A pattern for a path segment.
  enum SegmentPattern {
    VARIABLE,
    LITERAL,
    CUSTOM_VERB,
  }

  // Declares all the currently allowed rest method patterns. If none of those
  // matches, a warning will be produced.
  private static final ImmutableList<MethodPattern> METHOD_PATTERNS =
      ImmutableList.<MethodPattern>builder()

        // First list all standard methods. They have priority in matching.
        // Note that the only source of ambiguity here is a legacy custom
        // method which uses <prefix>/<literal> instead of <prefix>:<literal>, otherwise
        // our patterns would be unique.
        .add(MethodPattern.create(
            MethodKind.GET,
            "Get.*",
            SegmentPattern.VARIABLE,
            RestKind.GET,
            "Gets a resource."
        ))
        .add(MethodPattern.create(
            MethodKind.GET,
            "List.*",
            SegmentPattern.LITERAL,
            RestKind.LIST,
            "Lists all resources"
        ))
        .add(MethodPattern.create(
            MethodKind.PUT,
            "Update.*",
            SegmentPattern.VARIABLE,
            RestKind.UPDATE,
            "Update a resource."
        ))
        .add(MethodPattern.create(
            MethodKind.PUT,
            "(Create|Insert).*",
            SegmentPattern.VARIABLE,
            RestKind.CREATE,
            "Create a resource."
        ))
        .add(MethodPattern.create(
            MethodKind.PATCH,
            "(Update|Patch).*",
            SegmentPattern.VARIABLE,
            RestKind.PATCH,
            "Patch a resource."
        ))
        .add(MethodPattern.create(
            MethodKind.DELETE,
            "Delete.*",
            SegmentPattern.VARIABLE,
            RestKind.DELETE,
            "Delete a resource"
        ))
        .add(MethodPattern.create(
            MethodKind.POST,
            "(Create|Insert).*",
            SegmentPattern.LITERAL,
            RestKind.CREATE,
            "Create a resource"
        ))

        // Next list all custom methods.
        .add(MethodPattern.create(
            MethodKind.GET,
            "Get.*",
            SegmentPattern.CUSTOM_VERB,
            RestKind.CUSTOM,
            "get",
            "Custom get resource."
        ))
        .add(MethodPattern.create(
            MethodKind.GET,
            "List.*",
            SegmentPattern.CUSTOM_VERB,
            RestKind.CUSTOM,
            "list",
            "Custom list resources."
        ))
        .add(MethodPattern.create(
            MethodKind.PUT,
            "Update.*",
            SegmentPattern.CUSTOM_VERB,
            RestKind.CUSTOM,
            "update",
            "Custom update resource."
        ))
        .add(MethodPattern.create(
            MethodKind.PUT,
            "Create.*",
            SegmentPattern.CUSTOM_VERB,
            RestKind.CUSTOM,
            "create",
            "Custom create resource."
        ))
        .add(MethodPattern.create(
            MethodKind.PATCH,
            "Patch.*",
            SegmentPattern.CUSTOM_VERB,
            RestKind.CUSTOM,
            "patch",
            "Custom patch resource."
        ))
        .add(MethodPattern.create(
            MethodKind.PATCH,
            "Update.*",
            SegmentPattern.CUSTOM_VERB,
            RestKind.CUSTOM,
            "update",
            "Custom update resource"
        ))
        .add(MethodPattern.create(
            MethodKind.DELETE,
            "Delete.*",
            SegmentPattern.CUSTOM_VERB,
            RestKind.CUSTOM,
            "delete",
            "Custom delete resource"
        ))
        .add(MethodPattern.create(
            MethodKind.POST,
            ".*",
            SegmentPattern.CUSTOM_VERB,
            RestKind.CUSTOM,
            "General custom method."
        ))
        .build();

  private final HttpConfigAspect aspect;
  private final Map<String, CollectionAttribute> collectionMap =
      new TreeMap<String, CollectionAttribute>();

  /**
   * Registers lint rule names used by the analyzer.
   */
  static void registerLintRuleNames(HttpConfigAspect aspect) {
    aspect.registerLintRuleName(REST_STYLE_RULE_NAME, METHOD_SHADOWED_RULE_NAME);
  }

  /**
   * Creates a rest analyzer which reports errors via the given aspect.
   */
  RestAnalyzer(HttpConfigAspect aspect) {
    this.aspect = aspect;
  }

  /**
   * Finalizes rest analysis, delivering the collections used.
   */
  List<CollectionAttribute> finalizeAndGetCollections() {
    // Compute the resource types for each collection. We need to have all collections fully
    // built before this can be done.
    //
    // In the first pass, we walk over all messages and collect information from the
    // resource attribute as derived from a doc instruction. In the second pass, for those
    // collections which still have no resource, we run a heuristic to identify the resource.
    Map<String, TypeRef> definedResources = Maps.newLinkedHashMap();

    for (TypeRef type : aspect.getModel().getSymbolTable().getDeclaredTypes()) {
      if (!type.isMessage()) {
        continue;
      }
      MessageType message = type.getMessageType();
      List<ResourceAttribute> definitions = message.getAttribute(ResourceAttribute.KEY);
      if (definitions != null) {
        for (ResourceAttribute definition : definitions) {
          TypeRef old = definedResources.put(definition.collection(), type);
          if (old != null) {
            aspect.warning(message.getLocation(),
                "Resource association of '%s' for collection '%s' overridden by '%s'. "
                + "Currently there can be only one resource associated with a collection.",
                old, definition.collection(), type);
          }
        }
      }
    }

    ImmutableList.Builder<CollectionAttribute> result = ImmutableList.builder();
    for (CollectionAttribute collection : collectionMap.values()) {
      TypeRef type = definedResources.get(collection.getFullName());
      if (type == null) {
        // No defined resource association, run heuristics.
        type = new ResourceTypeSelector(aspect.getModel(),
            collection.getMethods()).getCandiateResourceType();
      }
      collection.setResourceType(type);
      result.add(collection);
    }
    return result.build();
  }

  /**
   * Analyzes the given method and http config and returns a rest method.
   */
  RestMethod analyzeMethod(Method method, HttpAttribute httpConfig) {
    // First check whether this is a special method.
    RestMethod restMethod = createSpecialMethod(method, httpConfig);

    if (restMethod == null) {
      // Search for the first matching method pattern.
      MethodMatcher matcher = null;
      for (MethodPattern pattern : METHOD_PATTERNS) {
        matcher = new MethodMatcher(pattern, method, httpConfig);
        if (matcher.matches) {
          break;
        }
        matcher = null;
      }
      if (matcher != null) {
        restMethod = matcher.createRestMethod();
      } else {
        // No pattern matches. Diagnose and create custom method. Even though the
        // custom method is non-conforming, it is a valid configuration.
        diagnose(method, httpConfig);
        restMethod = createCustomMethod(method, httpConfig, "");
      }
    }

    // Add method to collection.
    String collectionName = restMethod.getRestCollectionName();
    CollectionAttribute collection = collectionMap.get(collectionName);
    if (collection == null) {
      collection = new CollectionAttribute(aspect.getModel(), collectionName);
      collectionMap.put(collectionName, collection);
    }
    RestMethod oldMethod = collection.addMethod(restMethod);
    if (oldMethod != null) {
      aspect.lintWarning(METHOD_SHADOWED_RULE_NAME, restMethod.getBaseMethod(),
          "REST method '%s' from rpc method '%s' at '%s' on collection '%s' is shadowed by REST "
          + "method of same name from this rpc. The original method will not be available in "
          + "REST discovery and derived artifacts.",
          oldMethod.getRestMethodName(),
          oldMethod.getBaseMethod().getFullName(),
          oldMethod.getBaseMethod().getLocation().getDisplayString(),
          oldMethod.getRestCollectionName());
    }
    return restMethod;
  }

  // Determines whether to create a special rest method. This currently applies to
  // media methods. Returns null if no special rest method.
  private RestMethod createSpecialMethod(Method method, HttpAttribute httpConfig) {
    if (httpConfig.getMethodKind() == MethodKind.NONE) {
      // Not an HTTP method. Create a dummy rest method.
      return RestMethod.create(method, RestKind.CUSTOM, "", method.getFullName());
    }
    if (httpConfig.getHttpRule().getMediaUpload().getEnabled()) {
      return RestMethod.create(method, RestKind.CUSTOM, "media", "upload");
    }
    if (httpConfig.getHttpRule().getMediaDownload().getEnabled()) {
      return RestMethod.create(method, RestKind.CUSTOM, "media", "download");
    }
    return null;
  }

  // Create a custom rest method. If the last path segment is a literal, it will be used
  // as the verb for the custom method, otherwise the custom prefix or the rpc's name.
  private RestMethod createCustomMethod(Method method, HttpAttribute httpConfig,
      String customNamePrefix) {
    ImmutableList<PathSegment> path = httpConfig.getFlatPath();
    PathSegment lastSegment = path.get(path.size() - 1);

    // Determine base name.
    String customName = "";
    if (lastSegment instanceof LiteralSegment) {
      customName = ((LiteralSegment) lastSegment).getLiteral();
      path = path.subList(0, path.size() - 1);
    } else {
      if (aspect.getModel().getConfigVersion() > 1) {
        // From version 2 on, we generate a meaningful name here.
        customName = method.getSimpleName();
      } else if (customNamePrefix.isEmpty()){
        // Older versions use the prefix or derive from the http method.
        customName = httpConfig.getMethodKind().toString().toLowerCase();
      }
    }

    // Prepend prefix.
    if (!customNamePrefix.isEmpty()
        && !customName.toLowerCase().startsWith(customNamePrefix.toLowerCase())) {
      customName = customNamePrefix + ensureUpperCase(customName);
    }

    // Ensure effective start is lower case.
    customName = ensureLowerCase(customName);

    return RestMethod.create(method, RestKind.CUSTOM, buildCollectionName(path), customName);
  }

  private static String ensureUpperCase(String name) {
    if (!name.isEmpty() && Character.isLowerCase(name.charAt(0))) {
      return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
    return name;
  }

  private static String ensureLowerCase(String name) {
    if (!name.isEmpty() && Character.isUpperCase(name.charAt(0))) {
      return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
    return name;
  }

  // Create diagnosis after a unsuccessful match. We attempt to construct a list of candidates
  // which could have matched and show them to the user.
  private void diagnose(Method method, HttpAttribute httpConfig) {
    List<MethodPattern> cands = Lists.newArrayList();
    for (MethodPattern pattern : METHOD_PATTERNS) {
      if (pattern.nameRegexp().matcher(method.getSimpleName()).matches()) {
        // The name matches, but other attributes not. Add a cand with the given name and
        // required attributes.
        cands.add(MethodPattern.create(pattern.httpMethod(), method.getSimpleName(),
            pattern.lastSegmentPattern(), pattern.restKind(), ""));
      }
      // Attempt to match the pattern with no name restriction.
      MethodPattern noNameRestriction = MethodPattern.create(pattern.httpMethod(), ".*",
          pattern.lastSegmentPattern(), pattern.restKind(), "");
      if (new MethodMatcher(noNameRestriction, method, httpConfig).matches) {
        cands.add(pattern);
      }
    }
    if (cands.isEmpty()) {
      cands = METHOD_PATTERNS;
    }
    aspect.lintWarning(REST_STYLE_RULE_NAME, method,
        "'%s %s' is not a recognized REST pattern. Did you mean one of:\n  %s",
        MethodPattern.create(httpConfig.getMethodKind(), method.getSimpleName(), null, null, ""),
        PathSegment.toSyntax(httpConfig.getFlatPath()),
        Joiner.on("\n  ").join(cands));
  }

  // Builds the collection name from a path.
  private String buildCollectionName(Iterable<PathSegment> segments) {
    return Joiner.on('.').skipNulls().join(FluentIterable.from(segments).transform(
        new Function<PathSegment, String>() {
          @Override
          public String apply(PathSegment segm) {
            if (!(segm instanceof LiteralSegment)) {
              return null;
            }
            LiteralSegment literal = (LiteralSegment) segm;
            if (literal.isTrailingCustomVerb()) {
              return null;
            }
            return literal.getLiteral();
          }
        }));
  }

  /**
   * Helper class to match a method against a method pattern.
   */
  private class MethodMatcher {
    private final MethodPattern pattern;
    private final Method method;
    private final HttpAttribute httpConfig;
    private Matcher nameMatcher;
    private boolean matches;

    MethodMatcher(MethodPattern pattern, Method method, HttpAttribute httpConfig) {
      this.pattern = pattern;
      this.method = method;
      this.httpConfig = httpConfig;

      matches = false;

      // Check http method.
      if (httpConfig.getMethodKind() != pattern.httpMethod()) {
        return;
      }

      // Check name regexp.
      nameMatcher = pattern.nameRegexp().matcher(method.getSimpleName());
      if (!nameMatcher.matches()) {
        return;
      }

      // Determine match on last segment.
      List<PathSegment> flatPath = httpConfig.getFlatPath();
      PathSegment lastSegment = flatPath.get(flatPath.size() - 1);
      switch (pattern.lastSegmentPattern()) {
        case CUSTOM_VERB:
          // We allow both a custom verb literal and a regular literal, the latter is for supporting
          // legacy custom verbs.
          matches = lastSegment instanceof LiteralSegment;
          break;
        case VARIABLE:
          matches = lastSegment instanceof WildcardSegment;
          break;
        case LITERAL:
          matches = lastSegment instanceof LiteralSegment
              && !((LiteralSegment) lastSegment).isTrailingCustomVerb();
          break;
      }
    }

    // Creates a RestMethod from this matcher.
    private RestMethod createRestMethod() {
      if (pattern.lastSegmentPattern() == SegmentPattern.CUSTOM_VERB) {
        return createCustomMethod(method, httpConfig, pattern.customPrefix());
      }
      return RestMethod.create(method, pattern.restKind(),
          buildCollectionName(httpConfig.getFlatPath()), null);
    }
  }


  /**
   * Main entry point for generating a table of supported REST patterns.
   */
  public static void main(String[] args) {
    PrintStream out = System.out;
    out.println("HTTP method | RPC name regexp | Path | REST verb | REST name | Remarks");
    out.println("------------|-----------------|------|-----------|-----------|--------");
    for (MethodPattern pattern : METHOD_PATTERNS) {
      out.print("`" + pattern.httpMethod() + "`");
      out.print(" | ");
      out.print("`" + pattern.nameRegexp().pattern() + "`");
      out.print(" | ");
      out.print("`" + pattern.pathDisplay() + "`");
      out.print(" | ");
      out.print("`" + pattern.restKind() + "`");
      out.print(" | ");
      out.print("`" + restNameDisplay(pattern) + "`");
      out.print(" | ");
      out.print(pattern.description());
      out.println();
    }
  }

  private static String restNameDisplay(MethodPattern pattern) {
    if (pattern.restKind() == RestKind.CUSTOM) {
      return pattern.customPrefix().isEmpty() ? "<literal>" : pattern.customPrefix() + "<Literal>";
    }
    return pattern.restKind().toString().toLowerCase();
  }
}

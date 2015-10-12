package io.gapi.fx.aspects;

import com.google.api.Service;
import com.google.api.Service.Builder;
import io.gapi.fx.model.ConfigAspect;
import io.gapi.fx.model.Diag;
import io.gapi.fx.model.DiagCollector;
import io.gapi.fx.model.Element;
import io.gapi.fx.model.Location;
import io.gapi.fx.model.Model;
import io.gapi.fx.model.ProtoElement;
import io.gapi.fx.model.SimpleLocation;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

/**
 * Base class for implementing config aspects.
 *
 * <p>All aspect implementations must extend this class or it's descendants instead of directly
 * implementing the interface {@link ConfigAspect}. Errors and warnings must be reported using the
 * methods on this class instead of via the model.
 *
 * <p>Please observe that according to rules described in {@link ConfigAspect}, during the merging
 * state, only essential validation issues should be reported as errors, but not style violations.
 * It is recommended to use the {@link LintRule} mechanism for style violations.
 */
public abstract class ConfigAspectBase implements ConfigAspect {

  // The model this aspect is attached to.
  private final Model model;

  // The display name of the aspect. Used e.g. in error messages.
  private final String aspectName;

  // The validation rules registered for this aspect.
  private final Multimap<Class<? extends Element>, LintRule<? extends Element>> lintRules =
    LinkedHashMultimap.create();

  // The set of lint rule names used by this aspect.
  private final Set<String> lintRuleNames = Sets.newLinkedHashSet();

  /**
   * Initializes this base class. Takes a string which is used to identify the aspect in diagnosis.
   */
  protected ConfigAspectBase(Model model, String aspectName) {
    this.model = Preconditions.checkNotNull(model);
    this.aspectName = Preconditions.checkNotNull(aspectName);
  }

  /**
   * Returns the model to which the aspect is linked.
   */
  public Model getModel() {
    return model;
  }

  @Override
  public String getAspectName() {
    return aspectName;
  }

  @Override
  public Set<String> getLintRuleNames() {
    return lintRuleNames;
  }

  /**
   * Registers a linting rule with this aspect. The type of the rule can be either a proto element
   * or, for top-level scoped issues, the model itself. All registered rules are run automatically
   * by this class.
   */
  public <E extends Element> void registerLintRule(LintRule<E> rule) {
    lintRules.put(rule.getElementClass(), rule);
    registerLintRuleName(rule.getName());
  }

  /**
   * Registers name of a lint rule. Should be used for all rule names used in
   * {@link #lintWarning(String, Object, String, Object...)} to enable suppression. Does not need to
   * be used for rules using the {@link #registerLintRule(LintRule)} mechanism.
   */
  public void registerLintRuleName(String... names) {
    lintRuleNames.addAll(Lists.newArrayList(names));
  }

  /**
   * Default implementation of merge dependencies; returns the empty list.
   */
  @Override
  public List<Class<? extends ConfigAspect>> mergeDependencies() {
    return ImmutableList.of();
  }
  /**
   * Default implementation of starting merge; does nothing.
   */
  @Override
  public void startMerging() {}

  /**
   * Default implementation for merging an element; does nothing.
   */
  @Override
  public void merge(ProtoElement elem) {}

  /**
   * Default implementation ending merging; does nothing.
   */
  @Override
  public void endMerging() {}


  /**
   * Default implementation of start linting; does nothing.
   */
  @Override
  public void startLinting() {}

  /**
   * Default implementation of linting an element. This runs all registered style rules for the
   * element's type.
   *
   * <p>If you override this method, be sure to call {@code super}.
   */
  @Override
  public void lint(ProtoElement elem) {
    runRules(elem);
  }

  /**
   * Default implementation ending linting. This runs all style rules registered for the model
   * (top-level scope).
   *
   * <p>If you override this method, be sure to call {@code super}.
   */
  @Override
  public void endLinting() {
    runRules(model);
  }

  /**
   * Runs all rules for the given element.
   */
  @SuppressWarnings("unchecked")
  private void runRules(Element elem) {
    Class<?> type = elem.getClass();
    while (Element.class.isAssignableFrom(type)) {
      for (LintRule<? extends Element> rule : lintRules.get((Class<? extends Element>) type)) {
        @SuppressWarnings("unchecked")
        LintRule<Element> castedRule = (LintRule<Element>) rule;
        castedRule.run(elem);
      }
      type = type.getSuperclass();
    }
  }

  /**
   * Default implementation of starting normalization; does nothing.
   */
  @Override
  public void startNormalization(Service.Builder builder) {}


  /**
   * Default implementation to normalize an element; does nothing.
   */
  @Override
  public void normalize(ProtoElement element, Builder builder) { }

  /**
   * Default implementation of ending normalization; does nothing.
   */
  @Override
  public void endNormalization(Service.Builder builder) {}

  /**
   * Default implementation of aspect documentation title. Returns null, indicating
   * the aspect is not documented.
   */
  @Override
  public String getDocumentationTitle(ProtoElement element) {
    return null;
  }

  /**
   * Default implementation of aspect documentation. Returns null, indicating
   * the aspect is not documented.
   */
  @Override
  public String getDocumentation(ProtoElement element) {
    return null;
  }

  /**
   * Helper for subclasses to report an error.
   */
  public void error(Object elementOrLocation, String message, Object... params) {
    model.addDiag(Diag.error(getLocation(elementOrLocation),
        Model.diagPrefix(aspectName) + message, params));
  }

  /**
   * Helper for subclasses to report a warning.
   */
  public void warning(Object elementOrLocation, String message, Object... params) {
    model.addDiagIfNotSuppressed(elementOrLocation, Diag.warning(getLocation(elementOrLocation),
        Model.diagPrefix(aspectName) + message, params));
  }

  /**
   * Helper for subclasses to report a linter warning. Each such waring must have a name so the user
   * can suppress it. The name is relative to the aspect name.
   *
   * <p>This method should usually not be directly called. Instead, use the {@link LintRule}
   * mechanism.
   */
  public void lintWarning(String ruleName, Object elementOrLocation,
      String message, Object... params) {
    model.addDiagIfNotSuppressed(elementOrLocation, Diag.warning(getLocation(elementOrLocation),
        Model.diagPrefixForLint(aspectName, ruleName) + message, params));
  }

  private Location getLocation(Object elementOrLocation) {
    if (elementOrLocation instanceof Location) {
      return (Location) elementOrLocation;
    }
    if (elementOrLocation instanceof Element) {
      return ((Element) elementOrLocation).getLocation();
    }
    return SimpleLocation.TOPLEVEL;
  }

  /**
   * Return a view of this aspect as a diag collector. This allows for abstracting
   * the aspect to make code better testable.
   */
  public DiagCollector asDiagCollector() {
    return new DiagCollector() {

      @Override public void addDiag(Diag diag) {
        switch (diag.getKind()) {
          case ERROR:
            error(diag.getLocation(), diag.getMessage());
            break;
          case WARNING:
            warning(diag.getLocation(), diag.getMessage());
            break;
        }
      }

      @Override public int getErrorCount() {
        return model.getErrorCount();
      }
    };
  }
}

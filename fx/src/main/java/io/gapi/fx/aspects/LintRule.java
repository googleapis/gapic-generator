package io.gapi.fx.aspects;

import io.gapi.fx.model.Element;
import com.google.common.base.Preconditions;

/**
 * Base class of lint rules, which log style violations.
 *
 * <p>Lint rules are managed via {@link ConfigAspectBase} where they are registered during
 * construction time and automatically executed during the linting phase.
 */
public abstract class LintRule<E extends Element> {

  private final ConfigAspectBase aspect;
  private final String ruleName;
  private final Class<E> elemClass;

  /**
   * Constructs a lint rule applicable to the specified model element class. All
   * lint warnings emitted by the rule use the passed rule name.
   */
  protected LintRule(ConfigAspectBase aspect, String ruleName, Class<E> elemClass) {
    this.aspect = Preconditions.checkNotNull(aspect);
    this.ruleName = Preconditions.checkNotNull(ruleName);
    this.elemClass = Preconditions.checkNotNull(elemClass);
  }

  /**
   * Gets the element class this rule works on.
   */
  public Class<E> getElementClass() {
    return elemClass;
  }

  /**
   * Gets the name of the rule used in diagnosis.
   */
  public String getName() {
    return ruleName;
  }

  /**
   * Runs the rule. All issues should be reported using the
   * {@link #warning(Element, String, Object...)} method.
   */
  public abstract void run(E element);

  /**
   * Logs a lint warning.
   */
  protected void warning(E element, String message, Object... params) {
    aspect.lintWarning(ruleName, element, message, params);
  }
}

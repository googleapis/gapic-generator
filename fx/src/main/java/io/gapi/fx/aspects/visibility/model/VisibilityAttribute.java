package io.gapi.fx.aspects.visibility.model;

import com.google.api.VisibilityRule;
import com.google.common.base.Preconditions;
import com.google.inject.Key;

import java.util.Set;

/**
 * Visibility related configuration, including visibility rule and visibility labels in the rule.
 */
public class VisibilityAttribute {

  /**
   * Key to access this attribute.
   */
  public static final Key<VisibilityAttribute> KEY = Key.get(VisibilityAttribute.class);

  private final VisibilityRule rule;
  private final Set<String> visibilityLabels;

  public VisibilityAttribute(VisibilityRule rule, Set<String> visibilityLabels) {
    Preconditions.checkNotNull(rule);
    Preconditions.checkNotNull(visibilityLabels);
    this.rule = rule;
    this.visibilityLabels = visibilityLabels;
  }

  public VisibilityRule getVisibilityRule() {
    return rule;
  }

  public Set<String> getVisibilityLabels() {
    return visibilityLabels;
  }
}

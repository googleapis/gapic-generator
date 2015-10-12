package io.gapi.fx.aspects;

import io.gapi.fx.model.Diag;
import io.gapi.fx.model.DiagCollector;
import io.gapi.fx.model.ProtoElement;
import io.gapi.fx.model.SimpleLocation;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Represents a configuration rule set, allowing to discover a configuration rule for a given
 * proto element. Helper type to implement config aspects.
 */
public class ConfigRuleSet<RuleType extends Message> {

  /**
   * Constructs a rule set for the given type and rules.
   */
  public static <RuleType extends Message>
  ConfigRuleSet<RuleType> of(Descriptor ruleDescriptor, List<RuleType> rules) {
    return new ConfigRuleSet<RuleType>(ruleDescriptor, rules);
  }

  private static final int SELECTOR_FIELD_NUM = 1;
  private static final String SELECTOR_FIELD_NAME = "selector";
  private static final Joiner SELECTOR_JOINER = Joiner.on(',');
  // This pattern is: <segment>('.' <segment>)*('.' '*')? OR '*'
  private static final Pattern SELECTOR_PATTERN =
      Pattern.compile("^(\\w+(\\.\\w+)*(\\.\\*)?)|\\*$");

  private final List<Rule<RuleType>> rules;
  private final FieldDescriptor selectorField;
  private final Map<ProtoElement, RuleType> ruleMap = Maps.newHashMap();

  /**
   * Mapping from rule to unmatched selectors.
   */
  private final Map<Rule<RuleType>, Set<String>> unmatchedRules = Maps.newHashMap();

  public ConfigRuleSet(Descriptor ruleDescriptor, List<RuleType> rules) {
    Preconditions.checkNotNull(ruleDescriptor);
    this.selectorField = ruleDescriptor.findFieldByNumber(SELECTOR_FIELD_NUM);

    // Sanity check for selector field.
    Preconditions.checkArgument(
        selectorField != null && selectorField.getName().equals(SELECTOR_FIELD_NAME) &&
        selectorField.getType() == FieldDescriptor.Type.STRING &&
        !selectorField.isRepeated(),
        "Config rule selector field not present or has unexpected name, type, or cardinality.");

    this.rules = minimize(buildRules(rules));
    for (Rule<RuleType> rule : this.rules) {
      unmatchedRules.put(rule, Sets.newHashSet(rule.selectors));
    }
  }

  /**
   * Build {@link Rule} instances from given rules.
   */
  private List<Rule<RuleType>> buildRules(List<RuleType> rules) {
    ImmutableList.Builder<Rule<RuleType>> flattened = ImmutableList.builder();
    for (RuleType rule : rules) {
      flattened.add(Rule.of(rule));
    }
    return flattened.build();
  }

  public void reportBadSelectors(DiagCollector collector, String category) {
    for (Rule<RuleType> rule : rules) {
      for (String selector : rule.selectors) {
        if (!SELECTOR_PATTERN.matcher(selector).matches()) {
          collector.addDiag(Diag.error(SimpleLocation.TOPLEVEL,
              "%s rule has bad syntax in selector '%s'. See "
              + " go/api/basics.shtml#configuration_model for information on selector syntax.",
              category, selector));
        }
      }
    }
  }

  /**
   * Minimize a rule set, removing selectors that are subsumed by selectors of subsequent rules.
   * Rules will be removed if all selectors have been subsumed subsequent rules.
   */
  private List<Rule<RuleType>> minimize(List<Rule<RuleType>> rules) {
    ImmutableList.Builder<Rule<RuleType>> minimized = ImmutableList.builder();
    for (int i = 0; i < rules.size(); i++) {
      Rule<RuleType> rule = rules.get(i);
      rule.minimizeSelectors(rules, i + 1);
      if (!rule.selectors.isEmpty()) {
        minimized.add(rule);
      }
    }
    return minimized.build();
  }

  /**
   * Returns the matching rule for the element, or null, if no matching exists.
   */
  @Nullable public RuleType matchingRule(ProtoElement elem) {
    RuleType result = ruleMap.get(elem);
    if (result != null) {
      return result;
    }

    for (int i = rules.size() - 1; i >= 0; i--) {
      Rule<RuleType> rule = rules.get(i);
      String matchedSelector = rule.getMatchedSelector(elem);
      if (matchedSelector != null) {
        ruleMap.put(elem, rule.rule);
        if (unmatchedRules.containsKey(rule)) {
          Set<String> unmatchedSelectors = unmatchedRules.get(rule);
          unmatchedSelectors.remove(matchedSelector);
          if (unmatchedSelectors.isEmpty()) {
            unmatchedRules.remove(rule);
          }
        }
        return rule.rule;
      }
    }
    return null;
  }

  /**
   * Reports any unmatched rules.
   */
  public void reportUnmatchedRules(DiagCollector collector, String category) {
    for (Map.Entry<Rule<RuleType>, Set<String>> unmatched : unmatchedRules.entrySet()) {
      Set<String> selectors = unmatched.getValue();
      selectors.remove("*");
      // For rules which are not general default, report a warning.
      if (!selectors.isEmpty()) {
        String unmatchedSelectors = SELECTOR_JOINER.join(unmatched.getValue());
        collector.addDiag(Diag.warning(SimpleLocation.TOPLEVEL,
            "%s rule has selector(s) '%s' that do not match and are not "
            + "shadowed by other rules.", category, unmatchedSelectors));
      }
    }
  }

  /**
   * Represent Rule which keeps RuleType with comma delimited selectors Flattened into
   * {@link Iterable} of selectors.
   */
  private static class Rule<RuleType extends Message> {
    private static final Splitter SELECTOR_SPLITTER = Splitter.on(',').trimResults();
    private final RuleType rule;
    private final Set<String> selectors;
    private final FieldDescriptor selectorField;

    private static <T extends Message> Rule<T> of(T rule) {
      return new Rule<T>(rule);
    }

    private Rule(RuleType rule) {
      this.rule = rule;
      selectorField = rule.getDescriptorForType().findFieldByNumber(SELECTOR_FIELD_NUM);
      List<String> subSelectors = SELECTOR_SPLITTER.splitToList(getUnflattenedSelector());
      // It seems to be a common pattern to end all subselectors with a comma, even if it's the
      // last one. Warnings about the blank selector are ignored, so we remove the blank selector
      // if we see it. We only do it if there are multiple subselectors.
      if (subSelectors.size() > 1 && "".equals(subSelectors.get(subSelectors.size() - 1).trim())) {
        subSelectors = subSelectors.subList(0, subSelectors.size() - 1);
      }
      this.selectors = Sets.newHashSet(subSelectors);
    }

    /**
     * Returns the unflattened selectors of the rule.
     */
    private String getUnflattenedSelector() {
      return (String) rule.getField(selectorField);
    }

    /**
     * Returns the selector that matches full name of given {@link ProtoElement}.
     * Otherwise, returns null.
     */
    private String getMatchedSelector(ProtoElement elem) {
      for (String selector : selectors) {
        if (matches(selector, elem.getFullName())) {
          return selector;
        }
      }
      return null;
    }

    /**
     * Check whether a name matches selector.
     */
    private boolean matches(String selector, String name) {
      if (selector.equals("*")) {
        return true;
      }
      if (selector.endsWith(".*")) {
        return name.startsWith(selector.substring(0, selector.length() - 1));
      }
      return name.equals(selector);
    }

    /**
     * Remove selectors if they are subsumed by any selectors of given rule list.
     */
    private void minimizeSelectors(List<Rule<RuleType>> rules, int startIndex) {
      for (Iterator<String> iter = selectors.iterator(); iter.hasNext();) {
        String selector = iter.next();
        for (int i = startIndex; i < rules.size(); i++) {
          if (isSubsumed(selector, rules.get(i).selectors)) {
            iter.remove();
            break;
          }
        }
      }
    }

    /**
     * Check whether selector is subsumed by any of other selectors.
     */
    private boolean isSubsumed(String selector, Iterable<String> others) {
      for (String other : others) {
        if (subsumes(other, selector)) {
          return true;
        }
      }
      return false;
    }

    /**
     * Check whether selector subsumes another selector, i.e. every name matching the
     * other selector will also match this selector.
     */
    private boolean subsumes(String selector, String other) {
      if (selector.equals("*")) {
        return true;
      }
      if (other.equals("*")) {
        return false;
      }
      if (selector.endsWith(".*")) {
        selector = selector.substring(0, selector.length() - 1);
        if (other.endsWith(".*")) {
          other = other.substring(0, other.length() - 1);
        }
        return selector.startsWith(other);
      }
      return selector.equals(other);
    }
  }
}

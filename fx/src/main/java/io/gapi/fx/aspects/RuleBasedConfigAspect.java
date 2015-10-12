package io.gapi.fx.aspects;

import com.google.api.Service;
import com.google.inject.Key;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;

import io.gapi.fx.model.Model;
import io.gapi.fx.model.ProtoElement;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Base class for implementing simple, rule-based configuration aspects.
 *
 * <p>In many cases, RuleType == AttributeType, but in some possibly not.
 */
public abstract class RuleBasedConfigAspect<RuleType extends Message, AttributeType>
    extends ConfigAspectBase {


  // TODO: MIGRATION
//  /**
//   * Returns a {@link com.google.internal.api.DerivedService.Builder} based on the
//   * {@link DerivedService} held in the tech.type.Any derived_data field.
//   */
//
//  protected static DerivedService.Builder extractDerivedServiceBuilder(
//      Service.Builder serviceBuilder) {
//    try {
//      return DerivedService.newBuilder(
//          DerivedService.parseFrom(serviceBuilder.getDerivedData().getValue()));
//    } catch (InvalidProtocolBufferException e) {
//      throw new RuntimeException("Failed to parse derived_data", e);
//    }
//  }

  /**
   * Predicate determining whether a rule of the aspect is applicable to the element.
   */
  protected abstract boolean isApplicable(ProtoElement element);

  /**
   * Attempts to synthesize the rule from the IDL, e.g. via an annotation. This is called if there
   * is no matching rule found for a given element. By default, returns null.
   */
  @Nullable protected RuleType fromIdlLayer(ProtoElement element) {
    return null;
  }

  /**
   * Evaluates the rule w.r.t. the given element and returns the attribute which should be attached
   * to the element. May add errors on the element's model, and may return null if evaluation fails.
   */
  @Nullable
  protected abstract AttributeType evaluate(ProtoElement element, RuleType rule, boolean isFromIdl);

  /**
   * Clears the rule builder during normalization.
   */
  protected abstract void clearRuleBuilder(Service.Builder builder);

  /**
   * Adds a rule back to the rule builder, specialized for the given selector.
   */
  protected abstract void addToRuleBuilder(Service.Builder builder, String selector,
      AttributeType attribute);

  private final Key<AttributeType> key;
  private final ConfigRuleSet<RuleType> rules;

  protected RuleBasedConfigAspect(Model model, Key<AttributeType> key, String aspectName,
      Descriptor ruleDescriptor, List<RuleType> rules) {
    super(model, aspectName);
    this.key = key;
    this.rules = new ConfigRuleSet<RuleType>(ruleDescriptor, rules);
    this.rules.reportBadSelectors(getModel(), getAspectName());
  }

  @Override
  public void merge(ProtoElement element) {
    boolean isFromIdl = false;
    if (!isApplicable(element)) {
      return;
    }
    RuleType rule = rules.matchingRule(element);
    if (rule == null) {
      // Try to derive information from IDL layer (e.g. annotation)
      rule = fromIdlLayer(element);
      isFromIdl = true;
    }
    if (rule != null) {
      AttributeType attribute = evaluate(element, rule, isFromIdl);
      if (attribute != null) {
        element.putAttribute(key, attribute);
      }
    }
  }

  @Override
  public void endMerging() {
    // Report any unmatched rules.
    rules.reportUnmatchedRules(getModel(), getAspectName());
  }

  @Override
  public void startNormalization(Service.Builder builder) {
    // Clear all rules, as normalization will re-create them.
    clearRuleBuilder(builder);
  }

  @Override
  public void normalize(ProtoElement element, Service.Builder builder) {
    if (!isApplicable(element)) {
      return;
    }
    AttributeType attribute = element.getAttribute(key);
    if (attribute != null) {
      addToRuleBuilder(builder, element.getFullName(), attribute);
    }
  }
}
package io.gapi.fx.aspects.context;

import com.google.api.ContextRule;
import com.google.api.Service;
import io.gapi.fx.aspects.RuleBasedConfigAspect;
import io.gapi.fx.model.Method;
import io.gapi.fx.model.Model;
import io.gapi.fx.model.ProtoElement;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Key;

/**
 * Configuration aspect for context section.
 */
public class ContextConfigAspect extends RuleBasedConfigAspect<ContextRule, ContextRule> {

  private static final ImmutableSet<String> REQUEST_CONTEXTS =
      // TODO(wgg): consider instead of hard-coding this, retrieving it from
      // the file descriptor or other metadata.
      ImmutableSet.of(
          "google.rpc.context.AbuseContext",
          "google.rpc.context.ConditionRequestContext",
          "google.rpc.context.FieldMaskContext",
          "google.rpc.context.OriginContext",
          "google.rpc.context.ProjectContext",
          "google.rpc.context.HttpHeaderContext",
          "google.rpc.context.VisibilityContext");

  private static final ImmutableSet<String> RESPONSE_CONTEXTS =
      ImmutableSet.of(
          "google.rpc.context.ConditionResponseContext", "google.rpc.context.HttpHeaderContext");

  /**
   * A private key to access context rule attributes.
   */
  private static final Key<ContextRule> KEY = Key.get(ContextRule.class);

  /**
   * Creates new context config aspect.
   */
  public static ContextConfigAspect create(Model model) {
    return new ContextConfigAspect(model);
  }

  ContextConfigAspect(Model model) {
    super(model, KEY, "context", ContextRule.getDescriptor(), model.getServiceConfig()
        .getContext().getRulesList());
  }

  @Override
  protected boolean isApplicable(ProtoElement element) {
    return element instanceof Method;
  }

  @Override
  protected ContextRule evaluate(ProtoElement element, ContextRule rule, boolean isFromIdl) {
    for (String context : rule.getRequestedList()) {
      if (!REQUEST_CONTEXTS.contains(context)) {
        error(element, "Requested context header '%s' is unknown.", context);
      }
    }
    for (String context : rule.getProvidedList()) {
      if (!RESPONSE_CONTEXTS.contains(context)) {
        error(element, "Provided context header '%s' is unknown.", context);
      }
    }
    return rule;
  }

  @Override
  protected void clearRuleBuilder(Service.Builder builder) {
    builder.getContextBuilder().clearRules();
  }

  @Override
  protected void addToRuleBuilder(Service.Builder serviceBuilder, String selector,
      ContextRule attribute) {
    serviceBuilder.getContextBuilder()
      .addRules(attribute.toBuilder().setSelector(selector).build());
  }
}

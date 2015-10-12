package io.gapi.fx.aspects.visibility;

import com.google.api.AnnotationsProto;
import com.google.api.Service;
import com.google.api.Service.Builder;
import com.google.api.Visibility;
import com.google.api.VisibilityRule;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.protobuf.BoolValue;

import io.gapi.fx.aspects.RuleBasedConfigAspect;
import io.gapi.fx.aspects.visibility.model.VisibilityAttribute;
import io.gapi.fx.model.EnumType;
import io.gapi.fx.model.EnumValue;
import io.gapi.fx.model.Field;
import io.gapi.fx.model.Interface;
import io.gapi.fx.model.MessageType;
import io.gapi.fx.model.Method;
import io.gapi.fx.model.Model;
import io.gapi.fx.model.ProtoElement;
import io.gapi.fx.model.ProtoFile;
import io.gapi.fx.model.Visitor;
import io.gapi.fx.util.Visits;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Configuration aspect for visibility.
 */
public class VisibilityConfigAspect extends
    RuleBasedConfigAspect<VisibilityRule, VisibilityAttribute> {

  // Visibility label name allows word characters, hyphens and underscores.
  private static final Pattern VISIBILITY_LABEL_REGEX_PATTERN = Pattern.compile("^[\\w-_]+$");

  private final Set<String> visibilityLabels = Sets.newLinkedHashSet();

  // Explicitly set the default to false. It can be changed if we want to turn this setting on
  // for certain versions.
  private boolean topLevelEnforceRuntimeSetting = false;

  public static VisibilityConfigAspect create(Model model) {
    return new VisibilityConfigAspect(model);
  }

  private VisibilityConfigAspect(Model model) {
    super(model, VisibilityAttribute.KEY, "visibility", VisibilityRule.getDescriptor(),
        model.getServiceConfig().getVisibility().getRulesList());
  }

  @Override
  public void startMerging() {
    super.startMerging();
    Visibility visibilityConfig = getModel().getServiceConfig().getVisibility();
    if (visibilityConfig.hasEnforceRuntimeVisibility()) {
      topLevelEnforceRuntimeSetting = visibilityConfig.getEnforceRuntimeVisibility().getValue();
    } else if (getModel().getConfigVersion() > 1) {
      topLevelEnforceRuntimeSetting = true;
    }
  }

  @Override
  public void endMerging() {
    super.endMerging();
    Model model = getModel();
    Set<Set<String>> declaredVisibilityCombinations = Sets.newLinkedHashSet();
    declaredVisibilityCombinations.add(Sets.<String>newLinkedHashSet());
    for (String label : visibilityLabels) {
      declaredVisibilityCombinations.add(Sets.newHashSet(label));
    }
    Visibility visibilityConfig = getModel().getServiceConfig().getVisibility();
    for (String labelCombinations : visibilityConfig.getLabelCombinationsList()) {
      declaredVisibilityCombinations.add(buildLabelCombination(getModel(), labelCombinations));
    }
    model.setDeclaredVisibilityCombinations(declaredVisibilityCombinations);
  }

  @Override
  protected boolean isApplicable(ProtoElement element) {
    return true; // Applicable to all protocol elements
  }

  @Override
  protected VisibilityRule fromIdlLayer(ProtoElement element) {
    return new VisibilityFromIdlVisitor().run(element);
  }


  @Override
  protected VisibilityAttribute evaluate(ProtoElement element, VisibilityRule rule,
      boolean isFromIdl) {
    // Merge the `enforce_runtime_visibility` setting from top level setting and that of the rule.
    boolean mergedRuntimeEnforcementFlag = rule.hasEnforceRuntimeVisibility()
        ? rule.getEnforceRuntimeVisibility().getValue()
        : topLevelEnforceRuntimeSetting;
    VisibilityRule merged = rule.toBuilder()
        .setEnforceRuntimeVisibility(BoolValue.newBuilder().setValue(mergedRuntimeEnforcementFlag))
        .build();
    return new VisibilityAttribute(merged, buildRuleLabels(element, merged));
  }

  @Override
  protected void clearRuleBuilder(Builder builder) {
    builder.getVisibilityBuilder().clearRules();
  }

  @Override
  protected void addToRuleBuilder(
      Service.Builder serviceBuilder, String selector, VisibilityAttribute attribute) {
    // Add visibility config for this selector.
    serviceBuilder.getVisibilityBuilder().addRules(
        attribute.getVisibilityRule().toBuilder().setSelector(selector).build());
  }

  @Override
  public void endNormalization(Service.Builder serviceBuilder) {
    super.endNormalization(serviceBuilder);
    // TODO: MIGRATION
    //DerivedService.Builder derivedService = extractDerivedServiceBuilder(serviceBuilder);
    //addDerivedVisibility(getModel(), derivedService);
    //serviceBuilder.setDerivedData(
    //    ProtoAnyConverter.convertProtoToAnyWithStandardUrl(derivedService.build()));
  }

  /**
   * A one-level visitor which extracts the visibility rule annotation.
   */
  private static class VisibilityFromIdlVisitor extends Visitor {
    private VisibilityRule result;

    private VisibilityRule run(ProtoElement elem) {
      visit(elem);
      if (result != null && !result.equals(VisibilityRule.getDefaultInstance())) {
        return result;
      }
      return null;
    }

    @Visits void getVisibility(ProtoFile elem) {
      result = elem.getProto().getOptions().getExtension(AnnotationsProto.fileVisibility);
    }

    @Visits void getVisibility(Interface elem) {
      result = elem.getProto().getOptions().getExtension(AnnotationsProto.apiVisibility);
    }

    @Visits void getVisibility(Method elem) {
      result = elem.getDescriptor().getMethodAnnotation(AnnotationsProto.methodVisibility);
    }

    @Visits void getVisibility(MessageType elem) {
      result = elem.getProto().getOptions().getExtension(AnnotationsProto.messageVisibility);
    }

    @Visits void getVisibility(Field elem) {
      result = elem.getProto().getOptions().getExtension(AnnotationsProto.fieldVisibility);
    }

    @Visits void getVisibility(EnumType elem) {
      result = elem.getProto().getOptions().getExtension(AnnotationsProto.enumVisibility);
    }

    @Visits void getVisibility(EnumValue elem) {
      result = elem.getProto().getOptions().getExtension(AnnotationsProto.valueVisibility);
    }
  }

  /*
   * Builds and validates visibility labels for a {@link VisibilityRule}.
   */
  private Set<String> buildRuleLabels(ProtoElement element, VisibilityRule rule) {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    if (rule.getRestriction().isEmpty()) {
      // Handles a VisibilityRule without a restriction, in order to let us set other visibility
      // options for publicly accessible elements. The empty set indicates there are no visibility
      // restrictions on the element.
      return builder.build();
    }
    for (String label : Splitter.on(',').trimResults().splitToList(rule.getRestriction())) {
      if (VISIBILITY_LABEL_REGEX_PATTERN.matcher(label).matches()) {
        builder.add(label);
      } else {
        error(element, "Element '%s' has invalid visibility label '%s'.", element.getFullName(),
            label);
      }
    }
    Set<String> labels = builder.build();
    visibilityLabels.addAll(labels);
    return labels;
  }

  /*
   * Builds and validates visibility labels for a {@link VisibilityRule}.
   */
  private Set<String> buildLabelCombination(Model model, String commaDelimitedLabels) {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    for (String label : Splitter.on(',').trimResults().splitToList(commaDelimitedLabels)) {
      if (VISIBILITY_LABEL_REGEX_PATTERN.matcher(label).matches()
          && visibilityLabels.contains(label)) {
        builder.add(label);
      } else {
        error(model, "Label combination '%s' has invalid visibility label: '%s'.",
            commaDelimitedLabels, label);
      }
    }
    return builder.build();
  }

  // TODO: MIGRATION

//  /**
//   * Precompute all the visibility combinations that could be applied to a request
//   * and attach the results to the provided builder. Note this can produce errors
//   * as a scoper is computed for each label combination.
//   */
//  private void addDerivedVisibility(final Model model,
//      final DerivedService.Builder derivedService) {
//    for (final Set<String> visibilityLabels : model.getDeclaredVisibilityCombinations()) {
//      VisibilityUtil.withScopedModel(model, visibilityLabels, new Supplier<Void>() {
//        @Override
//        public Void get() {
//          // Add results of scoping to model.
//          DerivedVisibility.Builder derivedVisibility = DerivedVisibility.newBuilder()
//              .addAllVisibilityLabels(visibilityLabels)
//              .addAllMethods(getMethodVisibilityList(model))
//              .addAllEnums(getScopedEnums(model));
//
//          // No need to add anything if this is the public surface and nothing is hidden.
//          if (!visibilityLabels.isEmpty()
//              || !derivedVisibility.getMethodsList().isEmpty()
//              || !derivedVisibility.getEnumsList().isEmpty()) {
//            derivedService.addVisibility(derivedVisibility);
//          }
//          return null;
//        }
//      });
//    }
//  }
//
//  /**
//   * Returns a {@link MethodDerivedVisibility} for each method in the model that is
//   * either hidden or has a field mask. Returns an empty list if there are no such
//   * methods.
//   */
//  private List<MethodDerivedVisibility> getMethodVisibilityList(final Model scopedModel) {
//    final List<MethodDerivedVisibility> derivedMethods = Lists.newArrayList();
//    new Visitor() {
//      @Visits
//      public void checkMethods(Method method) {
//        MethodDerivedVisibility.Builder derived =
//            MethodDerivedVisibility.newBuilder()
//                .setMethod(method.getFullName())
//                .setHidden(!method.isReachable());
//        if (!derived.getHidden()) {
//          FieldMask inputMask = FieldMaskBuilder.getVisibleFieldMask(method.getInputMessage());
//          if (inputMask != null) {
//            derived.setInputMask(inputMask);
//          }
//          FieldMask outputMask = FieldMaskBuilder.getVisibleFieldMask(method.getOutputMessage());
//          if (outputMask != null) {
//            derived.setOutputMask(outputMask);
//          }
//        }
//        if (derived.getHidden()
//            || !derived.getInputMask().getPathsList().isEmpty()
//            || !derived.getOutputMask().getPathsList().isEmpty()) {
//          derivedMethods.add(derived.build());
//        }
//      }
//    }.visit(scopedModel);
//    return derivedMethods;
//  }
//
//  /**
//   * Returns a list of enums that have hidden values, or an empty list if no enums
//   * have hidden values.
//   */
//  private List<com.google.protobuf.Enum> getScopedEnums(Model scopedModel) {
//    final List<com.google.protobuf.Enum> scopedEnums = Lists.newArrayList();
//    new Visitor(scopedModel.getScoper()) {
//      @VisitsBefore public void detectScopedEnum(EnumType enumType) {
//        if (enumType.hasHiddenValue()) {
//          scopedEnums.add(enumType.getVisibleProto());
//        }
//      }
//    }.visit(scopedModel);
//    return scopedEnums;
//  }
}

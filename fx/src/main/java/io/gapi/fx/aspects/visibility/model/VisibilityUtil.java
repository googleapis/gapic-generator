package io.gapi.fx.aspects.visibility.model;

import io.gapi.fx.model.Model;
import io.gapi.fx.model.Scoper;
import io.gapi.fx.model.stages.Merged;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.Sets;

import java.util.Set;

import javax.annotation.Nullable;

/**
 * Utilities for working with visibility.
 */
public class VisibilityUtil {

  /**
   * Function to display a set of visibility labels in a user readable way.
   */
  public static final Function<Set<String>, String> DISPLAY_VISIBILITY =
      new Function<Set<String>, String>() {
        @Override public String apply(@Nullable Set<String> labels) {
          if (labels == null) {
            return "none";
          }
          if (labels.isEmpty()) {
            return "{} (public)";
          }
          return "{" + Joiner.on(",").join(labels) + "}";
        }

      };

  /**
   * If provided comma separated list of labels is not null, scope the model down
   * to the provided labels. This may produce errors resulting from violation of
   * visibility rules.
   */
  public static void scopeModel(Model model, @Nullable String visibilityLabels) {
    scopeModel(model, parseLabels(visibilityLabels));
  }

  /**
   * Same as {@link #scopeModel(Model, String)} but with a set of parsed labels.
   */
  public static void scopeModel(Model model, @Nullable Set<String> visibilityLabels) {
    if (visibilityLabels == null) {
      return;
    }
    model.establishStage(Merged.KEY);
    model.setVisibilityLabels(visibilityLabels);
    model.setScoper(model.getScoper().restrict(model, visibilityLabels));
  }

  /**
   * Execute the given action within a model scoped to visibility labels. Restores the
   * original model state.
   */
  public static <A> A withScopedModel(Model model, @Nullable String visibilityLabels,
      Supplier<A> action) {
    return withScopedModel(model, parseLabels(visibilityLabels), action);
  }

  /**
   * Same as {@link #withScopedModel(Model, String, Supplier)} but with a set of parsed labels.
   */
  public static <A> A withScopedModel(Model model, @Nullable Set<String> visibilityLabels,
      Supplier<A> action) {
    model.establishStage(Merged.KEY);
    Set<String> savedLabels = model.setVisibilityLabels(visibilityLabels);
    Scoper savedScoper = model.setScoper(model.getScoper().restrict(model, visibilityLabels));
    A result = action.get();
    model.setScoper(savedScoper);
    model.setVisibilityLabels(savedLabels);
    return result;
  }

  private static Set<String> parseLabels(String labels) {
    if (labels == null) {
      return null;
    }
    return Sets.newLinkedHashSet(
        Splitter.on(',').trimResults().omitEmptyStrings().splitToList(labels));
  }
}

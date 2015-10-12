package io.gapi.fx.model;

import com.google.api.Service;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Represents an abstraction of a configuration cross-cut, like documentation, http, auth, etc. In
 * order to integrate a configuration aspect into the system, this interface should not be directly
 * implemented, instead extend one of the abstract base classes implementing this interface in the
 * {@code aspects} package.
 *
 * <p>Processing of aspects is performed in three phases:
 *
 * <ol>
 * <li>Merging: During this phase the configuration is merged with the protocol definition.
 * Essential validation of the configuration is performed, and errors are reported. Attributes may
 * be added to the model representing information relevant to the aspect.</li>
 *
 * <li>Linting (style checking): During this phase, additional, non-essential validation is
 * performed on the model. This phase is optional and produces warnings.</li>
 *
 * <li>Normalization: In this phase the configuration is re-build in a normalized form, as it is
 * expected by downstream tools.</li>
 * </ol>
 *
 * Each phase is preluded by a method call to indicate its start, followed by a call to each model
 * element, followed by a call indicating the end of the phase.
 *
 * <p>Config aspects can communicate with each other by attaching attributes to the model.
 * Attributes are usually synthesized during the merging phase. If one aspect depends on another's
 * attributes in the merging phase, the dependency must be specified using the
 * {@link #mergeDependencies()} method.
 */
public interface ConfigAspect {

  /**
   * Returns the string identifying the aspect in diagnosis.
   */
  String getAspectName();

  /**
   * Returns the set of all style rule names related to this aspect.
   */
  Set<String> getLintRuleNames();

  /**
   * Returns the types of aspects which must be run before this aspect during the merging phase.
   */
  List<Class<? extends ConfigAspect>> mergeDependencies();

  /**
   * Marks start of merging. Can be used to merge model-global config and prepare merging.
   */
  void startMerging();

  /**
   * Merges the config aspect with the given proto element. This will be called for each element of
   * the model during the merging phase, breadth first. The config aspect decides whether it is
   * applicable for the element, performs merging, potentially reporting diagnostics on the model,
   * and attaching attributes via {@link Model#putAttribute(com.google.inject.Key, Object)}.
   */
  void merge(ProtoElement element);

  /**
   * Marks end of merging. Can be used to do post-merging validation.
   */
  void endMerging();

  /**
   * Marks the begin of style checking.
   */
  void startLinting();

  /**
   * Style checks the given element. This will be called for each element, breadth first.
   */
  void lint(ProtoElement element);

  /**
   * Marks end of style checking.
   */
  void endLinting();

  /**
   * Marks start of normalization. For normalization, the original service definition will be passed
   * in as a builder; this call can be used to clear any data related to this aspect before adding
   * it back in normalized form.
   */
  void startNormalization(Service.Builder builder);

  /**
   * Normalizes the service config w.r.t. to this aspect. Will be called for each element in the
   * model during normalization phase. If the config type has applicable aspects for the element it
   * can update the service config with normalized and derived data.
   */
  void normalize(ProtoElement element, Service.Builder builder);

  /**
   * Marks end of normalization. Can be used to do cleanup.
   */
  void endNormalization(Service.Builder builder);

  /**
   * The title of the configuration aspect documentation for given proto element. This is
   * called only after the merging phase has successfully concluded for the whole model. Return
   * null to indicate that no documentation is available.
   */
  @Nullable String getDocumentationTitle(ProtoElement element);

  /**
   * The configuration aspect documentation for the proto element. The documentation should be in
   * Markdown format. This is called only after the merging phase has successfully concluded for the
   * whole model. Return null to indicate no documentation is available.
   */
  @Nullable String getDocumentation(ProtoElement element);
}

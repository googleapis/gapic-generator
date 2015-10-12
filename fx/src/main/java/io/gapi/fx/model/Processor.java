package io.gapi.fx.model;

import com.google.common.collect.ImmutableList;
import com.google.inject.Key;

/**
 * Interface for a processor which performs an analysis or transformation task on the model.
 */
public interface Processor {

  /**
   * The list of stages this processor needs for its operations. Stages are attached as attributes
   * on the {@link Model}. A stage may or may not be associated with actual data of the type of the
   * parameter of the key.
   */
  ImmutableList<Key<?>> requires();

  /**
   * The stage this processor establishes if operation successfully finishes. The processor is
   * responsible to attach the stage key to the model.
   */
  Key<?> establishes();

  /**
   * Runs this processor. It is guaranteed that the {@link Processor#requires()} stages are
   * established before this method is called via {@link Model#establishStage(Key)}. The method
   * should return true if processing successfully finished and the given {@link #establishes()} key
   * has been attached to the model. Any errors, warnings, and hints during processing should be
   * attached to the model via {@link Model#addDiag(Diag)}.
   */
  boolean run(Model model);
}

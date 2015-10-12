package io.gapi.fx.model.stages;

import com.google.inject.Key;

/**
 * A marker class representing the stage that merging of deployment config and annotations has
 * happened.
 *
 * <p>
 * See the usage of {@code Requires(Merged.class)} in the model for information dependent on this
 * stage.
 */
public class Merged {

  /**
   * The key representing the resolved stage.
   */
  public static final Key<Merged> KEY = Key.get(Merged.class);
}

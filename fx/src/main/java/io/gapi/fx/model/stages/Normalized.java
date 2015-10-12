package io.gapi.fx.model.stages;

import com.google.inject.Key;

/**
 * A marker class representing the stage that config normalization has been performed
 * on the service.
 *
 * <p>See the usage of {@code Requires(Normalized.class)} in the model for information that is
 * dependent on this stage.
 */
public class Normalized {
  /**
   * The key representing the normalized stage.
   */
  public static final Key<Normalized> KEY = Key.get(Normalized.class);
}

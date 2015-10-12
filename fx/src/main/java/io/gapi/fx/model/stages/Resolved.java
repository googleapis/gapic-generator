package io.gapi.fx.model.stages;

import com.google.inject.Key;

/**
 * A marker class representing the stage that type resolution and basic consistency checking has
 * been performed on the api.
 *
 * <p>
 * See the usage of {@code Requires(Resolved.class)} in the model for information that is dependent
 * on this stage.
 */
public class Resolved {

  /**
   * The key representing the resolved stage.
   */
  public static final Key<Resolved> KEY = Key.get(Resolved.class);
}

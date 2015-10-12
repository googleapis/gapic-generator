package io.gapi.fx.model.stages;

import com.google.inject.Key;

/**
 * A marker class representing the stage that the model has been style checked (linted).
 */
public class Linted {

  /**
   * The key representing the linted stage.
   */
  public static final Key<Linted> KEY = Key.get(Linted.class);
}

package io.gapi.fx.model.stages;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An annotation to document that a processing stage is required for having an attribute
 * valid on a model element.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Requires {

  /**
   * The class to represent the stage.
   */
  Class<?> value();
}

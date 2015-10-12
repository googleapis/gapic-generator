package io.gapi.fx.model;

/**
 * An abstraction of a source location.
 */
public interface Location {

  // TODO(wgg): determine whether we want to provide structure here.

  /**
   * Get the location as a string readable to users and interpretable by IDEs. The actual
   * semantics depends on the underlying source type. This may or not be the same as
   * toString().
   */
  String getDisplayString();
}
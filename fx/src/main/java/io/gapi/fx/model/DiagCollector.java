package io.gapi.fx.model;

/**
 * An interface representing an object which collects diagnostics.
 */
public interface DiagCollector {

  /**
   * Adds a diagnosis.
   */
  void addDiag(Diag diag);

  /**
   * Returns the number of diagnosed proper errors.
   */
  int getErrorCount();
}

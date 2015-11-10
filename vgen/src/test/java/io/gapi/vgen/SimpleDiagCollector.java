package io.gapi.vgen;

import java.util.List;

import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.common.collect.Lists;

public class SimpleDiagCollector implements DiagCollector {
  private final List<Diag> diags = Lists.newArrayList();
  private int errorCount;

  /**
   * Adds a diagnosis.
   */
  @Override
  public void addDiag(Diag diag) {
    diags.add(diag);
    if (diag.getKind() == Diag.Kind.ERROR) {
      errorCount++;
    }
  }

  /**
   * Returns the number of diagnosed proper errors.
   */
  @Override
  public int getErrorCount() {
    return errorCount;
  }

  /**
   * Returns the diagnosis accumulated.
   */
  public List<Diag> getDiags() {
    return diags;
  }
}

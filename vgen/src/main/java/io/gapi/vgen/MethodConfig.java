package io.gapi.vgen;

import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Method;

import javax.annotation.Nullable;

/**
 * MethodConfig represents the code-gen config for a method, and includes the
 * specification of features like page streaming and parameter flattening.
 */
public class MethodConfig {

  private final PageStreaming pageStreaming;

  /**
   * Creates an instance of MethodConfig based on MethodConfigProto, linking it
   * up with the provided method. On errors, null will be returned, and
   * diagnostics are reported to the diag collector.
   */
  @Nullable public static MethodConfig createMethodConfig(DiagCollector diagCollector,
      MethodConfigProto methodConfig, Method method) {
    if (PageStreamingConfigProto.getDefaultInstance().equals(methodConfig.getPageStreaming())) {
      return new MethodConfig(null);
    }

    PageStreaming pageStreaming =
        PageStreaming.createPageStreaming(diagCollector,
            methodConfig.getPageStreaming(), method);
    if (pageStreaming == null) {
      return null;
    } else {
      return new MethodConfig(pageStreaming);
    }
  }

  private MethodConfig(PageStreaming pageStreaming) {
    this.pageStreaming = pageStreaming;
  }

  /**
   * Returns true if this method has page streaming configured.
   */
  public boolean isPageStreaming() {
    return pageStreaming != null;
  }

  /**
   * Returns the page streaming configuration of the method.
   */
  public PageStreaming getPageStreaming() {
    return pageStreaming;
  }
}

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
  private final Flattening flattening;

  /**
   * Creates an instance of MethodConfig based on MethodConfigProto, linking it
   * up with the provided method. On errors, null will be returned, and
   * diagnostics are reported to the diag collector.
   */
  @Nullable public static MethodConfig createMethodConfig(DiagCollector diagCollector,
      MethodConfigProto methodConfig, Method method) {

    boolean error = false;
    
    PageStreaming pageStreaming;
    if (PageStreamingConfigProto.getDefaultInstance().equals(methodConfig.getPageStreaming())) {
      pageStreaming = null;
    } else {
      pageStreaming = PageStreaming.createPageStreaming(diagCollector, 
          methodConfig.getPageStreaming(), method);
      if (pageStreaming == null) {
        error = true;
      }
    }

    Flattening flattening; 
    if (FlatteningConfigProto.getDefaultInstance().equals(methodConfig.getFlattening())) {
      flattening = null;
    } else {
      flattening = 
          Flattening.createFlattening(diagCollector, 
              methodConfig.getFlattening(), method);
      if (flattening == null) {
        error = true;
      }
    } 
        
    if (error) {
      return null;
    } else {
      return new MethodConfig(pageStreaming, flattening);
    }
  }

  private MethodConfig(PageStreaming pageStreaming, Flattening flattening) {
    this.pageStreaming = pageStreaming;
    this.flattening = flattening;
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

  /**
   * Returns true if this method has flattening configured.
   */
  public boolean isFlattening() {
    return flattening != null;
  }

  /**
   * Returns the flattening configuration of the method.
   */
  public Flattening getFlattening() {
    return flattening;
  }
}

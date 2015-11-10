package io.gapi.vgen;

import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SimpleLocation;

import javax.annotation.Nullable;

/**
 * PageStreaming represents the page streaming configuration for a method.
 */
public class PageStreaming {
  private final Field requestTokenField;
  private final Field responseTokenField;
  private final Field resourcesField;

  /**
   * Creates an instance of PageStreaming based on PageStreamingConfigProto, linking it
   * up with the provided method. On errors, null will be returned, and diagnostics
   * are reported to the diag collector.

   */
  @Nullable public static PageStreaming createPageStreaming(DiagCollector diagCollector,
      PageStreamingConfigProto pageStreaming, Method method) {
    String requestTokenFieldName = pageStreaming.getRequest().getTokenField();
    Field requestTokenField =
        method.getInputType().getMessageType().lookupField(requestTokenFieldName);
    if (requestTokenField == null) {
      diagCollector.addDiag(Diag.error(SimpleLocation.TOPLEVEL,
          "Field missing for page streaming: method = %s, message type = %s, field = %s",
          method.getFullName(), method.getInputType().getMessageType().getFullName(),
          requestTokenFieldName));
    }

    String responseTokenFieldName = pageStreaming.getResponse().getTokenField();
    Field responseTokenField =
        method.getOutputType().getMessageType().lookupField(responseTokenFieldName);
    if (responseTokenField == null) {
      diagCollector.addDiag(Diag.error(SimpleLocation.TOPLEVEL,
          "Field missing for page streaming: method = %s, message type = %s, field = %s",
          method.getFullName(), method.getOutputType().getMessageType().getFullName(),
          responseTokenFieldName));
    }

    String resourcesFieldName = pageStreaming.getResponse().getResourcesField();
    Field resourcesField =
        method.getOutputType().getMessageType().lookupField(resourcesFieldName);
    if (resourcesField == null) {
      diagCollector.addDiag(Diag.error(SimpleLocation.TOPLEVEL,
          "Field missing for page streaming: method = %s, message type = %s, field = %s",
          method.getFullName(), method.getOutputType().getMessageType().getFullName(),
          resourcesFieldName));
    }

    if (requestTokenField == null || responseTokenField == null || resourcesField == null) {
      return null;
    }
    return new PageStreaming(requestTokenField, responseTokenField, resourcesField);
  }

  private PageStreaming(Field requestTokenField, Field responseTokenField,
      Field resourcesField) {
    this.requestTokenField = requestTokenField;
    this.responseTokenField = responseTokenField;
    this.resourcesField = resourcesField;
  }

  /**
   * Returns the field used in the request to hold the page token.
   */
  public Field getRequestTokenField() {
    return requestTokenField;
  }

  /**
   * Returns the field used in the response to hold the next page token.
   */
  public Field getResponseTokenField() {
    return responseTokenField;
  }

  /**
   * Returns the field used in the response to hold the resource being returned.
   */
  public Field getResourcesField() {
    return resourcesField;
  }
}

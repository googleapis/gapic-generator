/* Copyright 2016 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen;

import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SimpleLocation;

import javax.annotation.Nullable;

/**
 * PageStreamingConfig represents the page streaming configuration for a method.
 */
public class PageStreamingConfig {
  private final Field requestPageSizeField;
  private final Field requestTokenField;
  private final Field responseTokenField;
  private final Field resourcesField;

  /**
   * Creates an instance of PageStreamingConfig based on PageStreamingConfigProto, linking it up
   * with the provided method. On errors, null will be returned, and diagnostics are reported to the
   * diag collector.
   *
   */
  @Nullable
  public static PageStreamingConfig createPageStreaming(
      DiagCollector diagCollector, PageStreamingConfigProto pageStreaming, Method method) {
    String requestPageSizeFieldName = pageStreaming.getRequest().getPageSizeField();
    Field requestPageSizeField =
        method.getInputType().getMessageType().lookupField(requestPageSizeFieldName);
    if (requestPageSizeField == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Request field missing for page streaming: method = %s, message type = %s, field = %s",
              method.getFullName(),
              method.getInputType().getMessageType().getFullName(),
              requestPageSizeFieldName));
    }

    String requestTokenFieldName = pageStreaming.getRequest().getTokenField();
    Field requestTokenField =
        method.getInputType().getMessageType().lookupField(requestTokenFieldName);
    if (requestTokenField == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Request field missing for page streaming: method = %s, message type = %s, field = %s",
              method.getFullName(),
              method.getInputType().getMessageType().getFullName(),
              requestTokenFieldName));
    }

    String responseTokenFieldName = pageStreaming.getResponse().getTokenField();
    Field responseTokenField =
        method.getOutputType().getMessageType().lookupField(responseTokenFieldName);
    if (responseTokenField == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Response field missing for page streaming: method = %s, message type = %s, field = %s",
              method.getFullName(),
              method.getOutputType().getMessageType().getFullName(),
              responseTokenFieldName));
    }

    String resourcesFieldName = pageStreaming.getResponse().getResourcesField();
    Field resourcesField = method.getOutputType().getMessageType().lookupField(resourcesFieldName);
    if (resourcesField == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Resources field missing for page streaming: method = %s, message type = %s, field = %s",
              method.getFullName(),
              method.getOutputType().getMessageType().getFullName(),
              resourcesFieldName));
    }

    if (requestPageSizeField == null || requestTokenField == null || responseTokenField == null || resourcesField == null) {
      return null;
    }
    return new PageStreamingConfig(requestPageSizeField, requestTokenField, responseTokenField, resourcesField);
  }

  private PageStreamingConfig(
      Field requestPageSizeField, Field requestTokenField, Field responseTokenField, Field resourcesField) {
    this.requestPageSizeField = requestPageSizeField;
    this.requestTokenField = requestTokenField;
    this.responseTokenField = responseTokenField;
    this.resourcesField = resourcesField;
  }

  /**
   * Returns the field used in the request to specify desired page size.
   */
  public Field getRequestPageSizeField() {
    return requestPageSizeField;
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

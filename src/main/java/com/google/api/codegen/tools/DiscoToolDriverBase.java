/* Copyright 2017 Google Inc
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
package com.google.api.codegen.tools;

import com.google.api.codegen.DiscoGapicGeneratorApi;
import com.google.api.codegen.DocumentGenerator;
import com.google.api.codegen.discovery.Document;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.api.tools.framework.tools.GenericToolDriverBase;
import com.google.api.tools.framework.tools.ToolOptions;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * Abstract base class for drivers running tools based on the framework.
 *
 * <p>Uses {@link ToolOptions} to pass arguments to the driver.
 */
public abstract class DiscoToolDriverBase extends GenericToolDriverBase {

  protected Document document;

  protected DiscoToolDriverBase(ToolOptions options) {
    super(options);
  }

  /** Returns the document. */
  @Nullable
  public Document getDocument() {
    return document;
  }

  /** Runs the tool. Returns a non-zero exit code on errors. */
  @Override
  public int run() {
    this.document = setupDocument();
    return super.run();
  }

  /** Initializes the Discovery document document. */
  private Document setupDocument() {
    // Prevent INFO messages from polluting the log.
    Logger.getLogger("").setLevel(Level.WARNING);
    String discoveryDocPath = options.get(DiscoGapicGeneratorApi.DISCOVERY_DOC);

    Document document = null;
    try {
      document = DocumentGenerator.createDocumentAndLog(discoveryDocPath, getDiagCollector());
    } catch (FileNotFoundException e) {
      getDiagCollector()
          .addDiag(Diag.error(SimpleLocation.TOPLEVEL, "File not found: " + discoveryDocPath));
    } catch (IOException e) {
      getDiagCollector()
          .addDiag(
              Diag.error(
                  SimpleLocation.TOPLEVEL, "Failed to read Discovery Doc: " + discoveryDocPath));
    }
    return document;
  }
}

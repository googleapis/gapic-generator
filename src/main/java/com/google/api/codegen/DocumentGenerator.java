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
package com.google.api.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.codegen.discovery.DiscoveryNode;
import com.google.api.codegen.discovery.Document;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.SimpleLocation;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrealin on 9/1/17.
 */
public class DocumentGenerator {
  public static Document createDocument(String discoveryDocPath) throws IOException {
    if (!new File(discoveryDocPath).exists()) {
      throw new FileNotFoundException();
    }

    Reader reader = new InputStreamReader(new FileInputStream(new File(discoveryDocPath)));
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(reader);
    return Document.from(new DiscoveryNode(root));
  }

  public static Document createDocumentAndLog(String discoveryDocPath, DiagCollector diagCollector) throws IOException {
    // Prevent INFO messages from polluting the log.
    Logger.getLogger("").setLevel(Level.WARNING);

    Document document = null;
    try {
      document = DocumentGenerator.createDocument(discoveryDocPath);
    } catch (FileNotFoundException e) {
      diagCollector
          .addDiag(Diag.error(
              SimpleLocation.TOPLEVEL, "File not found: " + discoveryDocPath));
    } catch (IOException e) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL, "Failed to read Discovery Doc: " + discoveryDocPath));
    }
    return document;
  }
}

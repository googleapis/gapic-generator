/*
 * Copyright (C) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.api.codegen;

import com.google.api.tools.framework.model.BoundedDiagCollector;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.testing.BaselineTestCase;
import com.google.api.tools.framework.model.testing.DiagUtils;
import com.google.api.tools.framework.snippet.Doc;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 * A baseline test case which prepares a model from proto and yaml config and handles printing the
 * result of a test run to the baseline.
 */
public abstract class DiscoConfigBaselineTestCase extends BaselineTestCase {

  @Rule public TemporaryFolder tempDir = new TemporaryFolder();

  protected boolean suppressDiagnosis() {
    return false;
  }

  /**
   * Determine if location information from the {@link Diag} should be printed in the baseline
   * files.
   */
  protected boolean showDiagLocation = true;

  /** List of suppression directives that should be added to the model. */
  protected List<String> suppressionDirectives = Lists.newArrayList("versioning-config");

  /** The model on which the test runs. */
  protected String discoveryFileName;

  /**
   * Run test specific logic. The returned object will be printed to the baseline if not null. The
   * object can be a map from string to object, in which case the map will be decomposed for the
   * baseline output. If a {@link Doc} appears it will be pretty printed before writing it.
   */
  @Nullable
  protected abstract Object run() throws Exception;

  /**
   * Run a test for the given file base name(s). Collects all .proto and .yaml files with the given
   * base name (i.e. baseName.proto or baseName.yaml), constructs model, and calls {@link #run()}.
   * Post that, prints diags and the result of the run to the baseline.
   */
  protected void test() throws Exception {
    DiagCollector diagCollector = new BoundedDiagCollector();

    // Run test specific logic.
    Object result = run();

    if (!diagCollector.hasErrors() && result != null) {
      // Output the result depending on its type.
      if (result instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
          testOutput().printf("============== file: %s ==============%n", entry.getKey());
          testOutput().println(displayValue(entry.getValue()));
        }
      } else {
        testOutput().println(displayValue(result));
      }
    }
  }

  /** Prints diag to the testOutput. */
  protected void printDiag(final Diag diag) {
    String message = DiagUtils.getDiagMessage(diag);
    if (showDiagLocation) {
      testOutput()
          .printf(
              String.format(
                      "%s: %s: %s",
                      diag.getKind().toString(), getLocationWithoutFullPath(diag), message)
                  + "%n");
    } else {
      testOutput().printf("%s: %s%n", diag.getKind(), message);
    }
  }

  private String getLocationWithoutFullPath(final Diag diag) {
    String location = diag.getLocation().getDisplayString();
    int firstSlashIndex = location.indexOf("/");
    int lastSlashIndex = location.lastIndexOf("/");
    if (firstSlashIndex != -1) {
      String toReplace = location.substring(firstSlashIndex, lastSlashIndex + 1);
      location = location.replace(toReplace, "");
    }
    return location;
  }

  /** Fetches content from various values for a content source (File, Doc, etc.) */
  private String displayValue(Object value) throws IOException {
    if (value instanceof Doc) {
      return ((Doc) value).prettyPrint(100);
    } else if (value instanceof File) {
      return Files.asCharSource((File) value, StandardCharsets.UTF_8).read();
    } else {
      return value.toString();
    }
  }
}

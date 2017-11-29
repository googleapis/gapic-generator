/* Copyright 2017 Google LLC
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
package com.google.api.codegen.configgen;

import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Location;
import com.google.api.tools.framework.model.SimpleLocation;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.Node;

/**
 * Convenience methods for common operations used during config yaml parsing and config node
 * merging.
 */
public class ConfigHelper {
  private final DiagCollector diag;
  private final String fileName;

  public ConfigHelper(DiagCollector diag, String fileName) {
    this.diag = diag;
    this.fileName = fileName;
  }

  public int getErrorCount() {
    return diag.getErrorCount();
  }

  public void error(Node node, String message, Object... params) {
    error(node.getStartMark(), message, params);
  }

  public void error(Mark mark, String message, Object... params) {
    error(getStartLine(mark), message, params);
  }

  public void error(int line, String message, Object... params) {
    error(getLocation(line), message, params);
  }

  public void error(String message, Object... params) {
    error(getLocation("?"), message, params);
  }

  public void error(Location location, String message, Object... params) {
    diag.addDiag(Diag.error(location, message, params));
  }

  public int getStartLine(Node node) {
    return getStartLine(node.getStartMark());
  }

  public int getStartLine(Mark mark) {
    return mark.getLine() + 1;
  }

  public Location getLocation(int line) {
    return getLocation(line);
  }

  private Location getLocation(Object line) {
    return new SimpleLocation(String.format("%s:%s", fileName, line), fileName);
  }
}

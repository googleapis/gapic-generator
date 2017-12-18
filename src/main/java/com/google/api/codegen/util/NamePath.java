/* Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.util;

import com.google.common.base.Ascii;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;

/** NamePath represents a fully-qualified name, separated by something like dots or slashes. */
public class NamePath {
  private ArrayList<String> pathPieces;

  /** Create a NamePath that is separated by dots. */
  public static NamePath dotted(String... pieces) {
    return parse("\\.", pieces);
  }

  public static NamePath backslashed(String... pieces) {
    return parse("\\\\", pieces);
  }

  public static NamePath doubleColoned(String... pieces) {
    return parse("::", pieces);
  }

  private static NamePath parse(String separatorRegex, String... pieces) {
    List<String> namePieces = new ArrayList<>();
    for (String piece : pieces) {
      for (String subPiece : piece.split(separatorRegex)) {
        namePieces.add(subPiece);
      }
    }
    if (namePieces.size() == 0) {
      throw new IllegalArgumentException("QualifiedName must not be zero length");
    }
    return new NamePath(namePieces);
  }

  private NamePath(List<String> pathPieces) {
    this.pathPieces = Lists.newArrayList(pathPieces);
  }

  /** Create a new NamePath where the head (the last piece) is replaced with the given newHead. */
  public NamePath withHead(String newHead) {
    List<String> newPathPieces = new ArrayList<>();
    newPathPieces.addAll(pathPieces);
    newPathPieces.set(pathPieces.size() - 1, newHead);
    return new NamePath(newPathPieces);
  }

  /**
   * Create a new NamePath where the head (the last piece) is removed. If there is no last piece
   * this will return a new empty NamePath.
   */
  public NamePath withoutHead() {
    List<String> newPathPieces = new ArrayList<>();
    newPathPieces.addAll(pathPieces);
    if (!newPathPieces.isEmpty()) {
      newPathPieces.remove(newPathPieces.size() - 1);
    }
    return new NamePath(newPathPieces);
  }

  /** Append the given head after the last piece of the path. */
  public NamePath append(String head) {
    pathPieces.add(head);
    return this;
  }

  /** Create a new NamePath where each name piece now starts with an uppercase word. */
  public NamePath withUpperPieces() {
    List<String> newPathPieces = new ArrayList<>();
    for (String piece : pathPieces) {
      String newPiece =
          (piece.isEmpty())
              ? piece
              : new StringBuilder(piece.length())
                  .append(Ascii.toUpperCase(piece.charAt(0)))
                  .append(piece.substring(1))
                  .toString();
      newPathPieces.add(newPiece);
    }
    return new NamePath(newPathPieces);
  }

  /** Returns the head (last piece) of the NamePath. */
  public String getHead() {
    return pathPieces.get(pathPieces.size() - 1);
  }

  /** Returns the namePath in dotted form. */
  public String toDotted() {
    return Joiner.on(".").join(pathPieces);
  }

  /** Returns the namePath in backslashed form. */
  public String toBackslashed() {
    return Joiner.on("\\").join(pathPieces);
  }

  public String toDoubleColoned() {
    return Joiner.on("::").join(pathPieces);
  }

  public String toSlashed() {
    return Joiner.on("/").join(pathPieces);
  }

  public String toDashed() {
    return Joiner.on("-").join(pathPieces);
  }
}

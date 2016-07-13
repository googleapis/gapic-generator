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
package com.google.api.codegen.util;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;

public class QualifiedName {
  private List<String> namePieces;

  public static QualifiedName dotted(String... pieces) {
    List<String> namePieces = new ArrayList<>();
    for (String piece : pieces) {
      for (String subPiece : piece.split("\\.")) {
        namePieces.add(subPiece);
      }
    }
    if (namePieces.size() == 0) {
      throw new IllegalArgumentException("QualifiedName must not be zero length");
    }
    return new QualifiedName(namePieces);
  }

  private QualifiedName(List<String> namePieces) {
    this.namePieces = namePieces;
  }

  public QualifiedName withHead(String newHead) {
    List<String> newNamePieces = new ArrayList<>();
    newNamePieces.addAll(namePieces);
    newNamePieces.set(namePieces.size() - 1, newHead);
    return new QualifiedName(newNamePieces);
  }

  public String getHead() {
    return namePieces.get(namePieces.size() - 1);
  }

  public String toDotted() {
    return Joiner.on(".").join(namePieces);
  }

  public String toBackslashed() {
    return Joiner.on("\\").join(namePieces);
  }
}

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

import com.google.api.client.util.Joiner;
import com.google.common.base.CaseFormat;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an identifier name which is casing-aware.
 */
public class Name {
  private List<NamePiece> namePieces;

  /**
   * Creates a Name from a sequence of lower-underscore strings. If any of the strings contain any characters
   * that are not lower case or underscores, an IllegalArgumentException is thrown.
   */
  public static Name from(String... pieces) {
    List<NamePiece> namePieces = new ArrayList<>();
    for (String piece : pieces) {
      validateLowerUnderscore(piece);
      namePieces.add(new NamePiece(piece, CaseFormat.LOWER_UNDERSCORE));
    }
    return new Name(namePieces);
  }

  public static Name lowerCamel(String identifier) {
    for (Character ch : identifier.toCharArray()) {
      if (!Character.isLowerCase(ch) && !Character.isUpperCase(ch)) {
        throw new IllegalArgumentException(
            "Name: identifier not in lower-underscore: '" + identifier + "'");
      }
    }
    return new Name(identifier, CaseFormat.LOWER_CAMEL);
  }

  public static void validateLowerUnderscore(String identifier) {
    Character underscore = Character.valueOf('_');
    for (Character ch : identifier.toCharArray()) {
      if (!Character.isLowerCase(ch) && !ch.equals(underscore)) {
        throw new IllegalArgumentException(
            "Name: identifier not in lower-underscore: '" + identifier + "'");
      }
    }
  }

  private Name(List<NamePiece> namePieces) {
    this.namePieces = namePieces;
  }

  public Name(String identifier, CaseFormat caseFormat) {
    List<NamePiece> namePieces = new ArrayList<>();
    namePieces.add(new NamePiece(identifier, caseFormat));
    this.namePieces = namePieces;
  }

  private static class NamePiece {
    public String identifier;
    public CaseFormat caseFormat;

    private NamePiece(String identifier, CaseFormat caseFormat) {
      this.identifier = identifier;
      this.caseFormat = caseFormat;
    }
  }

  public String toUpperUnderscore() {
    List<String> newPieces = new ArrayList<>();
    for (NamePiece namePiece : namePieces) {
      newPieces.add(namePiece.caseFormat.to(CaseFormat.UPPER_UNDERSCORE, namePiece.identifier));
    }
    return Joiner.on('_').join(newPieces);
  }

  public String toLowerCamel() {
    StringBuffer buffer = new StringBuffer();
    boolean firstPiece = true;
    for (NamePiece namePiece : namePieces) {
      if (firstPiece) {
        buffer.append(namePiece.caseFormat.to(CaseFormat.LOWER_CAMEL, namePiece.identifier));
        firstPiece = false;
      } else {
        buffer.append(namePiece.caseFormat.to(CaseFormat.UPPER_CAMEL, namePiece.identifier));
      }
    }
    return buffer.toString();
  }

  public Name append(String identifier) {
    validateLowerUnderscore(identifier);
    namePieces.add(new NamePiece(identifier, CaseFormat.LOWER_UNDERSCORE));
    return this;
  }
}

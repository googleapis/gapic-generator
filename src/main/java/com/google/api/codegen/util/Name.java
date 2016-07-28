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
 * Name represents an identifier name which is casing-aware.
 */
public class Name {
  private List<NamePiece> namePieces;

  /**
   * Creates a Name from a sequence of lower-underscore strings.
   *
   * @throws IllegalArgumentException if any of the strings contain any characters that are not
   * lower case or underscores.
   */
  public static Name from(String... pieces) {
    List<NamePiece> namePieces = new ArrayList<>();
    for (String piece : pieces) {
      validateLowerUnderscore(piece);
      namePieces.add(new NamePiece(piece, CaseFormat.LOWER_UNDERSCORE));
    }
    return new Name(namePieces);
  }

  /**
   * Creates a Name from a sequence of lower-camel strings.
   *
   * @throws IllegalArgumentException if any of the strings do not follow the lower-camel format.
   */
  public static Name lowerCamel(String... pieces) {
    List<NamePiece> namePieces = new ArrayList<>();
    for (String piece : pieces) {
      validateCamel(piece, false);
      namePieces.add(new NamePiece(piece, CaseFormat.LOWER_CAMEL));
    }
    return new Name(namePieces);
  }

  /**
   * Creates a Name from a sequence of upper-camel strings.
   *
   * @throws IllegalArgumentException if any of the strings do not follow the upper-camel format.
   */
  public static Name upperCamel(String... pieces) {
    List<NamePiece> namePieces = new ArrayList<>();
    for (String piece : pieces) {
      validateCamel(piece, true);
      namePieces.add(new NamePiece(piece, CaseFormat.UPPER_CAMEL));
    }
    return new Name(namePieces);
  }

  private static void validateLowerUnderscore(String identifier) {
    if (!isLowerUnderscore(identifier)) {
      throw new IllegalArgumentException(
          "Name: identifier not in lower-underscore: '" + identifier + "'");
    }
  }

  private static boolean isLowerUnderscore(String identifier) {
    Character underscore = Character.valueOf('_');
    for (Character ch : identifier.toCharArray()) {
      if (!Character.isLowerCase(ch) && !ch.equals(underscore) && !Character.isDigit(ch)) {
        return false;
      }
    }
    return true;
  }

  private static void validateCamel(String identifier, boolean upper) {
    if (!isCamel(identifier, upper)) {
      String casingDescription = "lower camel";
      if (upper) {
        casingDescription = "upper camel";
      }
      throw new IllegalArgumentException(
          "Name: identifier not in " + casingDescription + ": '" + identifier + "'");
    }
  }

  private static boolean isCamel(String identifier, boolean upper) {
    if (identifier.length() == 0) {
      return true;
    }
    if (upper && !Character.isUpperCase(identifier.charAt(0))) {
      return false;
    }
    if (!upper && !Character.isLowerCase(identifier.charAt(0))) {
      return false;
    }
    for (Character ch : identifier.toCharArray()) {
      if (!Character.isLowerCase(ch) && !Character.isUpperCase(ch)) {
        return false;
      }
    }
    return true;
  }

  private Name(List<NamePiece> namePieces) {
    this.namePieces = namePieces;
  }

  /**
   * Returns the identifier in upper-underscore format.
   */
  public String toUpperUnderscore() {
    return toUnderscore(CaseFormat.UPPER_UNDERSCORE);
  }

  /**
   * Returns the identifier in lower-underscore format.
   */
  public String toLowerUnderscore() {
    return toUnderscore(CaseFormat.LOWER_UNDERSCORE);
  }

  private String toUnderscore(CaseFormat caseFormat) {
    List<String> newPieces = new ArrayList<>();
    for (NamePiece namePiece : namePieces) {
      newPieces.add(namePiece.caseFormat.to(caseFormat, namePiece.identifier));
    }
    return Joiner.on('_').join(newPieces);
  }

  /**
   * Returns the identifier in lower-camel format.
   */
  public String toLowerCamel() {
    return toCamel(CaseFormat.LOWER_CAMEL);
  }

  /**
   * Returns the identifier in upper-camel format.
   */
  public String toUpperCamel() {
    return toCamel(CaseFormat.UPPER_CAMEL);
  }

  private String toCamel(CaseFormat caseFormat) {
    StringBuffer buffer = new StringBuffer();
    boolean firstPiece = true;
    for (NamePiece namePiece : namePieces) {
      if (firstPiece && caseFormat.equals(CaseFormat.LOWER_CAMEL)) {
        buffer.append(namePiece.caseFormat.to(CaseFormat.LOWER_CAMEL, namePiece.identifier));
      } else {
        buffer.append(namePiece.caseFormat.to(CaseFormat.UPPER_CAMEL, namePiece.identifier));
      }
      firstPiece = false;
    }
    return buffer.toString();
  }

  /**
   * Returns a new Name containing the pieces from this Name plus the given
   * identifier added on the end.
   */
  public Name join(String identifier) {
    validateLowerUnderscore(identifier);
    List<NamePiece> newPieceList = new ArrayList<>();
    newPieceList.addAll(namePieces);
    newPieceList.add(new NamePiece(identifier, CaseFormat.LOWER_UNDERSCORE));
    return new Name(newPieceList);
  }

  /**
   * Returns a new Name containing the pieces from this Name plus the pieces of the given
   * name added on the end.
   */
  public Name join(Name rhs) {
    List<NamePiece> newPieceList = new ArrayList<>();
    newPieceList.addAll(namePieces);
    newPieceList.addAll(rhs.namePieces);
    return new Name(newPieceList);
  }

  private static class NamePiece {
    public final String identifier;
    public final CaseFormat caseFormat;

    private NamePiece(String identifier, CaseFormat caseFormat) {
      this.identifier = identifier;
      this.caseFormat = caseFormat;
    }
  }
}

/* Copyright 2016 Google LLC
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

import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class to replace fully capitalized common acronyms with an upper camel interpretation.
 */
public class CommonAcronyms {
  private static final ImmutableSet<String> ACRONYMS =
      ImmutableSet.<String>builder()
          .add("IAM")
          .add("HTTP")
          .add("XML")
          .add("API")
          .add("SQL")
          .build();

  /** Represents the notion of whether a name piece is normal or an upper-case acronym. */
  public enum NamePieceCasingType {
    NORMAL,
    UPPER_ACRONYM;
  }

  /** Represents a name piece plus its NamePieceCasingType. */
  public static class SubNamePiece {
    private final String namePieceString;
    private final NamePieceCasingType type;

    public SubNamePiece(String namePieceString, NamePieceCasingType type) {
      this.namePieceString = namePieceString;
      this.type = type;
    }

    public String namePieceString() {
      return namePieceString;
    }

    public NamePieceCasingType type() {
      return type;
    }
  }

  public static String camelizeUpperAcronyms(String str) {
    StringBuilder builder = new StringBuilder();
    for (SubNamePiece piece : splitByUpperAcronyms(str)) {
      if (piece.type().equals(NamePieceCasingType.UPPER_ACRONYM)) {
        builder.append(
            CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, piece.namePieceString()));
      } else {
        builder.append(piece.namePieceString());
      }
    }
    return builder.toString();
  }

  public static List<SubNamePiece> splitByUpperAcronyms(String str) {
    List<NamePiecePosition> positions = getNamePiecePositions(str);
    if (positions.size() == 0) {
      return Collections.singletonList(newNormalPiece(str));
    } else {
      return getSubNamePieces(str, positions);
    }
  }

  private static List<NamePiecePosition> getNamePiecePositions(String str) {
    List<NamePiecePosition> positions = new ArrayList<>();
    for (String acronym : ACRONYMS) {
      int startIndex = 0;
      int foundIndex = -1;
      while ((foundIndex = str.indexOf(acronym, startIndex)) != -1) {
        int endIndex = foundIndex + acronym.length();
        positions.add(new NamePiecePosition(foundIndex, endIndex));
        startIndex = endIndex;
      }
    }
    Collections.sort(positions);

    return positions;
  }

  private static List<SubNamePiece> getSubNamePieces(
      String str, List<NamePiecePosition> positions) {
    Preconditions.checkArgument(positions.size() > 0);

    List<SubNamePiece> result = new ArrayList<>();
    NamePiecePosition lastPos = null;
    for (NamePiecePosition namePiecePos : positions) {
      if (lastPos != null) {
        if (lastPos.overlapsWith(namePiecePos)) {
          throw new IllegalArgumentException(
              "CommonAcronyms: A situation where combined acronyms was found. Acronym splitting "
                  + "is ambiguous. ex. \"APIAMName\".");
        }
      }

      // Add any component of the string that is not part of a previous acronym
      int lastEndIndex = 0;
      if (lastPos != null) {
        lastEndIndex = lastPos.endIndex;
      }
      if (namePiecePos.startIndex > lastEndIndex) {
        String namePiece = str.substring(lastEndIndex, namePiecePos.startIndex);
        result.add(newNormalPiece(namePiece));
      }

      // add the current acronym
      String namePiece = str.substring(namePiecePos.startIndex, namePiecePos.endIndex);
      result.add(new SubNamePiece(namePiece, NamePieceCasingType.UPPER_ACRONYM));

      lastPos = namePiecePos;
    }

    // Add any training component of the string that is not part of a previous acronym
    if (lastPos.endIndex < str.length()) {
      String namePiece = str.substring(lastPos.endIndex, str.length());
      result.add(newNormalPiece(namePiece));
    }

    return result;
  }

  private static SubNamePiece newNormalPiece(String namePiece) {
    return new SubNamePiece(namePiece, NamePieceCasingType.NORMAL);
  }

  private static class NamePiecePosition implements Comparable<NamePiecePosition> {
    public final int startIndex;
    // one past the end of the string
    public final int endIndex;

    public NamePiecePosition(int startIndex, int endIndex) {
      this.startIndex = startIndex;
      this.endIndex = endIndex;
      if (startIndex == endIndex) {
        throw new IllegalArgumentException(
            "Invalid name piece position - zero length: startIndex = "
                + startIndex
                + ", endIndex = "
                + endIndex);
      }
    }

    @Override
    public int compareTo(NamePiecePosition otherPos) {
      int startComp = Integer.compare(startIndex, otherPos.startIndex);
      if (startComp != 0) {
        return startComp;
      }
      return Integer.compare(endIndex, otherPos.endIndex);
    }

    public boolean overlapsWith(NamePiecePosition otherPos) {
      if (otherPos.startIndex < endIndex && otherPos.endIndex > startIndex) {
        return true;
      } else {
        return false;
      }
    }
  }
}

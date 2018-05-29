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

import java.util.regex.Pattern;

/**
 * Collects common regular expressions for formatting source comments to follow language-idiomatic
 * style.
 */
public final class CommentPatterns {
  public static final Pattern BACK_QUOTE_PATTERN = Pattern.compile("(?<!`)``?(?!`)");
  public static final Pattern ABSOLUTE_LINK_PATTERN =
      Pattern.compile("\\[([^\\]]+)\\]\\((\\p{Alpha}+:[^\\)]+)\\)");
  public static final Pattern RELATIVE_LINK_PATTERN =
      Pattern.compile("\\[([^\\]]+)\\]\\(((?!\\p{Alpha}+:)[^\\)]+)\\)");
  public static final Pattern PROTO_LINK_PATTERN =
      Pattern.compile("\\[([^\\]]+)\\]\\[([A-Za-z_][A-Za-z_.0-9]*)?\\]");
  public static final Pattern HEADLINE_PATTERN = Pattern.compile("^#+", Pattern.MULTILINE);
  public static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("\\s{4}.*");
  public static final Pattern UNORDERED_LIST_PATTERN = Pattern.compile("^\\s*[\\*\\+\\-]\\s+");
  public static final Pattern INDENT_PATTERN = Pattern.compile("^\\s*");
}

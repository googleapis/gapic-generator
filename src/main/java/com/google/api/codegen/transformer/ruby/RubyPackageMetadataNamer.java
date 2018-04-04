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
package com.google.api.codegen.transformer.ruby;

import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.transformer.PackageMetadataNamer;
import com.google.api.codegen.util.Name;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A RubyPackageMetadataNamer provides ruby specific names for metadata views. */
public class RubyPackageMetadataNamer extends PackageMetadataNamer {
  private final char METADATA_IDENTIFIER_SEPARATOR = '-';

  private String packageName;

  public RubyPackageMetadataNamer(String packageName) {
    this.packageName = packageName;
  }

  private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("(.+?)::[vV][0-9]+(::.*)?");

  @Override
  public String getMetadataIdentifier() {
    // strip out the string before the first v0 part of the path
    Matcher m = IDENTIFIER_PATTERN.matcher(packageName);
    List<String> names = Splitter.on("::").splitToList(m.matches() ? m.group(1) : packageName);

    // drop last if not a versioned id
    if (!m.matches() && names.size() > 0) {
      names = names.subList(0, names.size() - 1);
    }

    // convert case and replace :: with -
    return names
        .stream()
        .map(x -> Name.upperCamel(x).toLowerUnderscore())
        .reduce("", (x, y) -> x.length() > 0 ? x + METADATA_IDENTIFIER_SEPARATOR + y : y);
  }

  @Override
  public String getOutputFileName() {
    return getMetadataIdentifier() + ".gemspec";
  }

  public String getProjectVariable(boolean test) {
    return Name.from(getSimpleMetadataIdentifier(), test ? "test" : "", "project")
        .toUpperUnderscore();
  }

  public String getKeyfileVariable(boolean test) {
    return Name.from(getSimpleMetadataIdentifier(), test ? "test" : "", "keyfile")
        .toUpperUnderscore();
  }

  public String getJsonKeyVariable(boolean test) {
    return Name.from(getSimpleMetadataIdentifier(), test ? "test" : "", "keyfile", "json")
        .toUpperUnderscore();
  }

  private String getSimpleMetadataIdentifier() {
    return Iterables.getLast(
        Splitter.on(METADATA_IDENTIFIER_SEPARATOR).split(getMetadataIdentifier()));
  }

  @Override
  public String getReleaseAnnotation(ReleaseLevel releaseLevel) {
    switch (releaseLevel) {
      case GA:
        return "GA";
      default:
        return super.getReleaseAnnotation(releaseLevel);
    }
  }
}

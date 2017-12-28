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

import com.google.api.codegen.transformer.PackageMetadataNamer;
import com.google.api.codegen.util.Name;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.List;

/** A RubyPackageMetadataNamer provides ruby specific names for metadata views. */
public class RubyPackageMetadataNamer extends PackageMetadataNamer {
  private final char METADATA_IDENTIFIER_SEPARATOR = '-';

  private String packageName;

  public RubyPackageMetadataNamer(String packageName) {
    this.packageName = packageName;
  }

  @Override
  public String getMetadataIdentifier() {
    List<String> names = Splitter.on("::").splitToList(packageName);
    if (names.size() > 1) {
      names = names.subList(0, names.size() - 1);
    }
    ImmutableList.Builder<String> lowerNames = ImmutableList.builder();
    for (String name : names) {
      lowerNames.add(Name.upperCamel(name).toLowerUnderscore());
    }
    return Joiner.on(METADATA_IDENTIFIER_SEPARATOR).join(lowerNames.build());
  }

  @Override
  public String getOutputFileName() {
    return getMetadataIdentifier() + ".gemspec";
  }

  public String getSmokeTestProjectVariable() {
    return Name.from(getSimpleMetadataIdentifier(), "test", "project").toUpperUnderscore();
  }

  private String getSimpleMetadataIdentifier() {
    return Iterables.getLast(
        Splitter.on(METADATA_IDENTIFIER_SEPARATOR).split(getMetadataIdentifier()));
  }
}

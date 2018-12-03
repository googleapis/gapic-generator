/* Copyright 2017 Google LLC
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
package com.google.api.codegen.configgen.transformer;

import static com.google.api.codegen.configgen.mergers.LanguageSettingsMerger.LANGUAGE_FORMATTERS;

import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.configgen.mergers.LanguageSettingsMerger.LanguageFormatter;
import com.google.api.codegen.configgen.viewmodel.LanguageSettingView;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;

/** Generates language setting view objects using a package name. */
public class LanguageTransformer {

  List<LanguageSettingView> generateLanguageSettings(String packageName) {
    ImmutableList.Builder<LanguageSettingView> languageSettings = ImmutableList.builder();
    for (Map.Entry<TargetLanguage, LanguageFormatter> entry : LANGUAGE_FORMATTERS.entrySet()) {
      LanguageFormatter languageFormatter = entry.getValue();
      languageSettings.add(
          LanguageSettingView.newBuilder()
              .language(entry.getKey().name().toLowerCase())
              .packageName(languageFormatter.getFormattedPackageName(packageName))
              .build());
    }
    return languageSettings.build();
  }
}

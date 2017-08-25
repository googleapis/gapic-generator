/* Copyright 2017 Google Inc
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
package com.google.api.codegen.advising;

import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.LanguageSettingsProto;
import com.google.api.codegen.configgen.transformer.LanguageTransformer;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;

public class LanguageSettingsRule implements AdviserRule {
  @Override
  public String getName() {
    return "language-settings";
  }

  @Override
  public List<String> collectAdvice(Model model, ConfigProto configProto) {
    String packageName = getPackageName(model);
    ImmutableList.Builder<String> messages = ImmutableList.builder();
    for (Map.Entry<String, LanguageTransformer.LanguageFormatter> entry :
        LanguageTransformer.LANGUAGE_FORMATTERS.entrySet()) {
      String language = entry.getKey();
      LanguageSettingsProto languageSettingsProto =
          configProto.getLanguageSettingsOrDefault(language, null);
      if (languageSettingsProto == null) {
        String formattedPackageName = entry.getValue().getFormattedPackageName(packageName);
        messages.add(
            String.format(
                "Missing language_settings for key %s.%n%n"
                    + "language_settings:%n"
                    + "  # ...%n"
                    + "  %s:%n"
                    + "    package_name: %s%n%n",
                language, language, formattedPackageName));
      } else if (languageSettingsProto.getPackageName().isEmpty()) {
        String formattedPackageName = entry.getValue().getFormattedPackageName(packageName);
        messages.add(
            String.format(
                "Missing package_name in language_settings of key %s.%n%n"
                    + "%s:%n"
                    + "  # ...%n"
                    + "  package_name: %s%n%n",
                language, language, formattedPackageName));
      }
    }
    return messages.build();
  }

  private String getPackageName(Model model) {
    String apiName = model.getServiceConfig().getApis(0).getName();
    Interface apiInterface = model.getSymbolTable().lookupInterface(apiName);
    return apiInterface.getFile().getFullName();
  }
}

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
package com.google.api.codegen.advising;

import com.google.api.codegen.ConfigProto;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.Location;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;

public class Adviser {
  private static final ImmutableList<AdviserRule> DEFAULT_RULES =
      ImmutableList.<AdviserRule>of(new LanguageSettingsRule());

  private static final Location GAPIC_CONFIG_LOCATION = new SimpleLocation("gapic config");

  private final List<AdviserRule> rules;

  private final Collection<String> suppressors;

  public Adviser(Collection<String> suppressors) {
    this(DEFAULT_RULES, suppressors);
  }

  public Adviser(List<AdviserRule> rules, Collection<String> suppressors) {
    this.rules = rules;
    this.suppressors = suppressors;
  }

  public void advise(Model model, ConfigProto configProto) {
    for (AdviserRule rule : rules) {
      if (suppressors.contains(rule.getName())) {
        continue;
      }

      for (String message : rule.collectAdvice(model, configProto)) {
        model
            .getDiagReporter().getDiagCollector()
            .addDiag(Diag.warning(GAPIC_CONFIG_LOCATION, "(%s) %s", rule.getName(), message));
      }
    }
  }
}

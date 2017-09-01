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
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.Location;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Gives advice to the user as a console warning based on specified AdviserRules.
 *
 * <p>Advice can be suppressed through suppressors in the format
 * rule-name[@element-name[|element-name...]] where rule-name is the name of an AdviserRule and
 * element-name is the full name of a ProtoElement in the service protos. For example, "method"
 * suppresses all MethodRules, while "method@example.FooService" suppresses only the MethodRules on
 * the example.FooService methods.
 */
public class Adviser {
  private static final ImmutableList<AdviserRule> DEFAULT_RULES =
      ImmutableList.<AdviserRule>of(new LanguageSettingsRule(), new InterfaceRule());

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
    Map<String, List<String>> suppressorMap = generateSuppressorMap();
    for (AdviserRule rule : rules) {
      List<String> suppressedElements = suppressorMap.get(rule.getName());
      if (suppressedElements == null) {
        suppressedElements = ImmutableList.<String>of();
      } else if (suppressedElements.isEmpty()) {
        continue;
      }

      for (String message : rule.collectAdvice(model, configProto, suppressedElements)) {
        model
            .getDiagCollector()
            .addDiag(Diag.warning(GAPIC_CONFIG_LOCATION, "(%s) %s", rule.getName(), message));
      }
    }
  }

  private Map<String, List<String>> generateSuppressorMap() {
    ImmutableMap.Builder<String, List<String>> suppressorMap = ImmutableMap.builder();
    for (String suppressor : suppressors) {
      int delimeterIndex = suppressor.indexOf("@");
      if (delimeterIndex > 0) {
        String ruleName = suppressor.substring(0, delimeterIndex);
        String elementsStr = suppressor.substring(delimeterIndex + 1);
        List<String> elements =
            Splitter.on('|').trimResults().omitEmptyStrings().splitToList(elementsStr);
        suppressorMap.put(ruleName, elements);
      } else if (delimeterIndex < 0) {
        suppressorMap.put(suppressor, ImmutableList.<String>of());
      }
    }
    return suppressorMap.build();
  }
}

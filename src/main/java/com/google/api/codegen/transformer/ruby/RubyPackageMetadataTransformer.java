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
package com.google.api.codegen.transformer.ruby;

import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.PackageMetadataTransformer;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** Responsible for producing package metadata related views for Ruby */
public class RubyPackageMetadataTransformer implements ModelToViewTransformer {
  private static final String GEMSPEC_FILE = "ruby/gemspec.snip";
  private static final List<String> TOP_LEVEL_FILES =
      ImmutableList.of("ruby/Gemfile.snip", "ruby/Rakefile.snip", "ruby/README.md.snip");
  PackageMetadataConfig packageConfig;
  PackageMetadataTransformer metadataTransformer = new PackageMetadataTransformer();

  private static final String RUBY_PREFIX = "ruby/";

  public RubyPackageMetadataTransformer(PackageMetadataConfig packageConfig) {
    this.packageConfig = packageConfig;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.<String>builder().add(GEMSPEC_FILE).addAll(TOP_LEVEL_FILES).build();
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    RubyPackageMetadataNamer namer = new RubyPackageMetadataNamer(apiConfig.getPackageName());
    return ImmutableList.<ViewModel>builder()
        .add(generateGemspecView(model, namer))
        .addAll(generateMetadataViews(model, namer))
        .build();
  }

  private ViewModel generateGemspecView(Model model, RubyPackageMetadataNamer namer) {
    return metadataTransformer
        .generateMetadataView(
            packageConfig, model, GEMSPEC_FILE, namer.getOutputFileName(), TargetLanguage.RUBY)
        .identifier(namer.getMetadataIdentifier())
        .build();
  }

  private List<ViewModel> generateMetadataViews(Model model, RubyPackageMetadataNamer namer) {
    ImmutableList.Builder<ViewModel> views = ImmutableList.builder();
    for (String template : TOP_LEVEL_FILES) {
      views.add(generateMetadataView(model, template, namer));
    }
    return views.build();
  }

  private ViewModel generateMetadataView(
      Model model, String template, RubyPackageMetadataNamer namer) {
    String noLeadingRubyDir =
        template.startsWith(RUBY_PREFIX) ? template.substring(RUBY_PREFIX.length()) : template;
    int extensionIndex = noLeadingRubyDir.lastIndexOf(".");
    String outputPath = noLeadingRubyDir.substring(0, extensionIndex);
    return metadataTransformer
        .generateMetadataView(packageConfig, model, template, outputPath, TargetLanguage.RUBY)
        .identifier(namer.getMetadataIdentifier())
        .build();
  }
}

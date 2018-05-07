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
package com.google.api.codegen.gapic;

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.tools.framework.model.Model;
import java.util.List;

/** A factory for GapicProviders which perform code generation. */
public interface GapicProviderFactory {
  /**
   * Create the provider from the given model, configs and the output path.
   *
   * <p>The outputPath is used for copying static files from resources into the output directory.
   */
  List<GapicProvider<?>> create(
      Model model,
      GapicProductConfig productConfig,
      GapicGeneratorConfig generatorConfig,
      PackageMetadataConfig packageConfig);
}

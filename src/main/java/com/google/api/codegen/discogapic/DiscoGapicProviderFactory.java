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
package com.google.api.codegen.discogapic;

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.gapic.GapicGeneratorConfig;
import java.util.List;

public interface DiscoGapicProviderFactory {
  List<DiscoGapicProvider> create(
      Document document,
      GapicProductConfig productConfig,
      GapicGeneratorConfig generatorConfig,
      PackageMetadataConfig packageConfig);
}

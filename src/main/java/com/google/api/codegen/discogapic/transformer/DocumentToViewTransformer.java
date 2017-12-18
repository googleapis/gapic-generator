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
package com.google.api.codegen.discogapic.transformer;

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.viewmodel.ViewModel;
import java.util.List;

public interface DocumentToViewTransformer {

  /** Generate a list of ViewModels from a given Document model. */
  List<ViewModel> transform(Document document, GapicProductConfig productConfig);

  /** Return the list of filenames of View templates to be applied to the transformed ViewModels. */
  List<String> getTemplateFileNames();
}

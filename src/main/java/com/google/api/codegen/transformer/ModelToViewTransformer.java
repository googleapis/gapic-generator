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
package com.google.api.codegen.transformer;

import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.viewmodel.ViewModel;
import java.util.List;

/**
 * A ModelToViewTransformer transforms an ApiModel into a list of ViewModel instances that can be
 * rendered by a template engine.
 */
public interface ModelToViewTransformer<ApiModelT extends ApiModel> {

  /** Generate a list of ViewModels from a given ApiModel. */
  List<ViewModel> transform(ApiModelT model, GapicProductConfig productConfig);

  /** The list of template filenames the ViewModels apply to. */
  List<String> getTemplateFileNames();
}

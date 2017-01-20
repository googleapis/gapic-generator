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
package com.google.api.codegen.transformer.py;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

public class PythonGapicSurfaceTransformer implements ModelToViewTransformer {

  @Override
  public List<? extends ViewModel> transform(Model model, ApiConfig apiConfig) {
    // A ViewModel for each service.
    List<ViewModel> viewModels = new ArrayList<>();
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      viewModels.add(new ServiceViewModel(service, apiConfig));
    }
    // And one for the enums.py file.
    viewModels.add(new EnumViewModel(model, apiConfig));
    return viewModels;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.of(ServiceViewModel.SNIPPET_FILE_NAME, EnumViewModel.SNIPPET_FILE_NAME);
  }
}

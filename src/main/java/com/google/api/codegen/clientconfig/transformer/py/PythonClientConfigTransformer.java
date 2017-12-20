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
package com.google.api.codegen.clientconfig.transformer.py;

import com.google.api.codegen.clientconfig.transformer.ClientConfigTransformer;
import com.google.api.codegen.clientconfig.transformer.CommonMethodTransformer;
import com.google.api.codegen.clientconfig.viewmodel.ClientConfigView;
import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.List;

/** The ModelToViewTransformer to transform a Model into the client config for Python. */
public class PythonClientConfigTransformer implements ModelToViewTransformer {
  private final GapicCodePathMapper pathMapper;

  private final ClientConfigTransformer clientConfigTransformer;

  public PythonClientConfigTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
    this.clientConfigTransformer = new ClientConfigTransformer(new CommonMethodTransformer());
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.of(ClientConfigTransformer.TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    ApiModel apiModel = new ProtoApiModel(model);
    ImmutableList.Builder<ViewModel> views = ImmutableList.builder();
    for (InterfaceModel apiInterface : apiModel.getInterfaces()) {
      InterfaceConfig interfaceConfig = productConfig.getInterfaceConfig(apiInterface);
      ClientConfigView.Builder view = clientConfigTransformer.generateClientConfig(interfaceConfig);
      String subPath = pathMapper.getOutputPath(apiInterface.getFullName(), productConfig);
      String fileName =
          Name.upperCamel(apiInterface.getSimpleName()).join("client_config").toLowerUnderscore()
              + ".py";
      view.outputPath(subPath.isEmpty() ? fileName : subPath + File.separator + fileName);
      view.hasVariable(true);
      views.add(view.build());
    }
    return views.build();
  }
}

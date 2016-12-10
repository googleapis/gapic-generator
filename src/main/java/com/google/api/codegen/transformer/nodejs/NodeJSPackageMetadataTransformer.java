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
package com.google.api.codegen.transformer.nodejs;

import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.viewmodel.PackageMetadataView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Model;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Responsible for producing package metadata related views for NodeJS */
public class NodeJSPackageMetadataTransformer implements ModelToViewTransformer {
  private static final String PACKAGE_FILE = "nodejs/package.snip";

  // TODO(shinfan): Retrieve the following values from static file
  private static final String PACKAGE_VERSION = "0.7.1";
  private static final String GAX_VERSION = "^0.7.0";
  private static final String PROTO_VERSION = "^0.8.3";
  private static final String PACKAGE_URL = "https://github.com/googleapis/googleapis";

  @Override
  public List<String> getTemplateFileNames() {
    return Collections.singletonList(PACKAGE_FILE);
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    List<ViewModel> models = new ArrayList<ViewModel>();
    NodeJSPackageNamer namer = new NodeJSPackageNamer(apiConfig.getPackageName());
    models.add(generateMetadataView(namer));
    return models;
  }

  private ViewModel generateMetadataView(NodeJSPackageNamer namer) {
    return PackageMetadataView.newBuilder()
        .templateFileName(PACKAGE_FILE)
        .outputPath("package.json")
        .identifier(namer.getMetadataIdentifier())
        .version(PACKAGE_VERSION)
        .gaxVersion(GAX_VERSION)
        .protoVersion(PROTO_VERSION)
        .url(PACKAGE_URL)
        .serviceName(namer.getMetadataName())
        .build();
  }
}

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
package com.google.api.codegen.transformer.php;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.PackageMetadataNamer;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.metadata.PackageMetadataView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Responsible for producing package metadata related views for PHP */
public class PhpPackageMetadataTransformer implements ModelToViewTransformer {
  private static final String PACKAGE_FILE = "php/composer.snip";

  // TODO: Retrieve the following values from static file
  // Github issue: https://github.com/googleapis/toolkit/issues/848
  private static final String PACKAGE_VERSION = "0.1.0";
  private static final String GAX_VERSION = "0.6.*";
  private static final String PROTO_VERSION = "0.7.*";
  private static final String PACKAGE_URL = "https://github.com/googleapis/googleapis";

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(PACKAGE_FILE);
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    Iterable<Interface> services = new InterfaceView().getElementIterable(model);
    boolean hasMultipleServices = Iterables.size(services) > 1;
    List<ViewModel> models = new ArrayList<ViewModel>();
    PhpPackageMetadataNamer namer =
        new PhpPackageMetadataNamer(apiConfig.getPackageName(), apiConfig.getDomainLayerLocation());
    models.add(generateMetadataView(namer, hasMultipleServices));
    return models;
  }

  private ViewModel generateMetadataView(PackageMetadataNamer namer, boolean hasMultipleServices) {
    return PackageMetadataView.newBuilder()
        .templateFileName(PACKAGE_FILE)
        .outputPath("composer.json")
        .identifier(namer.getMetadataIdentifier())
        .version(PACKAGE_VERSION)
        .gaxVersion(GAX_VERSION)
        .protoVersion(PROTO_VERSION)
        .url(PACKAGE_URL)
        .serviceName(namer.getMetadataName())
        .hasMultipleServices(hasMultipleServices)
        .build();
  }
}

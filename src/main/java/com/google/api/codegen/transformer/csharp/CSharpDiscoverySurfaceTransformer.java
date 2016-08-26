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
package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.discovery.DiscoveryCodePathMapper;
import com.google.api.codegen.transformer.MethodToViewTransformer;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.protobuf.Method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The ModelToViewTransformer to transform a Model into the standard discovery
 * surface in C#.
 */
public class CSharpDiscoverySurfaceTransformer implements MethodToViewTransformer {

  private static final String SAMPLE_TEMPLATE_FILENAME = "csharp/sample.snip";

  public CSharpDiscoverySurfaceTransformer(DiscoveryCodePathMapper pathMapper) {}

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(SAMPLE_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Method method, ApiaryConfig apiaryConfig) {
    return new ArrayList<ViewModel>();
  }
}

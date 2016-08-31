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
package com.google.api.codegen.discovery.transformer;

import java.util.ArrayList;
import java.util.List;

import com.google.api.codegen.discovery.FieldInfo;
import com.google.api.codegen.discovery.viewmodel.SampleParamView;

public class SampleParamTransformer {

  public static List<SampleParamView> generateSampleParams(SampleTransformerContext context) {
    ArrayList<SampleParamView> sampleParams = new ArrayList<>();
    for (FieldInfo field : context.getSampleConfig().methodInfo().fields()) {
      sampleParams.add(SampleParamView.newBuilder().name(field.name()).doc(field.doc()).build());
    }
    return sampleParams;
  }
}

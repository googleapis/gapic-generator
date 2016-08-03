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
package com.google.api.codegen.transformer;

import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.ServiceDocView;

import java.util.List;

public class ServiceTransformer {

  public ServiceDocView generateServiceDoc(
      SurfaceTransformerContext context, ApiMethodView exampleApiMethod) {
    SurfaceNamer namer = context.getNamer();
    ServiceDocView.Builder serviceDoc = ServiceDocView.newBuilder();
    List<String> docLines = context.getNamer().getDocLines(context.getInterface());
    serviceDoc.firstLine(docLines.get(0));
    serviceDoc.remainingLines(docLines.subList(1, docLines.size()));
    serviceDoc.exampleApiMethod(exampleApiMethod);
    serviceDoc.apiVarName(namer.getApiWrapperVariableName(context.getInterface()));
    serviceDoc.apiClassName(namer.getApiWrapperClassName(context.getInterface()));
    serviceDoc.settingsVarName(namer.getApiSettingsVariableName(context.getInterface()));
    serviceDoc.settingsClassName(namer.getApiSettingsClassName(context.getInterface()));
    return serviceDoc.build();
  }
}

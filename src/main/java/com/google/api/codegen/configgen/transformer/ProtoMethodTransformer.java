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
package com.google.api.codegen.configgen.transformer;

import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.configgen.PagingParameters;
import com.google.api.codegen.configgen.viewmodel.PageStreamingResponseView;
import javax.annotation.Nullable;

/** Protobuf-model-specific functions for transforming method models into views for configgen. */
public class ProtoMethodTransformer extends MethodTransformer {

  public ProtoMethodTransformer(PagingParameters pagingParameters) {
    super(pagingParameters);
  }

  @Nullable
  @Override
  ResourceNameTreatment getResourceNameTreatment(MethodModel methodModel) {
    return null;
  }

  @Nullable
  @Override
  PageStreamingResponseView generatePageStreamingResponse(
      PagingParameters pagingParameters, MethodModel method) {
    boolean hasTokenField = false;
    String resourcesField = null;
    for (FieldModel field : method.getOutputFields()) {
      String fieldName = field.getSimpleName();
      if (fieldName.equals(pagingParameters.getNameForNextPageToken())) {
        hasTokenField = true;
      } else if (field.isRepeated()) {
        if (resourcesField == null) {
          resourcesField = fieldName;
        } else {
          // TODO(shinfan): Add a warning system that is used when heuristic decision cannot be made.
          System.err.printf(
              "Warning: Page Streaming resource field could not be heuristically"
                  + " determined for method %s\n",
              method.getSimpleName());
          break;
        }
      }
    }

    if (!hasTokenField || resourcesField == null) {
      return null;
    }

    return PageStreamingResponseView.newBuilder()
        .tokenField(pagingParameters.getNameForNextPageToken())
        .resourcesField(resourcesField)
        .build();
  }
}

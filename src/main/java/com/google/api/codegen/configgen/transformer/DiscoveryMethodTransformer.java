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
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.util.Name;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/** Discovery-doc-specific functions for transforming method models into views for configgen. */
public class DiscoveryMethodTransformer extends MethodTransformer {

  public DiscoveryMethodTransformer(PagingParameters pagingParameters) {
    super(pagingParameters);
  }

  @Override
  ResourceNameTreatment getResourceNameTreatment(MethodModel methodModel) {
    return ResourceNameTreatment.STATIC_TYPES;
  }

  /**
   * Returns a non-null PageStreamingResponseView iff the method has a response object that contains
   * a nextPageToken child property.
   */
  @Nullable
  @Override
  PageStreamingResponseView generatePageStreamingResponse(
      PagingParameters pagingParameters, MethodModel method) {
    String resourcesField = null;
    boolean hasNextPageToken = false;
    for (FieldModel field : method.getOutputFields()) {
      String fieldName = field.getSimpleName();
      if (!fieldName.equals(pagingParameters.getNameForNextPageToken())) {
        for (Schema property : field.getDiscoveryField().properties().values()) {
          if (property.getIdentifier().equals(pagingParameters.getNameForNextPageToken())) {
            hasNextPageToken = true;
            resourcesField = Name.anyCamel(fieldName).toUpperCamel();
            break;
          }
        }
      }
    }

    if (resourcesField == null || !hasNextPageToken) {
      return null;
    }

    return PageStreamingResponseView.newBuilder()
        .tokenField(pagingParameters.getNameForNextPageToken())
        .resourcesField(resourcesField)
        .build();
  }
}

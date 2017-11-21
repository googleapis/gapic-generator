/* Copyright 2017 Google LLC
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
package com.google.api.codegen.configgen;

import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProtoMethodModel;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.NullConfigNode;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;

/** PageStreamingTransformer implementation for proto Methods. */
public class ProtoPageStreamingTransformer implements PageStreamingTransformer {
  private static final PagingParameters PAGING_PARAMETERS = new ProtoPagingParameters();

  @Override
  public ConfigNode generateResponseValueNode(MethodModel method, DiagCollector diagCollector) {
    if (!hasResponseTokenField(method)) {
      return new NullConfigNode();
    }

    String resourcesFieldName = getResourcesFieldName(method, diagCollector);
    if (resourcesFieldName == null) {
      return new NullConfigNode();
    }

    ConfigNode tokenFieldNode =
        FieldConfigNode.createStringPair(
            "token_field", PAGING_PARAMETERS.getNameForNextPageToken());
    ConfigNode resourcesFieldNode =
        FieldConfigNode.createStringPair("resources_field", resourcesFieldName);
    return tokenFieldNode.insertNext(resourcesFieldNode);
  }

  private boolean hasResponseTokenField(MethodModel method) {
    FieldModel tokenField = method.getOutputField(PAGING_PARAMETERS.getNameForNextPageToken());
    return tokenField != null;
  }

  private String getResourcesFieldName(MethodModel method, DiagCollector diagCollector) {
    String resourcesField = null;
    for (FieldModel field : method.getOutputFields()) {
      if (!field.isRepeated()) {
        continue;
      }

      if (resourcesField != null) {
        diagCollector.addDiag(
            Diag.error(
                ((ProtoMethodModel) method).getProtoMethod().getLocation(),
                String.format(
                    "Page streaming resources field could not be heuristically determined for "
                        + "method '%s'%n",
                    method.getSimpleName())));
        return null;
      }

      resourcesField = field.getSimpleName();
    }
    return resourcesField;
  }
}

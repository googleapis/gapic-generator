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

import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.IamResourceView;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.TypeRef;
import java.util.ArrayList;
import java.util.List;

public class IamResourceTransformer {
  public List<IamResourceView> generateIamResources(GapicInterfaceContext context) {
    List<IamResourceView> resources = new ArrayList<>();
    for (Field field :
        context.getProductConfig().getInterfaceConfig(context.getInterface()).getIamResources()) {
      String resourceTypeName =
          context
              .getModelTypeTable()
              .getAndSaveNicknameFor(TypeRef.of((MessageType) field.getParent()));
      resources.add(
          IamResourceView.builder()
              .resourceGetterFunctionName(
                  context.getNamer().getIamResourceGetterFunctionName(field))
              .paramName(context.getNamer().getIamResourceParamName(field))
              .exampleName(
                  context
                      .getNamer()
                      .getIamResourceGetterFunctionExampleName(context.getInterface(), field))
              .fieldName(context.getNamer().publicFieldName(Name.from(field.getSimpleName())))
              .resourceTypeName(resourceTypeName)
              .resourceConstructorName(context.getNamer().getTypeConstructor(resourceTypeName))
              .build());
    }
    return resources;
  }
}

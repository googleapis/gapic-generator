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

import com.google.api.codegen.config.BundlingConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.BundlingConfigView;
import com.google.api.codegen.viewmodel.BundlingDescriptorClassView;
import com.google.api.codegen.viewmodel.BundlingPartitionKeyView;
import com.google.api.codegen.viewmodel.FieldCopyView;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.FieldSelector;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import java.util.ArrayList;
import java.util.List;

public class BundlingTransformer {

  public List<BundlingDescriptorClassView> generateDescriptorClasses(
      SurfaceTransformerContext context) {
    List<BundlingDescriptorClassView> descriptors = new ArrayList<>();

    for (Method method : context.getInterface().getMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      if (!methodConfig.isBundling()) {
        continue;
      }
      descriptors.add(generateDescriptorClass(context.asRequestMethodContext(method)));
    }

    return descriptors;
  }

  public BundlingConfigView generateBundlingConfig(MethodTransformerContext context) {
    BundlingConfig bundlingConfig = context.getMethodConfig().getBundling();
    BundlingConfigView.Builder bundlingConfigView = BundlingConfigView.newBuilder();

    bundlingConfigView.elementCountThreshold(bundlingConfig.getElementCountThreshold());
    bundlingConfigView.elementCountLimit(bundlingConfig.getElementCountLimit());
    bundlingConfigView.requestByteThreshold(bundlingConfig.getRequestByteThreshold());
    bundlingConfigView.requestByteLimit(bundlingConfig.getRequestByteLimit());
    bundlingConfigView.delayThresholdMillis(bundlingConfig.getDelayThresholdMillis());

    return bundlingConfigView.build();
  }

  private BundlingDescriptorClassView generateDescriptorClass(MethodTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    ModelTypeTable typeTable = context.getTypeTable();
    Method method = context.getMethod();
    BundlingConfig bundling = context.getMethodConfig().getBundling();

    Field bundledField = bundling.getBundledField();
    TypeRef bundledType = bundledField.getType();
    Name bundledTypeName = Name.from(bundledField.getSimpleName());

    Field subresponseField = bundling.getSubresponseField();
    TypeRef subresponseType = subresponseField.getType();
    Name subresponseTypeName = Name.from(subresponseField.getSimpleName());

    BundlingDescriptorClassView.Builder desc = BundlingDescriptorClassView.newBuilder();

    desc.name(namer.getBundlingDescriptorConstName(method));
    desc.requestTypeName(typeTable.getAndSaveNicknameFor(method.getInputType()));
    desc.responseTypeName(typeTable.getAndSaveNicknameFor(method.getOutputType()));
    desc.bundledFieldTypeName(typeTable.getAndSaveNicknameFor(bundledType));
    desc.subresponseTypeName(typeTable.getAndSaveNicknameFor(subresponseType));

    desc.partitionKeys(generatePartitionKeys(context));
    desc.discriminatorFieldCopies(generateDiscriminatorFieldCopies(context));

    desc.bundledFieldGetFunction(namer.getFieldGetFunctionName(bundledType, bundledTypeName));
    desc.bundledFieldSetFunction(namer.getFieldSetFunctionName(bundledType, bundledTypeName));
    desc.bundledFieldCountGetFunction(namer.getFieldCountGetFunctionName(bundledField));
    desc.subresponseByIndexGetFunction(namer.getByIndexGetFunctionName(subresponseField));
    desc.subresponseSetFunction(
        namer.getFieldSetFunctionName(subresponseType, subresponseTypeName));

    return desc.build();
  }

  private List<BundlingPartitionKeyView> generatePartitionKeys(MethodTransformerContext context) {
    List<BundlingPartitionKeyView> keys = new ArrayList<>();
    BundlingConfig bundling = context.getMethodConfig().getBundling();
    for (FieldSelector fieldSelector : bundling.getDiscriminatorFields()) {
      TypeRef selectedType = fieldSelector.getLastField().getType();
      Name selectedTypeName = Name.from(fieldSelector.getLastField().getSimpleName());
      BundlingPartitionKeyView key =
          BundlingPartitionKeyView.newBuilder()
              .separatorLiteral("\"|\"")
              .fieldGetFunction(
                  context.getNamer().getFieldGetFunctionName(selectedType, selectedTypeName))
              .build();
      keys.add(key);
    }
    return keys;
  }

  private List<FieldCopyView> generateDiscriminatorFieldCopies(MethodTransformerContext context) {
    List<FieldCopyView> fieldCopies = new ArrayList<>();
    BundlingConfig bundling = context.getMethodConfig().getBundling();
    for (FieldSelector fieldSelector : bundling.getDiscriminatorFields()) {
      TypeRef selectedType = fieldSelector.getLastField().getType();
      Name selectedTypeName = Name.from(fieldSelector.getLastField().getSimpleName());
      FieldCopyView fieldCopy =
          FieldCopyView.newBuilder()
              .fieldGetFunction(
                  context.getNamer().getFieldGetFunctionName(selectedType, selectedTypeName))
              .fieldSetFunction(
                  context.getNamer().getFieldSetFunctionName(selectedType, selectedTypeName))
              .build();
      fieldCopies.add(fieldCopy);
    }
    return fieldCopies;
  }
}

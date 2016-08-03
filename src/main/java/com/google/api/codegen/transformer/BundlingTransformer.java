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

import com.google.api.codegen.BundlingConfig;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.viewmodel.BundlingConfigView;
import com.google.api.codegen.viewmodel.BundlingDescriptorClassView;
import com.google.api.codegen.viewmodel.BundlingPartitionKeyView;
import com.google.api.codegen.viewmodel.FieldCopyView;
import com.google.api.tools.framework.model.FieldSelector;
import com.google.api.tools.framework.model.Method;

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
      descriptors.add(generateDescriptorClass(context.asMethodContext(method)));
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

    BundlingDescriptorClassView.Builder desc = BundlingDescriptorClassView.newBuilder();

    desc.name(namer.getBundlingDescriptorConstName(method));
    desc.requestTypeName(typeTable.getAndSaveNicknameFor(method.getInputType()));
    desc.responseTypeName(typeTable.getAndSaveNicknameFor(method.getOutputType()));
    desc.bundledFieldTypeName(
        typeTable.getAndSaveNicknameFor(bundling.getBundledField().getType()));
    desc.subresponseTypeName(
        typeTable.getAndSaveNicknameFor(bundling.getSubresponseField().getType()));

    desc.partitionKeys(generatePartitionKeys(context));
    desc.discriminatorFieldCopies(generateDiscriminatorFieldCopies(context));

    desc.bundledFieldGetFunction(namer.getFieldGetFunctionName(bundling.getBundledField()));
    desc.bundledFieldSetFunction(namer.getFieldSetFunctionName(bundling.getBundledField()));
    desc.bundledFieldCountGetFunction(
        namer.getFieldCountGetFunctionName(bundling.getBundledField()));
    desc.subresponseByIndexGetFunction(
        namer.getByIndexGetFunctionName(bundling.getSubresponseField()));
    desc.subresponseSetFunction(namer.getFieldSetFunctionName(bundling.getSubresponseField()));

    namer.addBundlingDescriptorImports(typeTable);

    return desc.build();
  }

  private List<BundlingPartitionKeyView> generatePartitionKeys(MethodTransformerContext context) {
    List<BundlingPartitionKeyView> keys = new ArrayList<>();
    BundlingConfig bundling = context.getMethodConfig().getBundling();
    for (FieldSelector fieldSelector : bundling.getDiscriminatorFields()) {
      BundlingPartitionKeyView key =
          BundlingPartitionKeyView.newBuilder()
              .separatorLiteral("\"|\"")
              .fieldGetFunction(
                  context.getNamer().getFieldGetFunctionName(fieldSelector.getLastField()))
              .build();
      keys.add(key);
    }
    return keys;
  }

  private List<FieldCopyView> generateDiscriminatorFieldCopies(MethodTransformerContext context) {
    List<FieldCopyView> fieldCopies = new ArrayList<>();
    BundlingConfig bundling = context.getMethodConfig().getBundling();
    for (FieldSelector fieldSelector : bundling.getDiscriminatorFields()) {
      FieldCopyView fieldCopy =
          FieldCopyView.newBuilder()
              .fieldGetFunction(
                  context.getNamer().getFieldGetFunctionName(fieldSelector.getLastField()))
              .fieldSetFunction(
                  context.getNamer().getFieldSetFunctionName(fieldSelector.getLastField()))
              .build();
      fieldCopies.add(fieldCopy);
    }
    return fieldCopies;
  }
}

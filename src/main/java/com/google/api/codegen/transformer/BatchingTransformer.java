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

import com.google.api.codegen.config.BatchingConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.GenericFieldSelector;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.viewmodel.BatchingConfigView;
import com.google.api.codegen.viewmodel.BatchingDescriptorClassView;
import com.google.api.codegen.viewmodel.BatchingDescriptorView;
import com.google.api.codegen.viewmodel.BatchingPartitionKeyView;
import com.google.api.codegen.viewmodel.FieldCopyView;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

public class BatchingTransformer {

  public List<BatchingDescriptorView> generateDescriptors(InterfaceContext context) {
    SurfaceNamer namer = context.getNamer();
    ImmutableList.Builder<BatchingDescriptorView> descriptors = ImmutableList.builder();
    for (MethodModel method : context.getBatchingMethods()) {
      BatchingConfig batching = context.getMethodConfig(method).getBatching();
      BatchingDescriptorView.Builder descriptor = BatchingDescriptorView.newBuilder();
      descriptor.methodName(context.getNamer().getMethodKey(method));
      descriptor.batchedFieldName(namer.getFieldName(batching.getBatchedField()));
      descriptor.discriminatorFieldNames(generateDiscriminatorFieldNames(batching));

      if (batching.hasSubresponseField()) {
        descriptor.subresponseFieldName(namer.getFieldName(batching.getSubresponseField()));
      }

      descriptor.byteLengthFunctionName(
          namer.getByteLengthFunctionName(batching.getBatchedField()));
      descriptors.add(descriptor.build());
    }
    return descriptors.build();
  }

  public List<BatchingDescriptorClassView> generateDescriptorClasses(InterfaceContext context) {
    List<BatchingDescriptorClassView> descriptors = new ArrayList<>();

    for (MethodModel method : context.getInterfaceConfigMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      if (methodConfig == null) {
        continue;
      }
      if (!methodConfig.isBatching()) {
        continue;
      }
      descriptors.add(generateDescriptorClass(context.asRequestMethodContext(method)));
    }

    return descriptors;
  }

  public BatchingConfigView generateBatchingConfig(MethodContext context) {
    BatchingConfig batchingConfig = context.getMethodConfig().getBatching();
    BatchingConfigView.Builder batchingConfigView = BatchingConfigView.newBuilder();

    batchingConfigView.elementCountThreshold(batchingConfig.getElementCountThreshold());
    batchingConfigView.elementCountLimit(batchingConfig.getElementCountLimit());
    batchingConfigView.requestByteThreshold(batchingConfig.getRequestByteThreshold());
    batchingConfigView.requestByteLimit(batchingConfig.getRequestByteLimit());
    batchingConfigView.delayThresholdMillis(batchingConfig.getDelayThresholdMillis());
    batchingConfigView.flowControlElementLimit(batchingConfig.getFlowControlElementLimit());
    batchingConfigView.flowControlByteLimit(batchingConfig.getFlowControlByteLimit());
    batchingConfigView.flowControlLimitExceededBehavior(
        batchingConfig.getFlowControlLimitConfig().toString());

    return batchingConfigView.build();
  }

  private List<String> generateDiscriminatorFieldNames(BatchingConfig batching) {
    ImmutableList.Builder<String> fieldNames = ImmutableList.builder();
    for (GenericFieldSelector fieldSelector : batching.getDiscriminatorFields()) {
      fieldNames.add(fieldSelector.getParamName());
    }
    return fieldNames.build();
  }

  private BatchingDescriptorClassView generateDescriptorClass(MethodContext context) {
    SurfaceNamer namer = context.getNamer();
    MethodModel method = context.getMethodModel();
    BatchingConfig batching = context.getMethodConfig().getBatching();

    FieldModel batchedField = batching.getBatchedField();

    FieldModel subresponseField = batching.getSubresponseField();

    BatchingDescriptorClassView.Builder desc = BatchingDescriptorClassView.newBuilder();

    desc.name(context.getNamer().getBatchingDescriptorConstName(context.getMethodModel()));
    desc.requestTypeName(
        method.getAndSaveRequestTypeName(context.getTypeTable(), context.getNamer()));
    desc.responseTypeName(
        method.getAndSaveResponseTypeName(context.getTypeTable(), context.getNamer()));
    desc.batchedFieldTypeName(context.getTypeTable().getAndSaveNicknameFor(batchedField));

    desc.partitionKeys(generatePartitionKeys(context));
    desc.discriminatorFieldCopies(generateDiscriminatorFieldCopies(context));

    desc.batchedFieldGetFunction(namer.getFieldGetFunctionName(batchedField));
    desc.batchedFieldSetFunction(namer.getFieldSetFunctionName(batchedField));
    desc.batchedFieldCountGetFunction(namer.getFieldCountGetFunctionName(batchedField));

    if (subresponseField != null) {
      desc.subresponseTypeName(context.getTypeTable().getAndSaveNicknameFor(subresponseField));
      desc.subresponseByIndexGetFunction(namer.getByIndexGetFunctionName(subresponseField));
      desc.subresponseSetFunction(namer.getFieldSetFunctionName(subresponseField));
    }

    return desc.build();
  }

  private List<BatchingPartitionKeyView> generatePartitionKeys(MethodContext context) {
    List<BatchingPartitionKeyView> keys = new ArrayList<>();
    BatchingConfig batching = context.getMethodConfig().getBatching();
    for (GenericFieldSelector fieldSelector : batching.getDiscriminatorFields()) {
      FieldModel selectedType = fieldSelector.getLastField();
      BatchingPartitionKeyView key =
          BatchingPartitionKeyView.newBuilder()
              .fieldGetFunction(context.getNamer().getFieldGetFunctionName(selectedType))
              .build();
      keys.add(key);
    }
    return keys;
  }

  private List<FieldCopyView> generateDiscriminatorFieldCopies(MethodContext context) {
    List<FieldCopyView> fieldCopies = new ArrayList<>();
    BatchingConfig batching = context.getMethodConfig().getBatching();
    for (GenericFieldSelector fieldSelector : batching.getDiscriminatorFields()) {
      FieldModel selectedType = fieldSelector.getLastField();
      FieldCopyView fieldCopy =
          FieldCopyView.newBuilder()
              .fieldGetFunction(context.getNamer().getFieldGetFunctionName(selectedType))
              .fieldSetFunction(context.getNamer().getFieldSetFunctionName(selectedType))
              .build();
      fieldCopies.add(fieldCopy);
    }
    return fieldCopies;
  }
}

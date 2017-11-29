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
package com.google.api.codegen.clientconfig.transformer;

import com.google.api.codegen.clientconfig.viewmodel.PairView;
import com.google.api.codegen.config.BatchingConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class BatchingTransformer {
  public List<PairView> generateBatching(MethodConfig methodConfig) {
    if (!methodConfig.isBatching()) {
      return ImmutableList.of();
    }

    BatchingConfig batchingConfig = methodConfig.getBatching();
    ImmutableList.Builder<PairView> builder = ImmutableList.builder();
    if (batchingConfig.getElementCountThreshold() > 0) {
      builder.add(
          PairView.newBuilder()
              .key("element_count_threshold")
              .value(String.valueOf(batchingConfig.getElementCountThreshold()))
              .build());
    }

    if (batchingConfig.getElementCountLimit() > 0) {
      builder.add(
          PairView.newBuilder()
              .key("element_count_limit")
              .value(String.valueOf(batchingConfig.getElementCountLimit()))
              .build());
    }

    if (batchingConfig.getRequestByteThreshold() > 0) {
      builder.add(
          PairView.newBuilder()
              .key("request_byte_threshold")
              .value(String.valueOf(batchingConfig.getRequestByteThreshold()))
              .build());
    }

    if (batchingConfig.getRequestByteLimit() > 0) {
      builder.add(
          PairView.newBuilder()
              .key("request_byte_limit")
              .value(String.valueOf(batchingConfig.getRequestByteLimit()))
              .build());
    }

    if (batchingConfig.getDelayThresholdMillis() > 0) {
      builder.add(
          PairView.newBuilder()
              .key("delay_threshold_millis")
              .value(String.valueOf(batchingConfig.getDelayThresholdMillis()))
              .build());
    }

    return builder.build();
  }
}

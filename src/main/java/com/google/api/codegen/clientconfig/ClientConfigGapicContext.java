/* Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.clientconfig;

import com.google.api.codegen.GapicContext;
import com.google.api.codegen.config.BatchingConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;

public class ClientConfigGapicContext extends GapicContext {

  /** Constructs the client config codegen context. */
  public ClientConfigGapicContext(Model model, GapicProductConfig config) {
    super(model, config);
  }

  public Map<String, String> batchingParams(BatchingConfig batching) {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder();
    if (batching.getElementCountThreshold() > 0) {
      builder.put("element_count_threshold", Integer.toString(batching.getElementCountThreshold()));
    }
    if (batching.getElementCountLimit() > 0) {
      builder.put("element_count_limit", Integer.toString(batching.getElementCountLimit()));
    }
    if (batching.getRequestByteThreshold() > 0) {
      builder.put("request_byte_threshold", Long.toString(batching.getRequestByteThreshold()));
    }
    if (batching.getRequestByteLimit() > 0) {
      builder.put("request_byte_limit", Long.toString(batching.getRequestByteLimit()));
    }
    if (batching.getDelayThresholdMillis() > 0) {
      builder.put("delay_threshold_millis", Long.toString(batching.getDelayThresholdMillis()));
    }
    return builder.build();
  }

  @Override
  protected boolean isSupported(Method method) {
    return true;
  }

  @Override
  public List<Method> getSupportedMethods(Interface apiInterface) {
    return getSupportedMethodsV2(apiInterface);
  }
}

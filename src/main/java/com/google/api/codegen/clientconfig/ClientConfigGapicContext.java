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
package com.google.api.codegen.clientconfig;

import com.google.api.codegen.GapicContext;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.BundlingConfig;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;

public class ClientConfigGapicContext extends GapicContext {

  /** Constructs the client config codegen context. */
  public ClientConfigGapicContext(Model model, ApiConfig config) {
    super(model, config);
  }

  public Map<String, String> bundlingParams(BundlingConfig bundling) {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder();
    if (bundling.getElementCountThreshold() > 0) {
      builder.put("element_count_threshold", Integer.toString(bundling.getElementCountThreshold()));
    }
    if (bundling.getElementCountLimit() > 0) {
      builder.put("element_count_limit", Integer.toString(bundling.getElementCountLimit()));
    }
    if (bundling.getRequestByteThreshold() > 0) {
      builder.put("request_byte_threshold", Long.toString(bundling.getRequestByteThreshold()));
    }
    if (bundling.getRequestByteLimit() > 0) {
      builder.put("request_byte_limit", Long.toString(bundling.getRequestByteLimit()));
    }
    if (bundling.getDelayThresholdMillis() > 0) {
      builder.put("delay_threshold_millis", Long.toString(bundling.getDelayThresholdMillis()));
    }
    return builder.build();
  }

  @Override
  protected boolean isSupported(Method method) {
    return true;
  }

  @Override
  public List<Method> getSupportedMethods(Interface service) {
    return getSupportedMethodsV2(service);
  }
}

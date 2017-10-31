/* Copyright 2016 Google LLC
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
package com.google.api.codegen.config;

import com.google.api.codegen.FlowControlLimitExceededBehaviorProto;
import com.google.common.collect.ImmutableMap;

public enum FlowControlLimitConfig {
  ThrowException,
  Block,
  Ignore;

  private static ImmutableMap<FlowControlLimitExceededBehaviorProto, FlowControlLimitConfig>
      protoMap =
          ImmutableMap.of(
              FlowControlLimitExceededBehaviorProto.THROW_EXCEPTION,
              ThrowException,
              FlowControlLimitExceededBehaviorProto.BLOCK,
              Block,
              FlowControlLimitExceededBehaviorProto.IGNORE,
              Ignore,
              FlowControlLimitExceededBehaviorProto.UNSET_BEHAVIOR,
              Ignore);

  public static FlowControlLimitConfig fromProto(FlowControlLimitExceededBehaviorProto proto) {
    return protoMap.get(proto);
  }
}

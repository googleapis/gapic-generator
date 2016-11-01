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
package com.google.api.codegen.discovery;

import com.google.api.tools.framework.snippet.Doc;
import com.google.protobuf.Method;
import java.util.Map;

/** A DiscoveryProvider performs fragment generation using discovery-based input. */
public interface DiscoveryProvider {
  /** Runs code generation and returns a map from relative file paths to generated Doc. */
  Map<String, Doc> generate(Method method);
}

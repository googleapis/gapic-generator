/* Copyright 2017 Google Inc
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
package com.google.api.codegen.discovery2.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.viewmodel.ViewModel;

/*
 * Transforms a Method into a ViewModel.
 */
public interface SampleTransformer {

  ViewModel transform(Method method, String authInstructionsUrl, JsonNode override);
}

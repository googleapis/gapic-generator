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
package com.google.api.codegen.viewmodel.testing;

import com.google.api.codegen.viewmodel.InitCodeView;

import java.util.List;

public class ApiTestCaseView {
  public String name;
  public String methodName;
  public String requestType;
  public String resourceType;
  public boolean isPageStreaming;
  public InitCodeView initCode;
  public List<ApiTestAssertView> asserts;
}

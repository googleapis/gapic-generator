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
package com.google.api.codegen.viewmodel;

import java.util.List;

public class OptionalArrayMethodView implements ApiMethodView {

  public ApiMethodType type;
  public String apiClassName;
  public String apiVariableName;
  public InitCodeView initCode;
  public ApiMethodDocView doc;
  public String name;
  public String requestTypeName;
  public String key;
  public Object grpcMethodName;
  public List<DynamicDefaultableParamView> methodParams;
  public List<RequestObjectParamView> requiredRequestObjectParams;
  public List<RequestObjectParamView> optionalRequestObjectParams;
  public boolean hasReturnValue;
}

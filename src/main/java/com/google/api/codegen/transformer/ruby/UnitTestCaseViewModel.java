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
package com.google.api.codegen.transformer.ruby;

import com.google.api.codegen.ruby.RubyGapicContext;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.common.base.Preconditions;

/** ViewModel of each unit test case for Ruby. */
// TODO(jcanizales): Only RubyGapicContext here is Ruby-specific, and unnecessarily so. Refactor so
// this can be shared with other languages.
public class UnitTestCaseViewModel {

  private final Interface service;
  private final Method method;
  private final ModelTypeTable typeTable;
  private final RubyGapicContext util;

  public UnitTestCaseViewModel(
      Interface service, Method method, ModelTypeTable typeTable, RubyGapicContext util) {
    this.service = Preconditions.checkNotNull(service);
    this.method = Preconditions.checkNotNull(method);
    this.typeTable = Preconditions.checkNotNull(typeTable);
    this.util = Preconditions.checkNotNull(util);
  }

  public OptionalArrayMethodView methodView() {
    return util.getMethodView(service, method);
  }

  public String requestTypeName() {
    return typeTable.getAndSaveNicknameFor(method.getInputType());
  }

  public String responseTypeName() {
    return typeTable.getAndSaveNicknameFor(method.getOutputType());
  }
}

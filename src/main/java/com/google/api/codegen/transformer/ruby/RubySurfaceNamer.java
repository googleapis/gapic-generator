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

import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.ruby.RubyNameFormatter;
import com.google.api.codegen.util.ruby.RubyTypeTable;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;

/** The SurfaceNamer for Ruby. */
public class RubySurfaceNamer extends SurfaceNamer {
  public RubySurfaceNamer() {
    super(
        new RubyNameFormatter(),
        new ModelTypeFormatterImpl(new RubyModelTypeNameConverter()),
        new RubyTypeTable());
  }

  @Override
  /** The function name to set a field having the given type and name. */
  public String getFieldSetFunctionName(TypeRef type, Name identifier) {
    if (type.isMap()) {
      return methodName(Name.from("merge").join(identifier));
    } else if (type.isRepeated()) {
      return methodName(Name.from("concat").join(identifier));
    } else {
      return methodName(Name.from("set").join(identifier));
    }
  }

  @Override
  /** The page streaming descriptor name for the given method. */
  public String getPageStreamingDescriptorName(Method method) {
    return varName(Name.from(method.getSimpleName(), "page", "streaming", "descriptor"));
  }

  @Override
  /** The name of the constant to hold the page streaming descriptor for the given method. */
  public String getPageStreamingDescriptorConstName(Method method) {
    return inittedConstantName(Name.upperCamel(method.getSimpleName()).join("PageStrDesc"));
  }

  @Override
  /** The name of the constant to hold the bundling descriptor for the given method. */
  public String getBundlingDescriptorConstName(Method method) {
    return inittedConstantName(Name.upperCamel(method.getSimpleName()).join("BundlingDesc"));
  }

  @Override
  /** Adds the imports used in the implementation of page streaming descriptors. */
  public void addPageStreamingDescriptorImports(ModelTypeTable typeTable) {
    typeTable.saveNicknameFor("Google::GAX::PageStreamingDescriptor");
  }

  @Override
  /** Adds the imports used for page streaming call settings. */
  public void addPageStreamingCallSettingsImports(ModelTypeTable typeTable) {
    typeTable.saveNicknameFor("Google::GAX::PageStreamingCallSettings");
  }

  @Override
  /** The key to use in a dictionary for the given method. */
  public String getMethodKey(Method method) {
    return keyName(Name.from(method.getSimpleName()));
  }

  @Override
  /** The path to the client config for the given interface. */
  public String getClientConfigPath(Interface service) {
    return getNotImplementedString("SurfaceNamer#get_client_config_path");
  }

  @Override
  /**
   * The type name of the method constant in the Grpc container class. This needs to match what Grpc
   * generates for the particular language.
   */
  public String getGrpcMethodConstant(Method method) {
    return inittedConstantName(Name.from("Method").join(Name.upperCamel(method.getSimpleName())));
  }

  @Override
  /** The name of the surface method which can call the given API method. */
  public String getApiMethodName(Method method) {
    return methodName(Name.from(method.getSimpleName()));
  }

  @Override
  /** The name of the paged callable variant of the given method. */
  public String getPagedCallableMethodName(Method method) {
    return methodName(Name.from(method.getSimpleName(), "paged", "callable"));
  }

  @Override
  /** The name of the callable for the paged callable variant of the given method. */
  public String getPagedCallableName(Method method) {
    return varName(Name.from(method.getSimpleName(), "paged", "callable"));
  }

  @Override
  /** The name of the plain callable variant of the given method. */
  public String getCallableMethodName(Method method) {
    return methodName(Name.from(method.getSimpleName(), "callable"));
  }

  @Override
  /** The name of the plain callable for the given method. */
  public String getCallableName(Method method) {
    return varName(Name.from(method.getSimpleName(), "callable"));
  }

  @Override
  /** The name of the settings member name for the given method. */
  public String getSettingsMemberName(Method method) {
    return methodName(Name.from(method.getSimpleName(), "settings"));
  }

  @Override
  /** The test case name for the given method. */
  public String getTestCaseName(Method method) {
    return methodName(Name.from(method.getSimpleName(), "test"));
  }

  @Override
  /** The method name of getter function call for the given name */
  public String getGetFunctionCallName(Name name) {
    return methodName(Name.from("get").join(name));
  }
}

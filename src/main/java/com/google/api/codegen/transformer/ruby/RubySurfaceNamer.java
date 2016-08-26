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

import com.google.api.codegen.CollectionConfig;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.ruby.RubyNameFormatter;
import com.google.api.codegen.util.ruby.RubyTypeTable;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;

/** The SurfaceNamer for Ruby. */
public class RubySurfaceNamer extends SurfaceNamer {
  public RubySurfaceNamer(String packageName) {
    super(
        new RubyNameFormatter(),
        new ModelTypeFormatterImpl(new RubyModelTypeNameConverter()),
        new RubyTypeTable());
  }

  /** The function name to set a field having the given type and name. */
  @Override
  public String getFieldSetFunctionName(TypeRef type, Name identifier) {
    return methodName(identifier);
  }

  /** The function name to format the entity for the given collection. */
  @Override
  public String getFormatFunctionName(CollectionConfig collectionConfig) {
    return staticFunctionName(Name.from(collectionConfig.getEntityName(), "path"));
  }

  /** The file name for an API service. */
  @Override
  public String getServiceFileName(Interface service, String packageName) {
    String[] names = packageName.split("::");
    List<String> newNames = new ArrayList<>();
    for (String name : names) {
      newNames.add(Name.upperCamel(name).toLowerUnderscore());
    }
    newNames.add(Name.upperCamel(getApiWrapperClassName(service)).toLowerUnderscore());
    return Joiner.on("/").join(newNames.toArray());
  }

  @Override
  public String getFullyQualifiedApiWrapperClassName(Interface service, String packageName) {
    return packageName + "::" + getApiWrapperClassName(service);
  }
}

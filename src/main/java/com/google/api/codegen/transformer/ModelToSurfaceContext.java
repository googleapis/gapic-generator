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
package com.google.api.codegen.transformer;

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.CollectionConfig;
import com.google.api.codegen.InterfaceConfig;
import com.google.api.codegen.MethodConfig;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.auto.value.AutoValue;

import java.util.Collection;

@AutoValue
public abstract class ModelToSurfaceContext {
  public static ModelToSurfaceContext create(
      Interface interfaze, ApiConfig apiConfig, ModelTypeTable typeTable, IdentifierNamer namer) {
    return new AutoValue_ModelToSurfaceContext(interfaze, apiConfig, typeTable, namer);
  }

  public abstract Interface getInterface();

  public abstract ApiConfig getApiConfig();

  public abstract ModelTypeTable getTypeTable();

  public abstract IdentifierNamer getNamer();

  public ModelToSurfaceContext withNewTypeTable() {
    return create(getInterface(), getApiConfig(), getTypeTable().cloneEmpty(), getNamer());
  }

  public InterfaceConfig getInterfaceConfig() {
    return getApiConfig().getInterfaceConfig(getInterface());
  }

  public MethodConfig getMethodConfig(Method method) {
    return getInterfaceConfig().getMethodConfig(method);
  }

  public Collection<CollectionConfig> getCollectionConfigs() {
    return getInterfaceConfig().getCollectionConfigs();
  }

  public CollectionConfig getCollectionConfig(String entityName) {
    return getInterfaceConfig().getCollectionConfig(entityName);
  }
}

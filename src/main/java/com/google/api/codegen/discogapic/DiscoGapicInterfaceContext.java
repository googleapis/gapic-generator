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
package com.google.api.codegen.discogapic;

import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.InterfaceContext;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.TypeTable;
import com.google.api.tools.framework.model.Method;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class DiscoGapicInterfaceContext implements InterfaceContext {
  public static DiscoGapicInterfaceContext create(
      Document document,
      GapicProductConfig productConfig,
      ModelTypeTable typeTable,
      SurfaceNamer namer,
      FeatureConfig featureConfig) {
    return new AutoValue_DiscoGapicInterfaceContext(
        document, productConfig, typeTable, namer, featureConfig);
  }

  public abstract Document getDocument();

  @Override
  public abstract GapicProductConfig getProductConfig();

  public abstract ModelTypeTable getModelTypeTable();

  @Override
  public TypeTable getTypeTable() {
    return getModelTypeTable().getTypeTable();
  }

  @Override
  public abstract SurfaceNamer getNamer();

  public abstract FeatureConfig getFeatureConfig();

  public DiscoGapicInterfaceContext withNewTypeTable() {
    return create(
        getDocument(),
        getProductConfig(),
        getModelTypeTable().cloneEmpty(),
        getNamer(),
        getFeatureConfig());
  }

  public DiscoGapicInterfaceConfig getInterfaceConfig() {
    return (DiscoGapicInterfaceConfig) getProductConfig().getInterfaceConfig(getDocument().name());
  }

  public GapicMethodConfig getMethodConfig(Method method) {
    return null;
  }
}

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

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.transformer.InterfaceContext;
import com.google.api.codegen.transformer.SchemaTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.TypeTable;
import com.google.auto.value.AutoValue;
import java.util.Comparator;

/**
 * The context for transforming a single top-level schema from Discovery Doc API into a top-level
 * view for client library generation.
 *
 * <p>This context contains a reference to the parent Document context.
 */
@AutoValue
public abstract class RequestInterfaceContext implements InterfaceContext {
  public static RequestInterfaceContext create(
      DiscoGapicInterfaceContext docContext, Method method, SchemaTypeTable typeTable) {
    return new AutoValue_RequestInterfaceContext(method, docContext, typeTable);
  }

  public abstract Method getMethod();

  /** @return the parent Document-level InterfaceContext. */
  public abstract DiscoGapicInterfaceContext getDocContext();

  public DiscoGapicNamer getDiscoGapicNamer() {
    return getDocContext().getDiscoGapicNamer();
  }

  public abstract SchemaTypeTable getSchemaTypeTable();

  public TypeTable getTypeTable() {
    return getSchemaTypeTable().getTypeTable();
  }

  @Override
  public GapicProductConfig getProductConfig() {
    return getDocContext().getProductConfig();
  }

  @Override
  public SurfaceNamer getNamer() {
    return getDocContext().getNamer();
  }

  public DiscoGapicInterfaceConfig getInterfaceConfig() {
    return (DiscoGapicInterfaceConfig) getProductConfig().getInterfaceConfig(getMethod().id());
  }

  public static Comparator<RequestInterfaceContext> comparator =
      new Comparator<RequestInterfaceContext>() {
        @Override
        public int compare(RequestInterfaceContext o1, RequestInterfaceContext o2) {
          return String.CASE_INSENSITIVE_ORDER.compare(o1.getMethod().id(), o2.getMethod().id());
        }
      };
}

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
package com.google.api.codegen.discovery.transformer.java;

import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.discovery.transformer.MethodToViewTransformer;
import com.google.api.codegen.discovery.transformer.MethodTypeTable;
import com.google.api.codegen.discovery.transformer.SurfaceTransformerContext;
import com.google.api.codegen.discovery.viewmodel.StaticLangXApiView;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.protobuf.Method;

/*
 * Transforms a Model into the standard discovery surface in Java.
 */
public class JavaSurfaceTransformer implements MethodToViewTransformer {

  private final static String TEMPLATE_FILENAME = "java/discovery_fragment.snip";

  public JavaSurfaceTransformer() {}

  @Override
  public ViewModel transform(Method method, ApiaryConfig apiaryConfig) {
    JavaSurfaceNamer namer = new JavaSurfaceNamer();
    SurfaceTransformerContext context =
        SurfaceTransformerContext.create(method, apiaryConfig, createTypeTable(), namer);

    return StaticLangXApiView.newBuilder()
        .templateFileName(TEMPLATE_FILENAME)
        .outputPath("output")
        .build();
  }

  /*
   * Returns a new Java TypeTable.
   */
  private MethodTypeTable createTypeTable() {
    return new MethodTypeTable(new JavaTypeTable(), new JavaTypeNameConverter());
  }
}

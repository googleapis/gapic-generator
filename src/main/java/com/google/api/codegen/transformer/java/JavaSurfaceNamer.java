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
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.java.JavaNameFormatter;
import com.google.api.codegen.util.java.JavaRenderingUtil;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.ProtoElement;

import java.util.List;

public class JavaSurfaceNamer extends SurfaceNamer {

  public JavaSurfaceNamer() {
    super(new JavaNameFormatter(), new ModelTypeFormatterImpl(new JavaModelTypeNameConverter()));
  }

  @Override
  public List<String> getDocLines(ProtoElement element) {
    return JavaRenderingUtil.getDocLines(DocumentationUtil.getDescription(element));
  }
}

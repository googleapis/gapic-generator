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

import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.java.JavaDocUtil;
import com.google.api.codegen.util.JavaNamer;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.TypeRef;

import java.util.Arrays;
import java.util.List;

public class JavaSurfaceNamer extends SurfaceNamer {

  public JavaSurfaceNamer() {
    super(new JavaNamer());
  }

  @Override
  public List<String> getDocLines(ProtoElement element) {
    return JavaDocUtil.getJavaDocLines(DocumentationUtil.getDescription(element));
  }

  @Override
  public List<String> getThrowsDocLines() {
    return Arrays.asList("@throws com.google.api.gax.grpc.ApiException if the remote call fails");
  }

  @Override
  public String getStaticReturnTypeName(
      ModelTypeTable typeTable, Method method, MethodConfig methodConfig) {
    if (ServiceMessages.s_isEmptyType(method.getOutputType())) {
      return "void";
    }
    return typeTable.getAndSaveNicknameFor(method.getOutputType());
  }

  @Override
  public String getGenericAwareResponseType(ModelTypeTable typeTable, TypeRef outputType) {
    if (ServiceMessages.s_isEmptyType(outputType)) {
      return "Void";
    } else {
      return typeTable.getAndSaveNicknameFor(outputType);
    }
  }
}

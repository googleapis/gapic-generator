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
package com.google.api.codegen.transformer.go;

import com.google.api.codegen.CollectionConfig;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.go.GoNameFormatter;
import com.google.api.codegen.util.go.GoTypeTable;
import com.google.api.codegen.util.Name;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.TypeRef;

import java.util.List;

public class GoSurfaceNamer extends SurfaceNamer {
  private final String serviceName;
  private final GoModelTypeNameConverter converter;

  public GoSurfaceNamer(String serviceName) {
    this(serviceName, new GoModelTypeNameConverter());
  }

  private GoSurfaceNamer(String serviceName, GoModelTypeNameConverter converter) {
    super(new GoNameFormatter(), new ModelTypeFormatterImpl(converter), new GoTypeTable());
    this.converter = converter;
    this.serviceName = serviceName;
  }

  @Override
  public String getPathTemplateName(CollectionConfig collectionConfig) {
    return inittedConstantName(
        Name.from(serviceName, collectionConfig.getEntityName(), "path", "template"));
  }

  @Override
  public String getPathTemplateNameGetter(CollectionConfig collectionConfig) {
    return methodName(Name.from(serviceName, collectionConfig.getEntityName(), "path"));
  }

  @Override
  public String getStaticLangReturnTypeName(Method method, MethodConfig methodConfig) {
    return converter.getTypeName(method.getOutputType()).getFullName();
  }

  @Override
  public List<String> getDocLines(ProtoElement element) {
    if (!(element instanceof Method)) {
      return super.getDocLines(element);
    }
    Method method = (Method) element;
    String text = DocumentationUtil.getDescription(method);
    text = lowerFirstLetter(text);
    return super.getDocLines(getApiMethodName(method) + " " + text);
  }

  @Override
  public String getAndSavePagedResponseTypeName(ModelTypeTable typeTable, TypeRef resourceType) {
    String typeName = converter.getTypeNameForElementType(resourceType).getNickname();
    return upperFirstLetter(typeName) + "Iterator";
  }

  private static String lowerFirstLetter(String s) {
    if (s.length() > 0) {
      s = Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
    return s;
  }

  private static String upperFirstLetter(String s) {
    if (s.length() > 0) {
      s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
    return s;
  }

  @Override
  public String getFieldGetFunctionName(TypeRef type, Name identifier) {
    return methodName(identifier);
  }
}

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
package com.google.api.codegen.java;

import com.google.api.codegen.CollectionConfig;
import com.google.api.codegen.LanguageUtil;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.metacode.FieldStructureParser;
import com.google.api.codegen.metacode.InitCode;
import com.google.api.codegen.metacode.InitCodeGenerator;
import com.google.api.codegen.metacode.InitValueConfig;
import com.google.api.codegen.metacode.InputParameter;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * Represents the documentation settings for an Api method.
 */
@AutoValue
abstract class JavaDocConfig {
  public abstract String getApiName();

  public abstract String getMethodName();

  public abstract String getReturnType();

  public String getGenericAwareReturnType() {
    String returnType = getReturnType();
    if (returnType == null || returnType.isEmpty()) {
      return "Void";
    } else {
      return returnType;
    }
  }

  public abstract InitCode getInitCode();

  public abstract ImmutableList<InputParameter> getParams();

  public abstract boolean isPagedVariant();

  public abstract boolean isCallableVariant();

  @Nullable
  public abstract Field getResourcesFieldForUnpagedListCallable();

  public boolean isUnpagedListCallableVariant() {
    return getResourcesFieldForUnpagedListCallable() != null;
  }

  public static Builder newBuilder() {
    return new AutoValue_JavaDocConfig.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {
    private static final String REQUEST_PARAM_DOC =
        "The request object containing all of the parameters for the API call.";

    private static final String REQUEST_PARAM_NAME = "request";

    public abstract JavaDocConfig.Builder setApiName(String serviceName);

    public abstract JavaDocConfig.Builder setMethodName(String methodName);

    public abstract JavaDocConfig.Builder setReturnType(String returnType);

    public abstract JavaDocConfig.Builder setInitCode(InitCode initCode);

    public JavaDocConfig.Builder setRequestObjectInitCode(JavaGapicContext context, Interface service, Method method) {
      Map<String, Object> initFieldStructure = createInitFieldStructure(context, service, method);
      InitCodeGenerator generator = new InitCodeGenerator();
      InitCode initCode = generator.generateRequestObjectInitCode(method, initFieldStructure);
      setInitCode(initCode);
      return this;
    }

    public JavaDocConfig.Builder setFieldInitCode(JavaGapicContext context, Interface service,
      Method method, Iterable<Field> fields) {
      Map<String, Object> initFieldStructure = createInitFieldStructure(context, service, method);
      InitCodeGenerator generator = new InitCodeGenerator();
      InitCode initCode = generator.generateRequestFieldInitCode(method, initFieldStructure, fields);
      setInitCode(initCode);
      return this;
    }

    private static Map<String, Object> createInitFieldStructure(
        JavaGapicContext context, Interface service, Method method) {
      MethodConfig methodConfig = context.getApiConfig().getInterfaceConfig(service).getMethodConfig(method);
      Map<String, String> fieldNamePatterns = methodConfig.getFieldNamePatterns();

      ImmutableMap.Builder<String, InitValueConfig> initValueConfigMap = ImmutableMap.builder();
      for (Map.Entry<String, String> fieldNamePattern : fieldNamePatterns.entrySet()) {
        CollectionConfig collectionConfig = context.getCollectionConfig(
            service, fieldNamePattern.getValue());
        InitValueConfig initValueConfig = InitValueConfig.create(context.getApiWrapperName(service), collectionConfig);
        initValueConfigMap.put(fieldNamePattern.getKey(), initValueConfig);
      }
      Map<String, Object> initFieldStructure = FieldStructureParser
          .parseFields(methodConfig.getSampleCodeInitFields(), initValueConfigMap.build());
      return initFieldStructure;
    }

    public abstract JavaDocConfig.Builder setParams(ImmutableList<InputParameter> params);

    public JavaDocConfig.Builder setRequestObjectParam(JavaGapicContext context, Interface service, Method method) {
      InputParameter param = InputParameter.newBuilder()
          .setType(method.getInputType())
          .setName(REQUEST_PARAM_NAME)
          .setDescription(REQUEST_PARAM_DOC)
          .build();
      return setParams(ImmutableList.of(param));
    }

    public JavaDocConfig.Builder setEmptyParams() {
      return setParams(ImmutableList.<InputParameter>of());
    }

    public JavaDocConfig.Builder setFieldParams(JavaGapicContext context, Iterable<Field> fields) {
      ImmutableList.Builder<InputParameter> params = ImmutableList.<InputParameter>builder();

      for (Field field : fields) {
        InputParameter.Builder inputParamBuilder = InputParameter.newBuilder();
        inputParamBuilder.setType(field.getType());
        inputParamBuilder.setName(LanguageUtil.lowerUnderscoreToLowerCamel(field.getSimpleName()));
        inputParamBuilder.setDescription(context.getDescription(field));
        params.add(inputParamBuilder.build());
      }
      setParams(params.build());
      return this;
    }

    public abstract JavaDocConfig.Builder setPagedVariant(boolean paged);

    public abstract JavaDocConfig.Builder setCallableVariant(boolean callable);

    public abstract JavaDocConfig.Builder setResourcesFieldForUnpagedListCallable(Field field);

    public abstract JavaDocConfig build();
  }
}

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
package com.google.api.codegen;

import com.google.api.codegen.metacode.FieldStructureParser;
import com.google.api.codegen.metacode.InitCode;
import com.google.api.codegen.metacode.InitCodeGenerator;
import com.google.api.codegen.metacode.InitCodeGeneratorContext;
import com.google.api.codegen.metacode.InitValueConfig;
import com.google.api.codegen.metacode.InputParameter;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/** Represents the generic documentation settings for an Api method. */
public abstract class DocConfig {
  public abstract String getApiName();

  public abstract String getMethodName();

  public abstract String getReturnType();

  public abstract InitCode getInitCode();

  public abstract ImmutableList<InputParameter> getParams();

  /** DocConfig builder minimum functionality */
  public abstract static class Builder<BuilderType extends Builder<BuilderType>> {
    private static final String REQUEST_PARAM_DOC =
        "The request object containing all of the parameters for the API call.";

    private static final String REQUEST_PARAM_NAME = "request";

    protected abstract BuilderType setInitCodeProxy(InitCode initCode);

    protected abstract BuilderType setParamsProxy(ImmutableList<InputParameter> params);

    @SuppressWarnings("unchecked")
    public BuilderType setRequestObjectInitCode(
        GapicContext context, Interface service, Method method) {
      Map<String, Object> initFieldStructure = createInitFieldStructure(context, service, method);
      InitCodeGenerator generator = new InitCodeGenerator();
      InitCodeGeneratorContext initContext =
          InitCodeGeneratorContext.newBuilder()
              .symbolTable(new SymbolTable())
              .initStructure(initFieldStructure)
              .method(method)
              .build();
      InitCode initCode = generator.generateRequestObjectInitCode(initContext);
      setInitCodeProxy(initCode);
      return (BuilderType) this;
    }

    @SuppressWarnings("unchecked")
    public BuilderType setFieldInitCode(
        GapicContext context, Interface service, Method method, Iterable<Field> fields) {
      Map<String, Object> initFieldStructure = createInitFieldStructure(context, service, method);
      InitCodeGenerator generator = new InitCodeGenerator();
      InitCodeGeneratorContext initContext =
          InitCodeGeneratorContext.newBuilder()
              .symbolTable(new SymbolTable())
              .initStructure(initFieldStructure)
              .method(method)
              .typeTable(null)
              .build();
      InitCode initCode = generator.generateRequestFieldInitCode(initContext, fields);
      setInitCodeProxy(initCode);
      return (BuilderType) this;
    }

    private static Map<String, Object> createInitFieldStructure(
        GapicContext context, Interface service, Method method) {
      MethodConfig methodConfig =
          context.getApiConfig().getInterfaceConfig(service).getMethodConfig(method);
      Map<String, String> fieldNamePatterns = methodConfig.getFieldNamePatterns();

      ImmutableMap.Builder<String, InitValueConfig> initValueConfigMap = ImmutableMap.builder();
      for (Map.Entry<String, String> fieldNamePattern : fieldNamePatterns.entrySet()) {
        CollectionConfig collectionConfig =
            context.getCollectionConfig(service, fieldNamePattern.getValue());
        InitValueConfig initValueConfig =
            InitValueConfig.create(context.getApiWrapperName(service), collectionConfig);
        initValueConfigMap.put(fieldNamePattern.getKey(), initValueConfig);
      }
      Map<String, Object> initFieldStructure =
          FieldStructureParser.parseFields(
              methodConfig.getSampleCodeInitFields(), initValueConfigMap.build());
      return initFieldStructure;
    }

    public BuilderType setRequestObjectParam(Method method) {
      InputParameter param =
          InputParameter.newBuilder()
              .setType(method.getInputType())
              .setName(REQUEST_PARAM_NAME)
              .setDescription(REQUEST_PARAM_DOC)
              .build();
      return setParamsProxy(ImmutableList.of(param));
    }

    public BuilderType setEmptyParams() {
      return setParamsProxy(ImmutableList.<InputParameter>of());
    }

    @SuppressWarnings("unchecked")
    public BuilderType setFieldParams(GapicContext context, Iterable<Field> fields) {
      ImmutableList.Builder<InputParameter> params = ImmutableList.<InputParameter>builder();

      for (Field field : fields) {
        InputParameter.Builder inputParamBuilder = InputParameter.newBuilder();
        inputParamBuilder.setType(field.getType());
        inputParamBuilder.setName(LanguageUtil.lowerUnderscoreToLowerCamel(field.getSimpleName()));
        inputParamBuilder.setDescription(context.getDescription(field));
        params.add(inputParamBuilder.build());
      }
      setParamsProxy(params.build());
      return (BuilderType) this;
    }
  }
}

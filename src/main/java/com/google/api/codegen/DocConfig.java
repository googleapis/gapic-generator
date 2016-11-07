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

import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.metacode.FieldSetting;
import com.google.api.codegen.metacode.InitCode;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeLine;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.metacode.InitValueConfig;
import com.google.api.codegen.metacode.InputParameter;
import com.google.api.codegen.metacode.StructureInitCodeLine;
import com.google.api.codegen.util.Name;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Represents the generic documentation settings for an Api method. */
@Deprecated // Obsolete with MVVM
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
      List<InitCodeLine> initCodeLines = createInitCodeLines(context, service, method, null);
      InitCodeLine lastLine = initCodeLines.get(initCodeLines.size() - 1);
      FieldSetting objectField =
          FieldSetting.create(
              method.getInputType(),
              Name.from("request"),
              lastLine.getIdentifier(),
              lastLine.getInitValueConfig());
      List<FieldSetting> outputFields = Arrays.asList(objectField);
      InitCode initCode = InitCode.create(initCodeLines, outputFields);
      setInitCodeProxy(initCode);
      return (BuilderType) this;
    }

    @SuppressWarnings("unchecked")
    public BuilderType setFieldInitCode(
        GapicContext context, Interface service, Method method, Iterable<Field> fields) {
      List<InitCodeLine> initCodeLines = createInitCodeLines(context, service, method, fields);
      InitCodeLine lastLine = initCodeLines.remove(initCodeLines.size() - 1);
      List<FieldSetting> outputFields;
      if (initCodeLines.size() == 0) {
        outputFields = new ArrayList<>();
      } else {
        outputFields = ((StructureInitCodeLine) lastLine).getFieldSettings();
      }
      InitCode initCode = InitCode.create(initCodeLines, outputFields);
      setInitCodeProxy(initCode);
      return (BuilderType) this;
    }

    private static List<InitCodeLine> createInitCodeLines(
        GapicContext context, Interface service, Method method, Iterable<Field> fields) {
      MethodConfig methodConfig =
          context.getApiConfig().getInterfaceConfig(service).getMethodConfig(method);
      Map<String, String> fieldNamePatterns = methodConfig.getFieldNamePatterns();

      ImmutableMap.Builder<String, InitValueConfig> initValueConfigMap = ImmutableMap.builder();
      for (Map.Entry<String, String> fieldNamePattern : fieldNamePatterns.entrySet()) {
        SingleResourceNameConfig resourceNameConfig =
            context.getSingleResourceNameConfig(fieldNamePattern.getValue());
        if (resourceNameConfig != null) {
          InitValueConfig initValueConfig =
              InitValueConfig.create(context.getApiWrapperName(service), resourceNameConfig);
          initValueConfigMap.put(fieldNamePattern.getKey(), initValueConfig);
        }
      }

      InitCodeNode rootNode =
          InitCodeNode.createTree(
              InitCodeContext.newBuilder()
                  .initObjectType(method.getInputType())
                  .initFieldConfigStrings(methodConfig.getSampleCodeInitFields())
                  .initFields(fields)
                  .initValueConfigMap(initValueConfigMap.build())
                  .suggestedName(Name.from("request"))
                  .build());
      List<InitCodeLine> initCodeLines = new ArrayList<>();
      for (InitCodeNode item : rootNode.listInInitializationOrder()) {
        initCodeLines.add(item.getInitCodeLine());
      }
      return initCodeLines;
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

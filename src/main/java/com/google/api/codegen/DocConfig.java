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

import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.TypeRef;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;

public abstract class DocConfig {
  public abstract String getApiName();

  public abstract String getMethodName();

  public abstract String getReturnType();

  // FIXME: is this Java-specific?
  public String getGenericAwareReturnType() {
    String returnType = getReturnType();
    if (returnType == null || returnType.isEmpty()) {
      return "Void";
    } else {
      return returnType;
    }
  }

  public abstract ImmutableList<Variable> getParams();

  public abstract ImmutableList<Variable> getRequiredParams();

  public abstract boolean isPagedVariant();

  public abstract boolean isCallableVariant();

  @Nullable
  public abstract Field getResourcesFieldForUnpagedListCallable();

  public boolean isUnpagedListCallableVariant() {
    return getResourcesFieldForUnpagedListCallable() != null;
  }

  /**
   * Generic variable definition
   */
  @AutoValue
  public abstract static class Variable {
    public abstract TypeRef getType();

    public abstract String getName();

    public abstract String getDescription();

    @Nullable
    public abstract CollectionConfig getFormattingConfig();

    // This function is necessary for use in snippets
    public boolean hasFormattingConfig() {
      return getFormattingConfig() != null;
    }
  }

  /**
   * Allow snippet code to instantiate a Variable object, since snippets can't call static
   * functions.
   */
  public Variable newVariable(TypeRef type, String name, String description) {
    return s_newVariable(type, name, description);
  }

  /**
   * Instantiate a Variable object with no formatting config.
   */
  public static Variable s_newVariable(TypeRef type, String name, String description) {
    return s_newVariable(type, name, description, null);
  }

  /**
   * Instantiate a Variable object.
   */
  public static Variable s_newVariable(
      TypeRef type, String name, String description, CollectionConfig formattingConfig) {
    return new AutoValue_DocConfig_Variable(type, name, description, formattingConfig);
  }
  
  /**
   * DocConfig builder minimum functionality
   */
  public abstract static class Builder<BuilderType extends Builder<BuilderType>> {
    protected abstract BuilderType _setParams(ImmutableList<Variable> params);

    protected abstract BuilderType _setRequiredParams(ImmutableList<Variable> params);

    public BuilderType setParams(GapicContext context, Iterable<Field> fields) {
      return _setParams(fieldsToParams(context, fields));
    }

    public BuilderType setParamsWithFormatting(
        GapicContext context,
        Interface service,
        Iterable<Field> fields,
        ImmutableMap<String, String> fieldNamePatterns) {
      return _setParams(fieldsToParamsWithFormatting(context, service, fields, fieldNamePatterns));
    }

    public BuilderType setSingleParam(
        @SuppressWarnings("unused") GapicContext context,
        TypeRef requestType,
        String name,
        String doc) {
      return _setParams(ImmutableList.of(s_newVariable(requestType, name, doc)));
    }

    public BuilderType setRequiredParams(GapicContext context, Iterable<Field> fields) {
      return _setRequiredParams(fieldsToParams(context, fields));
    }

    public BuilderType setRequiredParamsWithFormatting(
        GapicContext context,
        Interface service,
        Iterable<Field> fields,
        ImmutableMap<String, String> fieldNamePatterns) {
      return _setRequiredParams(
          fieldsToParamsWithFormatting(context, service, fields, fieldNamePatterns));
    }

    public BuilderType setRequiredParamsEmpty() {
      return _setRequiredParams(ImmutableList.<Variable>of());
    }

    private static ImmutableList<Variable> fieldsToParams(
        GapicContext context, Iterable<Field> fields) {
      ImmutableList.Builder<Variable> params = ImmutableList.<Variable>builder();
      for (Field field : fields) {
        params.add(
            s_newVariable(
                field.getType(),
                LanguageUtil.lowerUnderscoreToLowerCamel(field.getSimpleName()),
                context.getDescription(field)));
      }
      return params.build();
    }

    private static ImmutableList<Variable> fieldsToParamsWithFormatting(
        GapicContext context,
        Interface service,
        Iterable<Field> fields,
        ImmutableMap<String, String> fieldNamePatterns) {
      ImmutableList.Builder<Variable> params = ImmutableList.<Variable>builder();
      for (Field field : fields) {
        if (fieldNamePatterns.containsKey(field.getSimpleName())) {
          params.add(
              s_newVariable(
                  field.getType(),
                  LanguageUtil.lowerUnderscoreToLowerCamel(field.getSimpleName()),
                  context.getDescription(field),
                  context.getCollectionConfig(
                      service, fieldNamePatterns.get(field.getSimpleName()))));
        } else {
          params.add(
              s_newVariable(
                  field.getType(),
                  LanguageUtil.lowerUnderscoreToLowerCamel(field.getSimpleName()),
                  context.getDescription(field)));
        }
      }
      return params.build();
    }
  }
}

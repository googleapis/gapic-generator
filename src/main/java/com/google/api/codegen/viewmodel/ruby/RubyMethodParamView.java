/* Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.viewmodel.ruby;

import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.ruby.RubyCommentReformatter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class RubyMethodParamView {

  private final FieldModel field;

  private final boolean isRequired;

  private final boolean isPageSizeParam;

  public static RubyMethodParamView create(MethodConfig methodConfig, FieldModel field) {
    boolean isRequired =
        Iterables.any(
            methodConfig.getRequiredFields(),
            requiredField -> requiredField.getSimpleName().equals(field.getSimpleName()));
    boolean isPageSizeParam =
        methodConfig.isPageStreaming()
            && methodConfig.getPageStreaming().hasPageSizeField()
            && field.equals(methodConfig.getPageStreaming().getPageSizeField());
    return new RubyMethodParamView(field, isRequired, isPageSizeParam);
  }

  private RubyMethodParamView(FieldModel field, boolean isRequired, boolean isPageSizeParam) {
    this.field = field;
    this.isRequired = isRequired;
    this.isPageSizeParam = isPageSizeParam;
  }

  public String name() {
    return field.getNameAsParameterName().toLowerUnderscore();
  }

  public String typeName() {
    if (field.isMap()) {
      return String.format(
          "Hash{%s => %s}",
          new RubyTypeNameConverter(getType().getMapKeyType()).getTypeName(),
          getElementTypeName(getType().getMapValueType()));
    }

    if (field.isRepeated()) {
      return String.format("Array<%s>", getElementTypeName(getType()));
    }

    return getElementTypeName(getType());
  }

  private String getElementTypeName(TypeModel type) {
    String typeName = new RubyTypeNameConverter(type).getTypeName();
    return type.isMessage() ? typeName + " | Hash" : typeName;
  }

  public String defaultValue() {
    return isRequired ? "" : "nil";
  }

  public Iterable<String> docLines() {
    if (isPageSizeParam) {
      return ImmutableList.of(
          "The maximum number of resources contained in the underlying API",
          "response. If page streaming is performed per-resource, this",
          "parameter does not affect the return value. If page streaming is",
          "performed per-page, this determines the maximum number of",
          "resources in a page.");
    }

    ImmutableList.Builder<String> lines = ImmutableList.builder();
    lines.addAll(
        CommonRenderingUtil.getDocLines(
            new RubyCommentReformatter().reformat(field.getScopedDocumentation())));
    TypeModel valueType = field.isMap() ? getType().getMapValueType() : getType();
    if (valueType.isMessage()) {
      lines.add(
          String.format(
              "A hash of the same form as `%s`",
              new RubyTypeNameConverter(valueType).getTypeName()));
      lines.add("can also be provided.");
    }

    return lines.build();
  }

  private TypeModel getType() {
    return field.getType();
  }
}

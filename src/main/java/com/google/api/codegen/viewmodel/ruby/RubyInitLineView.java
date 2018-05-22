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

import com.google.api.codegen.config.ProtoTypeRef;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.metacode.InitCodeLineType;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.metacode.InitFieldConfig;
import com.google.api.codegen.metacode.InitValue;
import com.google.api.codegen.metacode.InitValueConfig;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.ruby.RubyRenderingUtil;
import com.google.api.tools.framework.model.EnumType;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.Map;

public class RubyInitLineView {

  private final InitCodeNode item;

  private final String clientName;

  private final boolean isRequired;

  public static RubyInitLineView createRequired(InitCodeNode item, String clientName) {
    return new RubyInitLineView(item, clientName, true);
  }

  public static RubyInitLineView createOptional(InitCodeNode item, String clientName) {
    return new RubyInitLineView(item, clientName, false);
  }

  private RubyInitLineView(InitCodeNode item, String clientName, boolean isRequired) {
    this.item = item;
    this.clientName = clientName;
    this.isRequired = isRequired;
  }

  public InitCodeLineType type() {
    return item.getLineType();
  }

  public String identifier() {
    String unwrapped =
        Name.from(getInitValueConfig().hasFormattingConfig() ? "formatted" : "")
            .join(item.getIdentifier())
            .toLowerUnderscore();
    return RubyRenderingUtil.wrapReserved(unwrapped);
  }

  public String clientName() {
    return clientName;
  }

  public String value() {
    if (getInitValueConfig().hasSimpleInitialValue()) {
      String value = getInitValueConfig().getInitialValue().getValue();
      switch (getInitValueConfig().getInitialValue().getType()) {
        case Literal:
          return getLiteralValue(item.getType(), value);
        case Random:
          return getRandomValue(value);
        case Variable:
          return value;
        default:
          throw new IllegalArgumentException("Unhandled init value type");
      }
    }

    return getZeroValue(item.getType());
  }

  public Iterable<RubySampleFieldView> fields() {
    ImmutableList.Builder<RubySampleFieldView> fields = ImmutableList.builder();
    for (InitCodeNode child : item.getChildren().values()) {
      fields.add(RubySampleFieldView.createOptional(child));
    }

    return fields.build();
  }

  public Iterable<String> elements() {
    ImmutableList.Builder<String> elements = ImmutableList.builder();
    for (InitCodeNode child : item.getChildren().values()) {
      elements.add(child.getIdentifier().toLowerUnderscore());
    }

    return elements.build();
  }

  public Iterable<Map.Entry<String, String>> entries() {
    Map<String, String> map = new HashMap<>();
    for (Map.Entry<String, InitCodeNode> entry : item.getChildren().entrySet()) {
      map.put(
          getPrimitiveValue(item.getType().getMapKeyType(), entry.getKey()),
          entry.getValue().getIdentifier().toLowerUnderscore());
    }

    return map.entrySet();
  }

  public Iterable<String> docLines() {
    if (getInitValueConfig().hasFormattingConfig() && !item.getType().isRepeated()) {
      return ImmutableList.of();
    }

    if (getInitValueConfig().hasSimpleInitialValue()) {
      return ImmutableList.of();
    }

    if (!isRequired) {
      return ImmutableList.of();
    }

    return ImmutableList.of(String.format("TODO: Initialize +%s+:", identifier()));
  }

  public Iterable<String> formatArgs() {
    ImmutableList.Builder<String> formatArgs = ImmutableList.builder();
    for (String var : getInitValueConfig().getSingleResourceNameConfig().getNameTemplate().vars()) {
      formatArgs.add(getFormatArg(var));
    }

    return formatArgs.build();
  }

  private String getFormatArg(String entityName) {
    if (getInitValueConfig().hasFormattingConfigInitialValues()
        && getInitValueConfig().getResourceNameBindingValues().containsKey(entityName)) {
      InitValue initValue = getInitValueConfig().getResourceNameBindingValues().get(entityName);
      switch (initValue.getType()) {
        case Variable:
        case Literal:
          return initValue.getValue();
        case Random:
          return getRandomValue(initValue.getValue());
        default:
          throw new IllegalArgumentException("Unhandled init value type");
      }
    }

    return String.format("\"[%s]\"", Name.anyLower(entityName).toUpperUnderscore());
  }

  public boolean hasFormattedValue() {
    return getInitValueConfig().hasFormattingConfig() && !item.getType().isRepeated();
  }

  public String functionName() {
    return new RubyPathTemplateView(getInitValueConfig().getSingleResourceNameConfig())
        .formatFunctionName();
  }

  private String getLiteralValue(TypeModel type, String value) {
    if (!type.isEnum()) {
      return getPrimitiveValue(type, value);
    }

    EnumType enumType = ((ProtoTypeRef) type).getProtoType().getEnumType();
    if (!enumType
        .getValues()
        .stream()
        .anyMatch(enumValue -> enumValue.getSimpleName().equals(value))) {
      throw new IllegalArgumentException("Unrecognized enum value: " + value);
    }

    return ":" + value;
  }

  private String getRandomValue(String randomString) {
    ImmutableList.Builder<String> stringParts = ImmutableList.builder();
    for (String token : Splitter.on(",").split(CommonRenderingUtil.stripQuotes(randomString))) {
      if (token.contains(InitFieldConfig.RANDOM_TOKEN)) {
        stringParts.add(getRandomToken(token));
      } else if (!token.isEmpty()) {
        stringParts.add(String.format("\"%s\"", token));
      }
    }

    return Joiner.on(" + ").join(stringParts.build());
  }

  private String getRandomToken(String token) {
    ImmutableList.Builder<String> stringParts = ImmutableList.builder();
    for (String part : Splitter.on(InitFieldConfig.RANDOM_TOKEN).split(token)) {
      stringParts.add(String.format("\"%s\"", part));
    }

    return Joiner.on(" + Time.new.to_i.to_s + ").join(stringParts.build());
  }

  private String getZeroValue(TypeModel type) {
    if (type.isMap()) {
      return "{}";
    }

    if (type.isRepeated()) {
      return "[]";
    }

    if (type.isMessage()) {
      return "{}";
    }

    if (type.isEnum()) {
      return ":"
          + ((ProtoTypeRef) type).getProtoType().getEnumType().getValues().get(0).getSimpleName();
    }

    if (type.isBooleanType()) {
      return "false";
    }

    if (type.isStringType() || type.isBytesType()) {
      return "''";
    }

    if (type.isFloatType() || type.isDoubleType()) {
      return "0.0";
    }

    return "0";
  }

  private String getPrimitiveValue(TypeModel type, String key) {
    if (type.isBooleanType()) {
      return key.toLowerCase();
    }

    if (type.isStringType() || type.isBytesType()) {
      return String.format("\"%s\"", key);
    }

    return key;
  }

  private InitValueConfig getInitValueConfig() {
    return item.getInitValueConfig();
  }
}

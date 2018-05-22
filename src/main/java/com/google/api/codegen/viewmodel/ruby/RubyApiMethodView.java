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
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.ruby.RubyCommentReformatter;
import com.google.api.codegen.viewmodel.GrpcStreamingView;
import com.google.common.collect.ImmutableList;

public class RubyApiMethodView {

  private final GapicMethodContext context;

  public RubyApiMethodView(GapicMethodContext context) {
    this.context = context;
  }

  public String name() {
    // This is defined in grpc/generic/service.rb
    return context
        .getMethodModel()
        .getSimpleName()
        .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
        .replaceAll("([a-z\\d])([A-Z])", "$1_$2")
        .replaceAll("-", "_")
        .toLowerCase();
  }

  public String stubName() {
    return Name.upperCamel(context.getTargetInterface().getSimpleName(), "Stub")
        .toLowerUnderscore();
  }

  public String requestTypeName() {
    return getTypeName(context.getMethodModel().getInputType());
  }

  public String returnTypeName() {
    if (context.getMethodModel().isOutputTypeEmpty()) {
      return "";
    }

    if (grpcStreaming().isResponseStreaming()) {
      return String.format("Enumerable<%s>", getReturnClassName());
    }

    if (context.getMethodConfig().isPageStreaming()) {
      String resourceTypeName =
          new RubyTypeNameConverter(
                  context.getMethodConfig().getPageStreaming().getResourcesField().getType())
              .getTypeName();
      return String.format("Google::Gax::PagedEnumerable<%s>", resourceTypeName);
    }

    if (isLongRunningOperation()) {
      return "Google::Gax::Operation";
    }

    return getReturnClassName();
  }

  public String lroPayloadTypeName() {
    return getTypeName(context.getMethodConfig().getLongRunningConfig().getReturnType());
  }

  public String lroMetadataTypeName() {
    return getTypeName(context.getMethodConfig().getLongRunningConfig().getMetadataType());
  }

  public String requestVariableName() {
    return grpcStreaming().isRequestStreaming() ? "reqs" : "req";
  }

  public RubySampleView sample() {
    return RubySampleView.create(context, name());
  }

  public GrpcStreamingView grpcStreaming() {
    return new GrpcStreamingView(context.getMethodConfig().getGrpcStreamingType());
  }

  public Iterable<String> docLines() {
    return CommonRenderingUtil.getDocLines(
        new RubyCommentReformatter().reformat(context.getMethodModel().getScopedDescription()));
  }

  public Iterable<String> headerRequestParams() {
    return context.getMethodConfig().getHeaderRequestParams();
  }

  public Iterable<String> returnsDocLines() {
    if (context.getMethodConfig().isPageStreaming()) {
      String resourceTypeName =
          new RubyTypeNameConverter(
                  context.getMethodConfig().getPageStreaming().getResourcesField().getType())
              .getTypeName();
      return ImmutableList.of(
          String.format("An enumerable of %s instances.", resourceTypeName),
          "See Google::Gax::PagedEnumerable documentation for other",
          "operations such as per-page iteration or access to the response",
          "object.");
    }

    if (grpcStreaming().isResponseStreaming()) {
      return ImmutableList.of(
          String.format("An enumerable of %s instances.", getReturnClassName()), "");
    }

    return ImmutableList.of();
  }

  public Iterable<RubyMethodParamView> methodParams() {
    ImmutableList.Builder<RubyMethodParamView> methodParams = ImmutableList.builder();
    for (FieldModel field : context.getMethodConfig().getRequiredFields()) {
      methodParams.add(RubyMethodParamView.create(context.getMethodConfig(), field));
    }

    for (FieldModel field : context.getMethodConfig().getOptionalFields()) {
      if (!context.getMethodConfig().isPageStreaming()
          || !field.equals(context.getMethodConfig().getPageStreaming().getRequestTokenField())) {
        methodParams.add(RubyMethodParamView.create(context.getMethodConfig(), field));
      }
    }

    return methodParams.build();
  }

  public boolean isLongRunningOperation() {
    return context.getMethodConfig().isLongRunningOperation();
  }

  public boolean hasEmptyReturn() {
    return context.getMethodModel().isOutputTypeEmpty();
  }

  private String getReturnClassName() {
    return getTypeName(context.getMethodModel().getOutputType());
  }

  private String getTypeName(TypeModel type) {
    if (type.isMap()) {
      return "Hash";
    }

    if (type.isRepeated()) {
      return "Array";
    }

    return new RubyTypeNameConverter(type).getTypeName();
  }
}

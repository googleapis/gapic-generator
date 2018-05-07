/* Copyright 2017 Google LLC
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
package com.google.api.codegen.transformer;

import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.ProtoField;
import com.google.api.codegen.config.TransportProtocol;
import com.google.api.codegen.viewmodel.HeaderRequestParamView;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class HeaderRequestParamTransformer {
  public List<HeaderRequestParamView> generateHeaderRequestParams(MethodContext context) {
    if (!context.getProductConfig().getTransportProtocol().equals(TransportProtocol.GRPC)) {
      return ImmutableList.of();
    }

    GapicMethodConfig methodConfig = (GapicMethodConfig) context.getMethodConfig();
    Method method = methodConfig.getMethod();
    SurfaceNamer namer = context.getNamer();
    if (method.getInputType() == null || !method.getInputType().isMessage()) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<HeaderRequestParamView> headerRequestParams = ImmutableList.builder();
    MessageType inputMessageType = method.getInputType().getMessageType();
    for (String headerRequestParam : methodConfig.getHeaderRequestParams()) {
      headerRequestParams.add(
          generateHeaderRequestParam(headerRequestParam, inputMessageType, namer));
    }

    return headerRequestParams.build();
  }

  private HeaderRequestParamView generateHeaderRequestParam(
      String headerRequestParam, MessageType inputMessageType, SurfaceNamer namer) {
    String[] fieldNameTokens = headerRequestParam.split("\\.");
    ImmutableList.Builder<String> gettersChain = ImmutableList.builder();

    MessageType subMessageType = inputMessageType;
    for (String fieldNameToken : fieldNameTokens) {
      Field matchingField = subMessageType.lookupField(fieldNameToken);
      if (matchingField == null) {
        throw new IllegalArgumentException(
            "Unknown field name token '"
                + fieldNameToken
                + "' in header request param '"
                + headerRequestParam
                + "'");
      }

      String matchingFieldGetter = namer.getFieldGetFunctionName(new ProtoField(matchingField));
      gettersChain.add(matchingFieldGetter);
      if (matchingField.getType() != null && matchingField.getType().isMessage()) {
        subMessageType = matchingField.getType().getMessageType();
      }
    }

    HeaderRequestParamView.Builder headerParam =
        HeaderRequestParamView.newBuilder()
            .fullyQualifiedName(headerRequestParam)
            .gettersChain(gettersChain.build());

    return headerParam.build();
  }
}

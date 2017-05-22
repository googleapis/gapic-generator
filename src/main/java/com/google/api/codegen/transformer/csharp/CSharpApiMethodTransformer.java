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
package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.StaticLangApiMethodTransformer;
import com.google.api.codegen.viewmodel.SimpleParamDocView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.apache.commons.lang3.NotImplementedException;

public class CSharpApiMethodTransformer extends StaticLangApiMethodTransformer {

  @Override
  protected void setServiceResponseTypeName(
      GapicMethodContext context, StaticLangApiMethodView.Builder methodViewBuilder) {
    String responseTypeName =
        context.getTypeTable().getAndSaveNicknameFor(context.getMethod().getOutputType());
    methodViewBuilder.serviceResponseTypeName(responseTypeName);
  }

  @Override
  public List<SimpleParamDocView> getRequestObjectParamDocs(
      GapicMethodContext context, TypeRef typeRef) {
    switch (context.getMethodConfig().getGrpcStreamingType()) {
      case NonStreaming:
        SimpleParamDocView doc =
            SimpleParamDocView.newBuilder()
                .paramName("request")
                .typeName(context.getTypeTable().getAndSaveNicknameFor(typeRef))
                .lines(
                    ImmutableList.of(
                        "The request object containing all of the parameters for the API call."))
                .build();
        return ImmutableList.of(doc);
      case BidiStreaming:
        SimpleParamDocView callSettingsDoc =
            SimpleParamDocView.newBuilder()
                .paramName("callSettings")
                .typeName("CallSettings")
                .lines(ImmutableList.of("If not null, applies overrides to this RPC call."))
                .build();
        SimpleParamDocView streamingSettingsDoc =
            SimpleParamDocView.newBuilder()
                .paramName("streamingSettings")
                .typeName("BidirectionalStreamingSettings")
                .lines(
                    ImmutableList.of("If not null, applies streaming overrides to this RPC call."))
                .build();
        return ImmutableList.of(callSettingsDoc, streamingSettingsDoc);
      default:
        throw new NotImplementedException(
            "Cannot handle streaming type: " + context.getMethodConfig().getGrpcStreamingType());
    }
  }
}

/* Copyright 2016 Google LLC
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

import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.TransportProtocol;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.viewmodel.testing.MockGrpcMethodView;
import com.google.api.codegen.viewmodel.testing.MockServiceUsageView;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** MockServiceTransformer contains helper methods useful for creating mock views. */
public class MockServiceTransformer {
  public List<InterfaceModel> getGrpcInterfacesToMock(
      ApiModel model, GapicProductConfig productConfig) {
    if (!productConfig.getTransportProtocol().equals(TransportProtocol.GRPC)) {
      return ImmutableList.of();
    }

    return model
        .getInterfaces()
        .stream()
        .filter(productConfig::hasInterfaceConfig)
        .filter(InterfaceModel::isReachable)
        .flatMap(i -> getGrpcInterfacesForService(model, productConfig, i).values().stream())
        .collect(ImmutableList.toImmutableList());
  }

  public Map<String, InterfaceModel> getGrpcInterfacesForService(
      ApiModel model, GapicProductConfig productConfig, InterfaceModel apiInterface) {
    if (!productConfig.getTransportProtocol().equals(TransportProtocol.GRPC)) {
      return ImmutableMap.of();
    }
    Map<String, InterfaceModel> interfaces = new LinkedHashMap<>();
    interfaces.put(apiInterface.getFullName(), apiInterface);
    InterfaceConfig interfaceConfig = productConfig.getInterfaceConfig(apiInterface);
    for (MethodConfig methodConfig : interfaceConfig.getMethodConfigs()) {
      String reroute = methodConfig.getRerouteToGrpcInterface();
      if (!Strings.isNullOrEmpty(reroute)) {
        InterfaceModel targetInterface = model.getInterface(reroute);
        interfaces.put(reroute, targetInterface);
      }
    }
    return interfaces;
  }

  public List<MockGrpcMethodView> createMockGrpcMethodViews(InterfaceContext context) {
    if (!context.getProductConfig().getTransportProtocol().equals(TransportProtocol.GRPC)) {
      return ImmutableList.of();
    }
    List<MethodModel> methods = context.getInterfaceMethods();
    ArrayList<MockGrpcMethodView> mocks = new ArrayList<>(methods.size());
    for (MethodModel method : methods) {
      if (context.getMethodConfig(method) == null) {
        continue;
      }
      MethodContext methodContext = context.asRequestMethodContext(method);
      String requestTypeName =
          method.getAndSaveRequestTypeName(methodContext.getTypeTable(), methodContext.getNamer());
      String responseTypeName =
          method.getAndSaveResponseTypeName(methodContext.getTypeTable(), methodContext.getNamer());
      MethodConfig methodConfig = methodContext.getMethodConfig();
      mocks.add(
          MockGrpcMethodView.newBuilder()
              .name(methodContext.getNamer().getApiMethodName(method, VisibilityConfig.PUBLIC))
              .requestTypeName(requestTypeName)
              .responseTypeName(responseTypeName)
              .grpcStreamingType(methodConfig.getGrpcStreamingType())
              .streamHandleTypeName(methodContext.getNamer().getStreamingServerName(method))
              .build());
    }
    return mocks;
  }

  public List<MockServiceUsageView> createMockServices(
      SurfaceNamer namer, ApiModel model, GapicProductConfig productConfig) {
    List<MockServiceUsageView> mockServices = new ArrayList<>();

    for (InterfaceModel apiInterface : getGrpcInterfacesToMock(model, productConfig)) {
      MockServiceUsageView mockService =
          MockServiceUsageView.newBuilder()
              .className(namer.getMockServiceClassName(apiInterface))
              .varName(namer.getMockServiceVarName(apiInterface))
              .implName(namer.getMockGrpcServiceImplName(apiInterface))
              .registerFunctionName(namer.getServerRegisterFunctionName(apiInterface))
              .build();
      mockServices.add(mockService);
    }

    return mockServices;
  }
}

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
package com.google.api.codegen.transformer;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.viewmodel.testing.MockGrpcMethodView;
import com.google.api.codegen.viewmodel.testing.MockServiceUsageView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** MockServiceTransformer contains helper methods useful for creating mock views. */
public class MockServiceTransformer {
  public List<Interface> getGrpcInterfacesToMock(Model model, ApiConfig apiConfig) {
    Map<String, Interface> interfaces = new LinkedHashMap<>();

    for (Interface service : new InterfaceView().getElementIterable(model)) {
      if (!service.isReachable()) {
        continue;
      }
      interfaces.putAll(getGrpcInterfacesForService(model, apiConfig, service));
    }

    return new ArrayList<Interface>(interfaces.values());
  }

  public Map<String, Interface> getGrpcInterfacesForService(
      Model model, ApiConfig apiConfig, Interface service) {
    Map<String, Interface> interfaces = new LinkedHashMap<>();
    interfaces.put(service.getFullName(), service);
    InterfaceConfig interfaceConfig = apiConfig.getInterfaceConfig(service);
    for (MethodConfig methodConfig : interfaceConfig.getMethodConfigs()) {
      String reroute = methodConfig.getRerouteToGrpcInterface();
      if (!Strings.isNullOrEmpty(reroute)) {
        Interface targetInterface = model.getSymbolTable().lookupInterface(reroute);
        interfaces.put(reroute, targetInterface);
      }
    }
    return interfaces;
  }

  public List<MockGrpcMethodView> createMockGrpcMethodViews(SurfaceTransformerContext context) {
    List<Method> methods = context.getInterface().getMethods();
    ArrayList<MockGrpcMethodView> mocks = new ArrayList<>(methods.size());
    for (Method method : methods) {
      MethodTransformerContext methodContext = context.asRequestMethodContext(method);
      String requestTypeName =
          methodContext.getTypeTable().getAndSaveNicknameFor(method.getInputType());
      String responseTypeName =
          methodContext.getTypeTable().getAndSaveNicknameFor(method.getOutputType());
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
      SurfaceNamer namer, Model model, ApiConfig apiConfig) {
    List<MockServiceUsageView> mockServices = new ArrayList<>();

    for (Interface service : getGrpcInterfacesToMock(model, apiConfig)) {
      MockServiceUsageView mockService =
          MockServiceUsageView.newBuilder()
              .className(namer.getMockServiceClassName(service))
              .varName(namer.getMockServiceVarName(service))
              .implName(namer.getMockGrpcServiceImplName(service))
              .registerFunctionName(namer.getServerRegisterFunctionName(service))
              .build();
      mockServices.add(mockService);
    }

    return mockServices;
  }
}

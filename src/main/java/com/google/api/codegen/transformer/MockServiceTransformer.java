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
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.viewmodel.testing.MockGrpcMethodView;
import com.google.api.codegen.viewmodel.testing.MockServiceUsageView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** MockServiceTransformer contains helper methods useful for creating mock views. */
public class MockServiceTransformer {
  public List<Interface> getGrpcInterfacesToMock(Model model, GapicProductConfig productConfig) {
    Map<String, Interface> interfaces = new LinkedHashMap<>();

    for (Interface apiInterface : new InterfaceView().getElementIterable(model)) {
      if (!apiInterface.isReachable()) {
        continue;
      }
      interfaces.putAll(getGrpcInterfacesForService(model, productConfig, apiInterface));
    }

    return new ArrayList<Interface>(interfaces.values());
  }

  public Map<String, Interface> getGrpcInterfacesForService(
      Model model, GapicProductConfig productConfig, Interface apiInterface) {
    Map<String, Interface> interfaces = new LinkedHashMap<>();
    interfaces.put(apiInterface.getFullName(), apiInterface);
    InterfaceConfig interfaceConfig = productConfig.getInterfaceConfig(apiInterface);
    for (MethodConfig methodConfig : interfaceConfig.getMethodConfigs()) {
      String reroute = methodConfig.getRerouteToGrpcInterface();
      if (!Strings.isNullOrEmpty(reroute)) {
        Interface targetInterface = model.getSymbolTable().lookupInterface(reroute);
        interfaces.put(reroute, targetInterface);
      }
    }
    return interfaces;
  }

  public List<MockGrpcMethodView> createMockGrpcMethodViews(InterfaceContext context) {
    List<MethodModel> methods = context.getInterfaceMethods();
    ArrayList<MockGrpcMethodView> mocks = new ArrayList<>(methods.size());
    for (MethodModel method : methods) {
      MethodContext methodContext = context.asRequestMethodContext(method);
      String requestTypeName =
          method.getAndSaveRequestTypeName(methodContext.getTypeTable(), methodContext.getNamer());
      String responseTypeName =
          method.getAndSaveResponseTypeName(methodContext.getTypeTable(), methodContext.getNamer());
      MethodConfig methodConfig = methodContext.getMethodConfig();
      mocks.add(
          MockGrpcMethodView.newBuilder()
              .name(
                  methodContext
                      .getNamer()
                      .getApiMethodName(methodContext.getMethodModel(), VisibilityConfig.PUBLIC))
              .requestTypeName(requestTypeName)
              .responseTypeName(responseTypeName)
              .grpcStreamingType(methodConfig.getGrpcStreamingType())
              .streamHandleTypeName(methodContext.getNamer().getStreamingServerName(method))
              .build());
    }
    return mocks;
  }

  public List<MockServiceUsageView> createMockServices(
      SurfaceNamer namer, Model model, GapicProductConfig productConfig) {
    List<MockServiceUsageView> mockServices = new ArrayList<>();

    for (Interface apiInterface : getGrpcInterfacesToMock(model, productConfig)) {
      MockServiceUsageView mockService =
          MockServiceUsageView.newBuilder()
              .className(namer.getMockServiceClassName(apiInterface.getSimpleName()))
              .varName(namer.getMockServiceVarName(apiInterface.getSimpleName()))
              .implName(namer.getMockGrpcServiceImplName(apiInterface.getSimpleName()))
              .registerFunctionName(namer.getServerRegisterFunctionName(apiInterface))
              .build();
      mockServices.add(mockService);
    }

    return mockServices;
  }
}

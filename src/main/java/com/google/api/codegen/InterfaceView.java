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
package com.google.api.codegen;

import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.protobuf.Api;
import java.util.ArrayList;
import java.util.List;

/**
 * An interface-based view of model, consisting of a strategy for getting the interfaces of the
 * model.
 */
public class InterfaceView implements InputElementView<Interface> {

  /** Gets the interfaces for the apis in the service config. */
  @Override
  public Iterable<Interface> getElementIterable(Model model) {
    List<Interface> interfaces = new ArrayList<>();
    for (Api api : model.getServiceConfig().getApisList()) {
      interfaces.add(model.getSymbolTable().lookupInterface(api.getName()));
    }
    return interfaces;
  }
}

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
package com.google.api.codegen.discovery;

import java.util.ArrayList;
import java.util.List;

import com.google.api.codegen.ApiaryConfig;
import com.google.auto.value.AutoValue;
import com.google.protobuf.Field;
import com.google.protobuf.Method;
import com.google.protobuf.Type;

@AutoValue
public abstract class MessageTypeInfo {

  public static MessageTypeInfo createMessageTypeInfo(
      Type type, Method method, ApiaryConfig apiaryConfig, boolean isRequestType) {
    Builder messageTypeInfo = newBuilder();
    // Unfortunately we need isRequestType to get the right name. method.get(Request|Response)Name()
    // returns a synthetic label used by the DiscoveryImporter if it's a request, but the real name
    // if it's a response...
    if (isRequestType) {
      messageTypeInfo.name(apiaryConfig.getSyntheticNameMapping().get(method.getRequestTypeUrl()));
    } else {
      messageTypeInfo.name(method.getResponseTypeUrl());
    }
    System.out.println(method.getName() + ": " + apiaryConfig.getResources(method.getName()));
    messageTypeInfo.packagePath(method.getName());
    List<TypeInfo> fields = new ArrayList<>();
    for (Field field : type.getFieldsList()) {
      fields.add(TypeInfo.createTypeInfo(field, method, apiaryConfig));
    }
    messageTypeInfo.fields(fields);
    return messageTypeInfo.build();
  }

  public abstract String name();

  /*
   * Always equivalent to the parent method's name. Serves as a utility for an easy way to construct
   * a full resource name.
   */
  public abstract String packagePath();

  public abstract List<TypeInfo> fields();

  public static Builder newBuilder() {
    return new AutoValue_MessageTypeInfo.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {

    public abstract Builder name(String val);

    public abstract Builder packagePath(String val);

    public abstract Builder fields(List<TypeInfo> val);

    public abstract MessageTypeInfo build();
  }
}

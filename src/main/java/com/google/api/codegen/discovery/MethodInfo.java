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

import javax.annotation.Nullable;

import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.DiscoveryImporter;
import com.google.auto.value.AutoValue;
import com.google.protobuf.Field;
import com.google.protobuf.Method;
import com.google.protobuf.Type;

@AutoValue
public abstract class MethodInfo {

  public static final String EMPTY_TYPE_URL = "Empty";

  public static MethodInfo createMethodInfo(Method method, ApiaryConfig apiaryConfig) {
    Builder methodInfo = newBuilder();
    String methodName = method.getName();
    methodInfo.name(methodName);
    // Initialize...
    methodInfo.hasRequest(false);
    methodInfo.requestType(null);
    methodInfo.hasResponse(true);
    methodInfo.responseType(null);

    List<TypeInfo> paramTypes = new ArrayList<>();
    for (String paramName : apiaryConfig.getMethodParams(methodName)) {
      Type type = apiaryConfig.getType(method.getRequestTypeUrl());
      if (paramName == DiscoveryImporter.REQUEST_FIELD_NAME) {
        methodInfo.hasRequest(true);
        methodInfo.requestType(
            MessageTypeInfo.createMessageTypeInfo(type, method, apiaryConfig, true));
        continue;
      }
      Field field = apiaryConfig.getField(type, paramName);
      TypeInfo paramTypeInfo = TypeInfo.createTypeInfo(field, method, apiaryConfig);
      paramTypes.add(paramTypeInfo);
    }
    methodInfo.paramTypes(paramTypes);

    String responseTypeUrl = method.getResponseTypeUrl();
    boolean responseEmpty =
        responseTypeUrl.equals(DiscoveryImporter.EMPTY_TYPE_NAME)
            || responseTypeUrl.equals(EMPTY_TYPE_URL);
    methodInfo.hasResponse(!responseEmpty);
    if (!responseEmpty) {
      methodInfo.responseType(
          MessageTypeInfo.createMessageTypeInfo(
              apiaryConfig.getType(method.getResponseTypeUrl()), method, apiaryConfig, false));
    }
    return methodInfo.build();
  }

  public abstract String name();

  public abstract List<TypeInfo> paramTypes();

  public abstract boolean hasRequest();

  @Nullable
  public abstract MessageTypeInfo requestType();

  public abstract boolean hasResponse();

  @Nullable
  public abstract MessageTypeInfo responseType();

  public static Builder newBuilder() {
    return new AutoValue_MethodInfo.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {

    public abstract Builder name(String val);

    public abstract Builder paramTypes(List<TypeInfo> val);

    public abstract Builder hasRequest(boolean val);

    public abstract Builder requestType(MessageTypeInfo val);

    public abstract Builder hasResponse(boolean val);

    public abstract Builder responseType(MessageTypeInfo val);

    public abstract MethodInfo build();
  }
}

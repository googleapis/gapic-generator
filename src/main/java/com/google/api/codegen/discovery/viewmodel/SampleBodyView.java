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
package com.google.api.codegen.discovery.viewmodel;

import java.util.List;

import javax.annotation.Nullable;

import com.google.api.codegen.discovery.config.AuthType;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class SampleBodyView {

  public abstract String serviceVarName();

  public abstract String serviceTypeName();

  public abstract String methodVerb();

  public abstract List<String> methodNameComponents();

  public abstract String requestVarName();

  public abstract String requestTypeName();

  public abstract boolean hasRequestBody();

  public abstract String requestBodyVarName();

  public abstract String requestBodyTypeName();

  public abstract boolean hasResponse();

  public abstract String responseVarName();

  public abstract String responseTypeName();

  public abstract List<SampleFieldView> fields();

  public abstract List<String> fieldVarNames();

  public abstract boolean isPageStreaming();

  public abstract String resourceFieldName();

  public abstract String resourceGetterName();

  public abstract String resourceVarName();

  public abstract String resourceTypeName();

  public abstract boolean isResourceMap();

  public abstract boolean hasMediaUpload();

  public abstract boolean hasMediaDownload();

  public abstract AuthType authType();

  public abstract String authInstructionsUrl();

  public abstract List<String> authScopes();

  public abstract boolean isAuthScopesSingular();

  // Java specific section...
  @Nullable
  public abstract Boolean isResourceSetterInRequestBody();

  @Nullable
  public abstract String createServiceFuncName();

  // Node.js specific section...
  @Nullable
  public abstract String googleImportVarName();

  @Nullable
  public abstract String pageVarName();

  @Nullable
  public abstract String handlePageVarName();

  @Nullable
  public abstract String authFuncName();

  // Go specific section...
  @Nullable
  public abstract String servicePackageName();

  @Nullable
  public abstract String clientVarName();

  @Nullable
  public abstract List<String> authScopeConsts();

  public static Builder newBuilder() {
    return new AutoValue_SampleBodyView.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {

    public abstract Builder serviceVarName(String val);

    public abstract Builder serviceTypeName(String val);

    public abstract Builder methodVerb(String val);

    public abstract Builder methodNameComponents(List<String> val);

    public abstract Builder requestVarName(String val);

    public abstract Builder requestTypeName(String val);

    public abstract Builder hasRequestBody(boolean val);

    public abstract Builder requestBodyVarName(String val);

    public abstract Builder requestBodyTypeName(String val);

    public abstract Builder hasResponse(boolean val);

    public abstract Builder resourceFieldName(String val);

    public abstract Builder responseVarName(String val);

    public abstract Builder responseTypeName(String val);

    public abstract Builder fields(List<SampleFieldView> val);

    public abstract Builder fieldVarNames(List<String> val);

    public abstract Builder isPageStreaming(boolean val);

    public abstract Builder resourceGetterName(String val);

    public abstract Builder resourceVarName(String val);

    public abstract Builder resourceTypeName(String val);

    public abstract Builder isResourceMap(boolean val);

    public abstract Builder hasMediaUpload(boolean val);

    public abstract Builder hasMediaDownload(boolean val);

    public abstract Builder authType(AuthType val);

    public abstract Builder authInstructionsUrl(String val);

    public abstract Builder authScopes(List<String> val);

    public abstract Builder isAuthScopesSingular(boolean val);

    public abstract Builder isResourceSetterInRequestBody(Boolean val);

    public abstract Builder createServiceFuncName(String val);

    public abstract Builder googleImportVarName(String val);

    public abstract Builder pageVarName(String val);

    public abstract Builder handlePageVarName(String val);

    public abstract Builder authFuncName(String val);

    public abstract Builder servicePackageName(String val);

    public abstract Builder clientVarName(String val);

    public abstract Builder authScopeConsts(List<String> val);

    public abstract SampleBodyView build();
  }
}

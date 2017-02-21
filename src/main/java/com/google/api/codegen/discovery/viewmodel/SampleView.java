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

import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class SampleView implements ViewModel {

  @Override
  public abstract String templateFileName();

  @Override
  public abstract String outputPath();

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  public abstract String apiTitle();

  public abstract String apiName();

  public abstract String apiVersion();

  @Nullable
  public abstract String appName();

  @Nullable
  public abstract ImportSectionView importSection();

  public abstract SampleAuthView auth();

  @Nullable
  public abstract String serviceVarName();

  @Nullable
  public abstract String serviceTypeName();

  public abstract String methodVerb();

  public abstract List<String> methodNameComponents();

  @Nullable
  public abstract String requestVarName();

  @Nullable
  public abstract String requestTypeName();

  public abstract boolean hasRequestBody();

  @Nullable
  public abstract String requestBodyVarName();

  @Nullable
  public abstract String requestBodyTypeName();

  @Nullable
  public abstract List<SampleFieldView> requestBodyFields();

  public abstract boolean hasResponse();

  @Nullable
  public abstract String responseVarName();

  @Nullable
  public abstract String responseTypeName();

  public abstract List<SampleFieldView> requiredFields();

  @Nullable
  public abstract List<SampleFieldView> optionalFields();

  @Nullable
  public abstract List<String> methodCallFieldVarNames();

  public abstract boolean isPageStreaming();

  @Nullable
  public abstract SamplePageStreamingView pageStreaming();

  public abstract boolean hasMediaUpload();

  public abstract boolean hasMediaDownload();

  // Java specific section.
  @Nullable
  public abstract String className();

  @Nullable
  public abstract String createServiceFuncName();

  // Node.js specific section.
  @Nullable
  public abstract String googleImportVarName();

  // Go specific section.
  @Nullable
  public abstract String servicePackageName();

  @Nullable
  public abstract String clientVarName();

  @Nullable
  public abstract String getClientFuncName();

  @Nullable
  public abstract String contextVarName();

  // PHP specific section.
  @Nullable
  public abstract String optParamsVarName();

  // Ruby specific section.
  @Nullable
  public abstract String serviceRequirePath();

  @Nullable
  public abstract String serviceTypeNamespace();

  // C# specific section.
  @Nullable
  public abstract String dataNamespace();

  @Nullable
  public abstract String namespaceName();

  // Python specific section.
  @Nullable
  public abstract String credentialsVarName();

  @Nullable
  public abstract String lastMethodNameComponent();

  @Nullable
  public abstract List<String> methodParamAssigments();

  // JS specific section.
  @Nullable
  public abstract String makeApiCallFuncName();

  @Nullable
  public abstract String initClientFuncName();

  @Nullable
  public abstract String handleClientLoadFuncName();

  @Nullable
  public abstract String updateSignInStatusFuncName();

  @Nullable
  public abstract String handleSignInClickFuncName();

  @Nullable
  public abstract String handleSignOutClickFuncName();

  @Nullable
  public abstract String paramsVarName();

  /**
   * Returns true if there are fields, or if the method is page streaming and the `pageToken` field
   * is in the `params` object.
   *
   * <p>Only intended to be used in the JS sample, where `params` may not contain any required
   * fields, but is necessary if the method is page streaming.
   */
  @Nullable
  public abstract Boolean needParams();

  public static Builder newBuilder() {
    return new AutoValue_SampleView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder templateFileName(String val);

    /*
     * Since the semantics of output path generation differ from GAPIC and
     * discovery, this field should only ever contain the name of the output
     * file. The rest of the path is generated and prefixed in ViewModel, where
     * there is access to the service configuration.
     */
    public abstract Builder outputPath(String val);

    public abstract Builder apiTitle(String val);

    public abstract Builder apiName(String val);

    public abstract Builder apiVersion(String val);

    public abstract Builder appName(String val);

    public abstract Builder importSection(ImportSectionView val);

    public abstract Builder auth(SampleAuthView val);

    public abstract Builder serviceVarName(String val);

    public abstract Builder serviceTypeName(String val);

    public abstract Builder methodVerb(String val);

    public abstract Builder methodNameComponents(List<String> val);

    public abstract Builder requestVarName(String val);

    public abstract Builder requestTypeName(String val);

    public abstract Builder hasRequestBody(boolean val);

    public abstract Builder requestBodyVarName(String val);

    public abstract Builder requestBodyTypeName(String val);

    public abstract Builder requestBodyFields(List<SampleFieldView> val);

    public abstract Builder hasResponse(boolean val);

    public abstract Builder responseVarName(String val);

    public abstract Builder responseTypeName(String val);

    public abstract Builder requiredFields(List<SampleFieldView> val);

    public abstract Builder optionalFields(List<SampleFieldView> val);

    public abstract Builder methodCallFieldVarNames(List<String> val);

    public abstract Builder isPageStreaming(boolean val);

    public abstract Builder pageStreaming(SamplePageStreamingView val);

    public abstract Builder hasMediaUpload(boolean val);

    public abstract Builder hasMediaDownload(boolean val);

    public abstract Builder className(String val);

    public abstract Builder createServiceFuncName(String val);

    public abstract Builder googleImportVarName(String val);

    public abstract Builder servicePackageName(String val);

    public abstract Builder clientVarName(String val);

    public abstract Builder getClientFuncName(String val);

    public abstract Builder contextVarName(String val);

    public abstract Builder optParamsVarName(String val);

    public abstract Builder serviceRequirePath(String val);

    public abstract Builder serviceTypeNamespace(String val);

    public abstract Builder dataNamespace(String val);

    public abstract Builder namespaceName(String val);

    public abstract Builder credentialsVarName(String val);

    public abstract Builder lastMethodNameComponent(String val);

    public abstract Builder methodParamAssigments(List<String> val);

    public abstract Builder paramsVarName(String val);

    public abstract Builder makeApiCallFuncName(String val);

    public abstract Builder initClientFuncName(String val);

    public abstract Builder handleClientLoadFuncName(String val);

    public abstract Builder updateSignInStatusFuncName(String val);

    public abstract Builder handleSignInClickFuncName(String val);

    public abstract Builder handleSignOutClickFuncName(String val);

    public abstract Builder needParams(Boolean val);

    public abstract SampleView build();
  }
}

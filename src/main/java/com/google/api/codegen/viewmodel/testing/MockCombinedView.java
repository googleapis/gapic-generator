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
package com.google.api.codegen.viewmodel.testing;

import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.FileHeaderView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;

/**
 * MockCombinedView gathers unit-test-related classes. Used in languages that idiomatically put
 * mocks and tests in the same file.
 */
@AutoValue
public abstract class MockCombinedView implements ViewModel {
  public abstract FileHeaderView fileHeader();

  public abstract List<MockServiceImplView> serviceImpls();

  public abstract List<ClientTestClassView> testClasses();

  public abstract List<MockServiceUsageView> mockServices();

  public boolean hasGrpcStreaming() {
    for (ClientTestClassView testClass : testClasses()) {
      for (TestCaseView testCase : testClass.testCases()) {
        if (testCase.grpcStreamingType() != GrpcStreamingType.NonStreaming) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean hasLongRunning() {
    for (ClientTestClassView testClass : testClasses()) {
      for (TestCaseView testCase : testClass.testCases()) {
        if (testCase.clientMethodType() == ClientMethodType.OperationCallableMethod) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean hasServerStreaming() {
    return hasStreamingType(GrpcStreamingType.ServerStreaming);
  }

  public boolean hasBidiStreaming() {
    return hasStreamingType(GrpcStreamingType.BidiStreaming);
  }

  @Nullable
  public abstract String localPackageName();

  public abstract boolean packageHasMultipleServices();

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  @Override
  public abstract String templateFileName();

  @Override
  public abstract String outputPath();

  public static Builder newBuilder() {
    return new AutoValue_MockCombinedView.Builder().packageHasMultipleServices(false);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder fileHeader(FileHeaderView val);

    public abstract Builder serviceImpls(List<MockServiceImplView> val);

    public abstract Builder testClasses(List<ClientTestClassView> val);

    public abstract Builder mockServices(List<MockServiceUsageView> val);

    public abstract Builder localPackageName(String val);

    public abstract Builder outputPath(String val);

    public abstract Builder templateFileName(String val);

    public abstract Builder packageHasMultipleServices(boolean val);

    public abstract MockCombinedView build();
  }

  private boolean hasStreamingType(GrpcStreamingType type) {
    for (ClientTestClassView testClass : testClasses()) {
      for (TestCaseView testCase : testClass.testCases()) {
        if (testCase.grpcStreamingType() == type) {
          return true;
        }
      }
    }
    return false;
  }
}

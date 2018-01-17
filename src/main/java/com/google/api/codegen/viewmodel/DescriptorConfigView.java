/* Copyright 2018 Google LLC
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
package com.google.api.codegen.viewmodel;

import com.google.api.codegen.SnippetSetRunner;
import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class DescriptorConfigView implements ViewModel {
  public abstract String templateFileName();

  public abstract List<PageStreamingDescriptorView> pageStreamingDescriptors();

  @Nullable
  public abstract List<BatchingDescriptorView> batchingDescriptors();

  public abstract List<LongRunningOperationDetailView> longRunningDescriptors();

  public abstract List<GrpcStreamingDetailView> grpcStreamingDescriptors();

  public abstract String interfaceKey();

  public abstract String outputPath();

  public boolean hasPageStreamingMethods() {
    return pageStreamingDescriptors().size() > 0;
  }

  public boolean hasBatchingMethods() {
    if (batchingDescriptors() == null) {
      return false;
    }

    return batchingDescriptors().size() > 0;
  }

  public boolean hasLongRunningOperations() {
    return longRunningDescriptors().size() > 0;
  }

  public boolean hasGrpcStreamingMethods() {
    return grpcStreamingDescriptors().size() > 0;
  }

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  public static Builder newBuilder() {
    return new AutoValue_DescriptorConfigView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder templateFileName(String val);

    public abstract Builder pageStreamingDescriptors(List<PageStreamingDescriptorView> val);

    public abstract Builder batchingDescriptors(List<BatchingDescriptorView> val);

    public abstract Builder longRunningDescriptors(List<LongRunningOperationDetailView> val);

    public abstract Builder grpcStreamingDescriptors(List<GrpcStreamingDetailView> val);

    public abstract Builder interfaceKey(String val);

    public abstract Builder outputPath(String val);

    public abstract DescriptorConfigView build();
  }
}

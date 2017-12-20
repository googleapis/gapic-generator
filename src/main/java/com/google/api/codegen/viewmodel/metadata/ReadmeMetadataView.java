/* Copyright 2017 Google LLC
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

package com.google.api.codegen.viewmodel.metadata;

import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class ReadmeMetadataView implements ModuleView {
  public abstract String moduleName();

  @Nullable
  public abstract String identifier();

  public abstract String apiSummary();

  public abstract String fullName();

  public abstract String shortName();

  public abstract String gapicPackageName();

  public abstract String majorVersion();

  public abstract String developmentStatusTitle();

  public abstract boolean hasMultipleServices();

  public abstract String targetLanguage();

  public abstract String mainReadmeLink();

  public abstract String authDocumentationLink();

  public abstract String libraryDocumentationLink();

  public abstract String versioningDocumentationLink();

  public abstract List<ApiMethodView> exampleMethods();

  public static Builder newBuilder() {
    return new AutoValue_ReadmeMetadataView.Builder();
  }

  public String type() {
    return ReadmeMetadataView.class.getSimpleName();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    /** The name of the module this view provides metadata for. */
    public abstract Builder moduleName(String val);

    public abstract Builder identifier(String val);

    /** The descriptive summary of the api. */
    public abstract Builder apiSummary(String val);

    /** The full name of the API, including branding. E.g., "Stackdriver Logging". */
    public abstract Builder fullName(String val);

    /** A single-word short name of the API. e.g., "logging". */
    public abstract Builder shortName(String val);

    /** The base name of the GAPIC client library package. E.g., "gapic-google-cloud-logging-v1". */
    public abstract Builder gapicPackageName(String val);

    /** The major version of the API, as used in the package name. E.g., "v1". */
    public abstract Builder majorVersion(String val);

    /** The developement status of the package used in titles. E.g., "Alpha". */
    public abstract Builder developmentStatusTitle(String val);

    /** Whether the package contains multiple service objects */
    public abstract Builder hasMultipleServices(boolean val);

    /**
     * The language that is being generated; primarily used in titles. First letter is uppercase.
     */
    public abstract Builder targetLanguage(String val);

    /** Link to the main README of the metapackage. */
    public abstract Builder mainReadmeLink(String val);

    /** Link to authentication instructions on github.io. */
    public abstract Builder authDocumentationLink(String val);

    /** Link to the client library documentation on github.io. */
    public abstract Builder libraryDocumentationLink(String val);

    /** Link to the semantic versioning information of the metapackage. */
    public abstract Builder versioningDocumentationLink(String val);

    /** Methods to show smoke test examples for in the readme * */
    public abstract Builder exampleMethods(List<ApiMethodView> val);

    public abstract ReadmeMetadataView build();
  }
}

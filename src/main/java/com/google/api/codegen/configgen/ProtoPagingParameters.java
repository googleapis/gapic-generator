/* Copyright 2017 Google Inc
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
package com.google.api.codegen.configgen;

import com.google.common.collect.ImmutableList;
import java.util.List;

/** Names of paging parameters used by protobuf-defined APIs. */
public class ProtoPagingParameters implements PagingParameters {
  private static final String PARAMETER_PAGE_TOKEN = "page_token";
  private static final String PARAMETER_NEXT_PAGE_TOKEN = "next_page_token";
  private static final String PARAMETER_MAX_RESULTS = "page_size";

  private static final ImmutableList<String> IGNORED_PARAMETERS =
      ImmutableList.of(PARAMETER_PAGE_TOKEN, PARAMETER_MAX_RESULTS);

  @Override
  public String getNameForPageToken() {
    return PARAMETER_PAGE_TOKEN;
  }

  @Override
  public String getNameForPageSize() {
    return PARAMETER_MAX_RESULTS;
  }

  @Override
  public String getNameForNextPageToken() {
    return PARAMETER_NEXT_PAGE_TOKEN;
  }

  @Override
  public List<String> getIgnoredParameters() {
    return IGNORED_PARAMETERS;
  }
}

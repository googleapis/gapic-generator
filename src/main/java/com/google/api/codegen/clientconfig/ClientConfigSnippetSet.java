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
package com.google.api.codegen.clientconfig;

import com.google.api.tools.framework.snippet.Doc;

/** Entry points for a client config snippet set. */
interface ClientConfigSnippetSet<Element> {

  /** Generates the result filename for the generated document. */
  Doc generateFilename(Element service);

  /** Generates the body of the config for the service interface. */
  Doc generateBody(Element service);
}

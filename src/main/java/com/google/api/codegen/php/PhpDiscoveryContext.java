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
package com.google.api.codegen.php;

import com.google.api.Service;
import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.DiscoveryContext;

/**
 * A subclass of DiscoveryContext which is specialized for PHP language.
 */
public class PhpDiscoveryContext extends DiscoveryContext implements PhpContext {

  /**
   * Constructs the PHP discovery context.
   */
  public PhpDiscoveryContext(Service service, ApiaryConfig apiaryConfig) {
    super(service, apiaryConfig);
  }

  @Override
  public void resetState(PhpSnippetSet<?> phpSnippetSet, PhpContextCommon phpCommon) {
    // TODO implement when necessary
  }

}

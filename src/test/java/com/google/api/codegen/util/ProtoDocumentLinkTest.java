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
package com.google.api.codegen.util;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.codegen.util.js.JSCommentReformatter;
import org.junit.Test;

public class ProtoDocumentLinkTest {

  @Test
  public void testJSCommentReformatter() {
    JSCommentReformatter commentReformatter = new JSCommentReformatter();
    assertThat(commentReformatter.reformat("[Shelf][google.example.library.v1.Shelf]"))
        .isEqualTo("Shelf");
    assertThat(commentReformatter.reformat("[$Shelf][google.example.library.v1.Shelf]"))
        .isEqualTo("$Shelf");

    // Cloud link may contain special character '$'
    assertThat(commentReformatter.reformat("[cloud docs!](/library/example/link)"))
        .isEqualTo("[cloud docs!](https://cloud.google.com/library/example/link)");
    assertThat(commentReformatter.reformat("[cloud docs!](/library/example/link$)"))
        .isEqualTo("[cloud docs!](https://cloud.google.com/library/example/link$)");

    // Absolute link may contain special character '$'
    assertThat(commentReformatter.reformat("[not a cloud link](http://www.google.com)"))
        .isEqualTo("[not a cloud link](http://www.google.com)");
    assertThat(commentReformatter.reformat("[not a cloud link](http://www.google.com$)"))
        .isEqualTo("[not a cloud link](http://www.google.com$)");
  }
}

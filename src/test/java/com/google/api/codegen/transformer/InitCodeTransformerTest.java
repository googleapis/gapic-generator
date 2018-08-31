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
package com.google.api.codegen.transformer;

import static org.junit.Assert.fail;

import com.google.api.codegen.metacode.InitCodeLineType;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class InitCodeTransformerTest {
  @Test
  public void testOverlap() {
    InitCodeNode x = InitCodeNode.createWithChildren("x", InitCodeLineType.SimpleInitLine);
    InitCodeNode y = InitCodeNode.createWithChildren("y", InitCodeLineType.SimpleInitLine);

    InitCodeNode a = InitCodeNode.createWithChildren("a", InitCodeLineType.StructureInitLine, x, y);
    InitCodeNode b = InitCodeNode.createWithChildren("b", InitCodeLineType.SimpleInitLine);

    InitCodeNode root =
        InitCodeNode.createWithChildren("root", InitCodeLineType.StructureInitLine, a, b);

    InitCodeTransformer.assertNoOverlap(root, ImmutableList.of("a", "b"));
    InitCodeTransformer.assertNoOverlap(root, ImmutableList.of("a.x", "a.y"));

    assertThrows(() -> InitCodeTransformer.assertNoOverlap(root, ImmutableList.of("a", "a.x")));
    assertThrows(() -> InitCodeTransformer.assertNoOverlap(root, ImmutableList.of("a", "a.y")));
    assertThrows(() -> InitCodeTransformer.assertNoOverlap(root, ImmutableList.of("a", "a")));
    assertThrows(() -> InitCodeTransformer.assertNoOverlap(root, ImmutableList.of("a.x", "a.x")));
    assertThrows(() -> InitCodeTransformer.assertNoOverlap(root, ImmutableList.of("a.y", "a.y")));
  }

  private static void assertThrows(Runnable runnable) {
    try {
      runnable.run();
      fail("should throw");
    } catch (IllegalArgumentException e) {
      // expected.
    }
  }
}

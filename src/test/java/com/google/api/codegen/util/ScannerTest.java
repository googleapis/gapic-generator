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
package com.google.api.codegen.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class ScannerTest {
  @Test
  public void testScanner() {
    Scanner scanner = new Scanner("$abc =   def123 + 456 + \"xyz\";");

    assertThat(scanner.scan()).isEqualTo(Scanner.IDENT);
    assertThat(scanner.token()).isEqualTo("$abc");

    assertThat(scanner.scan()).isEqualTo('=');

    assertThat(scanner.scan()).isEqualTo(Scanner.IDENT);
    assertThat(scanner.token()).isEqualTo("def123");

    assertThat(scanner.scan()).isEqualTo('+');

    assertThat(scanner.scan()).isEqualTo(Scanner.INT);
    assertThat(scanner.token()).isEqualTo("456");

    assertThat(scanner.scan()).isEqualTo('+');

    assertThat(scanner.scan()).isEqualTo(Scanner.STRING);
    assertThat(scanner.token()).isEqualTo("xyz");

    assertThat(scanner.scan()).isEqualTo(';');

    assertThat(scanner.scan()).isEqualTo(Scanner.EOF);
  }

  @Test
  public void testScannerDollar() {
    assertThrow(() -> new Scanner("$$abc").scan());

    {
      Scanner scanner = new Scanner("$a$b$");

      assertThat(scanner.scan()).isEqualTo(Scanner.IDENT);
      assertThat(scanner.token()).isEqualTo("$a");

      assertThat(scanner.scan()).isEqualTo(Scanner.IDENT);
      assertThat(scanner.token()).isEqualTo("$b");

      assertThrow(() -> scanner.scan());
    }

    {
      Scanner scanner = new Scanner("a$$b");

      assertThat(scanner.scan()).isEqualTo(Scanner.IDENT);
      assertThat(scanner.token()).isEqualTo("a");

      assertThrow(() -> scanner.scan());
    }

    {
      Scanner scanner = new Scanner("a$");

      assertThat(scanner.scan()).isEqualTo(Scanner.IDENT);
      assertThat(scanner.token()).isEqualTo("a");

      assertThrow(() -> scanner.scan());
    }
  }

  @Test
  public void testScannerInt() {
    {
      Scanner scanner = new Scanner("123 456");

      assertThat(scanner.scan()).isEqualTo(Scanner.INT);
      assertThat(scanner.token()).isEqualTo("123");

      assertThat(scanner.scan()).isEqualTo(Scanner.INT);
      assertThat(scanner.token()).isEqualTo("456");

      assertThat(scanner.scan()).isEqualTo(Scanner.EOF);
    }

    // Leading zero not allowed.
    assertThrow(() -> new Scanner("0123").scan());
  }

  private void assertThrow(Runnable r) {
    try {
      r.run();
      throw new IllegalStateException("expected exception");
    } catch (IllegalArgumentException e) {
      // success
    }
  }
}

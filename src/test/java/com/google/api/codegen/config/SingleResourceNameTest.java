package com.google.api.codegen.config;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class SingleResourceNameTest {

  @Test
  public void testCreatePathTemplate() {
    assertThat(SingleResourceNameConfig.escapePathTemplate("bookShelves/*/books/{book}")).isEqualTo("bookShelves/{bookShelf}/books/{book}");
    assertThat(SingleResourceNameConfig.escapePathTemplate("bookShelves/{gibberish}/books/{book}")).isEqualTo("bookShelves/{gibberish}/books/{book}");
    assertThat(SingleResourceNameConfig.escapePathTemplate("projects/*/books/{book_id=*}")).isEqualTo("projects/{project}/books/{book_id=*}");
  }
}

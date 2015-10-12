package io.gapi.vgen;

/**
 * Represents a snippet input resource.
 */
public class SnippetDescriptor {

  private final String snippetInputName;

  public SnippetDescriptor(String snippetInputName) {
    this.snippetInputName = snippetInputName;
  }

  public String getSnippetInputName() {
    return snippetInputName;
  }

}

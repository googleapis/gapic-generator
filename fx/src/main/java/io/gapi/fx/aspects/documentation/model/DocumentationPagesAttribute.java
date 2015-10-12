package io.gapi.fx.aspects.documentation.model;

import com.google.api.Page;
import com.google.auto.value.AutoValue;
import com.google.inject.Key;

import java.util.List;

/**
 * An attribute attached to the model to represent normalized documentation pages specified through
 * the documentation configuration.
 */
@AutoValue
public abstract class DocumentationPagesAttribute {

  /**
   * Key used to access this attribute.
   */
  public static final Key<DocumentationPagesAttribute> KEY =
      Key.get(DocumentationPagesAttribute.class);

  /**
   * Returns the top level pages of the docset.
   * Note that comment filtering is not applied to this text; use
   * {@link DocumentationUtil#getScopedToplevelPages} for that.
   */
  public abstract List<Page> toplevelPages();

  /**
   * Create this attribute.
   */
  public static DocumentationPagesAttribute create(List<Page> toplevelPages) {
    return new AutoValue_DocumentationPagesAttribute(toplevelPages);
  }
}

package io.gapi.fx.aspects.documentation.model;

import com.google.auto.value.AutoValue;
import com.google.inject.Key;

/**
 * An attribute attached by this aspect to elements, representing resolved documentation.
 * For the model itself, this attribute represents the overview documentation. For
 * protocol elements, this represents the documentation of the element, merged from
 * proto comments and doc provided via the service config.
 */
@AutoValue
public abstract class ElementDocumentationAttribute {

  /**
   * Key used to access this attribute.
   */
  public static final Key<ElementDocumentationAttribute> KEY =
      Key.get(ElementDocumentationAttribute.class);

  /**
   * The processed documentation of the proto element, with documentation directives resolved.
   * Note that comment filtering is not applied to this text; use
   * {@link DocumentationUtil#getScopedDescription} for that.
   */
  public abstract String documentation();

  /**
   * Create attribute.
   */
  public static ElementDocumentationAttribute create(String doc) {
    return new AutoValue_ElementDocumentationAttribute(doc.trim());
  }
}

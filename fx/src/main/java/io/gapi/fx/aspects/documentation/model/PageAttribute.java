package io.gapi.fx.aspects.documentation.model;

import com.google.auto.value.AutoValue;
import com.google.inject.Key;

/**
 * Attribute for an associated documentation page. The effective page for an element
 * is either directly attached to the element, or to the enclosing proto file.
 */
@AutoValue
public abstract class PageAttribute {

  /**
   * Key used to access this attribute.
   */
  public static final Key<PageAttribute> KEY = Key.get(PageAttribute.class);

  /**
   * The name of the page this element is associated with.
   */
  public abstract String page();

  /**
   * Create attribute.
   */
  public static PageAttribute create(String doc) {
    return new AutoValue_PageAttribute(doc);
  }
}

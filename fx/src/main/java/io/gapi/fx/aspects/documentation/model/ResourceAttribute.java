package io.gapi.fx.aspects.documentation.model;

import com.google.auto.value.AutoValue;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import java.util.List;

/**
 * Attribute indicating a user defined resource-to-collection association. A message type
 * can contain a list of such attributes. Used to override the default heuristic for
 * determining the resource of a collection.
 */
@AutoValue
public abstract class ResourceAttribute {

  /**
   * Key used to access this attribute.
   */
  public static final Key<List<ResourceAttribute>> KEY =
      Key.get(new TypeLiteral<List<ResourceAttribute>>() {});

  /**
   * The collection the attributed message type is a resource for.
   */
  public abstract String collection();

  /**
   * Create attribute.
   */
  public static ResourceAttribute create(String collection) {
    return new AutoValue_ResourceAttribute(collection);
  }
}

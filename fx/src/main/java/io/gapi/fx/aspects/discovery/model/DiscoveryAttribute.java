package io.gapi.fx.aspects.discovery.model;

import com.google.auto.value.AutoValue;
import com.google.inject.Key;

/**
 * An attribute attached by this aspect to the model representing information related to discovery.
 */
@AutoValue
public abstract class DiscoveryAttribute {

  /**
   * Key used to access this attribute.
   */
  public static final Key<DiscoveryAttribute> KEY = Key.get(DiscoveryAttribute.class);

  /**
   * Whether the API enables public directory.
   */
  public abstract boolean publicDirectory();

  /**
   * API name that will be used in the generated discovery doc.
   */
  public abstract String apiName();

  /**
   * Canonical name that will be used in the generated discovery doc.
   */
  public abstract String canonicalName();

  /**
   * Create attribute.
   */
  public static DiscoveryAttribute create(
      boolean publicDirectory, String apiName, String canonicalName) {
    return new AutoValue_DiscoveryAttribute(publicDirectory, apiName, canonicalName);
  }
}

package io.gapi.fx.aspects.versioning.model;

import io.gapi.fx.model.Method;
import com.google.auto.value.AutoValue;
import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * An attribute attached by this aspect to {@link Interface} and {@link Method} elements,
 * representing versioning information.
 */
@AutoValue
public abstract class VersionAttribute {

  /**
   * Key used to access this attribute.
   */
  public static final Key<VersionAttribute> KEY = Key.get(VersionAttribute.class);

  /**
   * A key used to access the version used by usage manager, which may differ from
   * the logical version.
   */
  public static final Key<VersionAttribute> USAGE_MANAGER_KEY =
      Key.get(VersionAttribute.class, Names.named("usage-manager"));

  /**
   * The major version of the interface. Is either obtained from the service config
   * or derived from the package name. If both are provided, they are guaranteed to be
   * consistent.
   */
  public abstract String majorVersion();

  /**
   * Create attribute.
   */
  public static VersionAttribute create(String apiVersion) {
    return new AutoValue_VersionAttribute(apiVersion);
  }
}

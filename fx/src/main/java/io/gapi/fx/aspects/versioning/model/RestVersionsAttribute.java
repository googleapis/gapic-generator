package io.gapi.fx.aspects.versioning.model;

import io.gapi.fx.model.Model;
import com.google.inject.Key;

import java.util.Set;

/**
 * An attribute attached by this aspect to the {@link Model} representing the set of all rest
 * versions used in the API. The rest versions are determined from the path of each method.
 */
public class RestVersionsAttribute {

  public static final Key<RestVersionsAttribute> KEY = Key.get(RestVersionsAttribute.class);

  private final Set<String> versions;

  public RestVersionsAttribute(Set<String> versions) {
    this.versions = versions;
  }

  public Set<String> getVersions() {
    return versions;
  }
}

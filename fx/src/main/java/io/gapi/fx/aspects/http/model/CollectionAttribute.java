package io.gapi.fx.aspects.http.model;

import io.gapi.fx.aspects.versioning.model.ApiVersionUtil;
import io.gapi.fx.model.Element;
import io.gapi.fx.model.Location;
import io.gapi.fx.model.Model;
import io.gapi.fx.model.TypeRef;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * An object representing a REST collection, as derived from analyzing an Api's
 * services and methods. A list of those attributes is attached to the model object.
 */
public class CollectionAttribute extends Element {

  /**
   * The key to access the collections of the model.
   */
  public static final Key<List<CollectionAttribute>> KEY =
      Key.get(new TypeLiteral<List<CollectionAttribute>>() {});

  private final Model model;
  private final String name;
  private final Map<String, RestMethod> methods = Maps.newLinkedHashMap();
  private TypeRef resourceType;
  private CollectionAttribute parent;

  public CollectionAttribute(Model model, String name) {
    this.model = model;
    this.name = name;
  }

  @Override
  public Model getModel() {
    return model;
  }

  @Override
  public Location getLocation() {
    return model.getLocation();
  }

  @Override
  public String getSimpleName() {
    return name;
  }

  @Override
  public String getFullName() {
    return getSimpleName();
  }

  /**
   * Returns the methods associated with this collection.
   */
  public Iterable<RestMethod> getMethods() {
    return methods.values();
  }

  /**
   * Returns the methods associated with this collection and reachable with the current scoper.
   */
  public ImmutableList<RestMethod> getReachableMethods() {
    return FluentIterable.from(methods.values()).filter(new Predicate<RestMethod>() {
      @Override
      public boolean apply(RestMethod method) {
        return method.isReachable();
      }
    }).toList();
  }

  /**
   * Returns the resource type, or null, if none assigned.
   */
  @Nullable
  public TypeRef getResourceType() {
    return resourceType;
  }

  /**
   * Sets the resource type.
   */
  public void setResourceType(@Nullable TypeRef resourceType) {
    this.resourceType = resourceType;
  }

  /**
   * Returns the parent collection, or null if none.
   */
  @Nullable
  public CollectionAttribute getParent() {
    return parent;
  }

  /*
   * Returns the version associated with the collection full name. If there is no version, the
   * default "v1" will be returned.
   */
  public String getVersionWithDefault() {
    return ApiVersionUtil.extractDefaultMajorVersionFromRestName(getFullName());
  }

  /**
   * Returns the full name with version prefix stripped if the full name has it.
   */
  public String getFullNameNoVersion() {
    return ApiVersionUtil.stripVersionFromRestName(getFullName());
  }

  /**
   * Set parent collection.
   */
  public void setParent(CollectionAttribute collection) {
    this.parent = collection;
  }

  /**
   * Add method to collection. Returns null or the old declaration.
   */
  @Nullable
  public RestMethod addMethod(RestMethod method) {
    return methods.put(method.getRestMethodName(), method);
  }
}

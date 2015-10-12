package io.gapi.fx.aspects.http.model;

import io.gapi.fx.aspects.versioning.model.ApiVersionUtil;
import io.gapi.fx.model.Element;
import io.gapi.fx.model.Location;
import io.gapi.fx.model.Method;
import io.gapi.fx.model.Model;
import io.gapi.fx.model.TypeRef;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import javax.annotation.Nullable;

/**
 * Encapsulates information about a REST method derived from an RPC method. Each HttpAttribute has
 * a REST method associated with it.
 */
public class RestMethod extends Element {

  /**
   * Create a new REST method.
   */
  public static RestMethod create(Method method, RestKind kind, String collectionName,
      String customMethodName) {
    return new RestMethod(method, kind, collectionName, customMethodName);
  }

  /**
   * Delivers the primary REST method associated with the given RPC method. Returns null
   * if the method has no HTTP attribute.
   */
  @Nullable
  public static RestMethod getPrimaryRestMethod(Method method) {
    HttpAttribute attrib = method.getAttribute(HttpAttribute.KEY);
    if (attrib == null) {
      return null;
    }
    return attrib.getRestMethod();
  }

  private RestKind restKind;
  private String restCustomMethodName;
  private String restCollectionName = "";

  private final Method method;

  private RestMethod(Method method, RestKind kind, String collectionName, String customMethodName) {
    this.method = method;
    this.restKind = kind;
    this.restCollectionName = collectionName;
    this.restCustomMethodName = customMethodName;
  }

  @Override
  public Location getLocation() {
    return method.getLocation();
  }

  @Override
  public Model getModel() {
    return method.getModel();
  }

  @Override
  public String getFullName() {
    return method.getFullName();
  }

  @Override
  public String toString() {
    return method.getFullName();
  }

  @Override
  public String getSimpleName() {
    return method.getSimpleName();
  }

  /**
   * Get the input type of the underlying method.
   */
  public TypeRef getInputType() {
    return method.getInputType();
  }

  /**
   * Get the output type of the underlying method.
   */
  public TypeRef getOutputType() {
    return method.getOutputType();
  }

  /**
   * Returns the underlying method element.
   */
  public Method getBaseMethod() {
    return method;
  }

  /**
   * Returns the full REST method name.
   */
  public String getRestFullMethodName() {
    if (Strings.isNullOrEmpty(restCollectionName)) {
      // Top-level method without collection
      return getRestMethodName();
    }
    return restCollectionName + "." + getRestMethodName();
  }

  /**
   * Returns the full rest name without the version string.
   */
  public String getRestFullMethodNameNoVersion() {
    return ApiVersionUtil.stripVersionFromRestName(getRestFullMethodName());
  }

  /**
   * Returns if the rest method reachable with current scoper.
   */
  public boolean isReachable() {
    return method.isReachable();
  }

  /**
   * Returns the rest method name.
   */
  public String getRestMethodName() {
    return restKind == RestKind.CUSTOM ? restCustomMethodName : restKind.getMethodName();
  }

  /**
   * Returns the rest custom method name.
   */
  public String getRestCustomMethodName() {
    return restCustomMethodName;
  }

  /**
   * Returns the rest kind.
   */
  public RestKind getRestKind() {
    return restKind;
  }

  /**
   * Returns the rest collection name.
   */
  public String getRestCollectionName() {
    return restCollectionName;
  }

  /**
   * Returns the rest collection name without version prefix.
   */
  public String getRestCollectionNameNoVersion() {
    return ApiVersionUtil.stripVersionFromRestName(restCollectionName);
  }

  /**
   * Sets the rest kind.
   */
  public void setRestKind(RestKind kind) {
    Preconditions.checkArgument(kind != RestKind.CUSTOM);
    this.restKind = kind;
  }

  /**
   * Sets the rest custom method name.
   */
  public void setRestCustomMethodName(String restCustomMethodName) {
    restKind = RestKind.CUSTOM;
    this.restCustomMethodName = restCustomMethodName;
  }

  /**
   * Sets the rest collection name.
   */
  public void setRestCollectionName(String restCollectionName) {
    this.restCollectionName = restCollectionName;
  }

  /**
   * Returns the full name with version prefix stripped if the full name has it.
   */
  public String getVersionWithDefault() {
    return ApiVersionUtil.extractDefaultMajorVersionFromRestName(getRestFullMethodName());
  }
}

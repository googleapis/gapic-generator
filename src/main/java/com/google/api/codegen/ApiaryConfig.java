/* Copyright 2016 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen;

import com.google.api.codegen.discovery.config.AuthType;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Table;
import com.google.protobuf.Field;
import com.google.protobuf.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * ApiaryConfig contains additional information about discovery docs parsed by {@link
 * DiscoveryImporter} that do not easily fit into {@link com.google.api.Service} itself.
 */
public class ApiaryConfig {
  /** Maps method name to an ordered list of parameters that the method takes. */
  private final ListMultimap<String, String> methodParams =
      ArrayListMultimap.<String, String>create();

  /** Maps (type name, field name) to textual description of that field. */
  private final Table<String, String, String> fieldDescription =
      HashBasedTable.<String, String, String>create();

  /** Maps method name to its HTTP method kind. */
  private final Map<String, String> fieldHttpMethod = new HashMap<String, String>();

  /** Maps method name to an ordered list of resources comprising the method namespace. */
  private final ListMultimap<String, String> resources = ArrayListMultimap.<String, String>create();

  /**
   * A pair (type name, field name) is in this table if specified as "additionalProperties" in the
   * discovery doc, indicating that the field type is a map from string to the named type.
   */
  private final Table<String, String, Boolean> additionalProperties =
      HashBasedTable.<String, String, Boolean>create();

  /**
   * Specifies the format of each field. A pair (type name, field name) is in this table if the type
   * of the field is "string" and specific format is given in the discovery doc. The format is one
   * of {"int64", "uint64", "byte", "date", "date-time"}. Note: other string formats from the
   * discovery doc are encoded as types in the Service.
   */
  private final Table<String, String, String> stringFormat =
      HashBasedTable.<String, String, String>create();

  /**
   * Specifies the pattern of each field. The pattern is expressed as a regular expression, like
   * "^projects/[^/]*$". The table is indexed by (type name, field name).
   */
  private final Table<String, String, String> pattern =
      HashBasedTable.<String, String, String>create();

  /** Records whether or not the method allows media upload. */
  private final Set<String> mediaUpload = new HashSet<>();

  /** Records whether or not the method allows media download. */
  private final Set<String> mediaDownload = new HashSet<>();

  /** Maps type name to type (from {@link DiscoveryImporter}). */
  private final Map<String, Type> types = new HashMap<>();

  /** Maps (type, field name) to field. */
  private final Table<Type, String, Field> fields = HashBasedTable.<Type, String, Field>create();

  /*
   * The API title.
   */
  private String apiTitle;

  /*
   * The API name.
   */
  private String apiName;

  /*
   * The API version.
   */
  private String apiVersion;

  /*
   * Whether or not this service should be qualified with its version.
   *
   * Not all client library generators honor this field, but the ones that do
   * require that the version of the API be specified in the package if
   * "version_module" is true.
   */
  private boolean versionModule;

  /**
   * Maps method name to set of auth scope URLs, e.g.,
   * https://www.googleapis.com/auth/cloud-platform.
   */
  private final ListMultimap<String, String> authScopes =
      ArrayListMultimap.<String, String>create();

  /** The service canonical name, or name if no canonical name. */
  private String serviceCanonicalName;

  /** The service version string. */
  private String serviceVersion;

  /** The auth instructions URL. */
  private String authInstructionsUrl;

  /** Maps API names to an AuthType override. */
  private Map<String, AuthType> authOverrides = new HashMap<>();

  /**
   * If present in the scope list, indicates that the API supports application default credentials
   * based auth.
   */
  private static final String CLOUD_PLATFORM_SCOPE =
      "https://www.googleapis.com/auth/cloud-platform";

  /** Returns the auth type supported by the service. */
  public AuthType getAuthType() {
    String key = getServiceCanonicalName();
    if (!Strings.isNullOrEmpty(key) && authOverrides.containsKey(key)) {
      return authOverrides.get(key);
    }
    // If the API has no scopes, then we know it's API key-based.
    if (getAuthScopes().isEmpty()) {
      return AuthType.API_KEY;
    }
    // If there are scopes and cloud platform is one of them, then we can use ADC.
    if (getAuthScopes().containsValue(CLOUD_PLATFORM_SCOPE)) {
      return AuthType.APPLICATION_DEFAULT_CREDENTIALS;
    }
    return AuthType.OAUTH_3L;
  }

  public String getAuthInstructionsUrl() {
    return Strings.nullToEmpty(authInstructionsUrl);
  }

  public ListMultimap<String, String> getMethodParams() {
    return methodParams;
  }

  public Table<String, String, String> getFieldDescription() {
    return fieldDescription;
  }

  public Map<String, String> getFieldHttpMethod() {
    return fieldHttpMethod;
  }

  public ListMultimap<String, String> getResources() {
    return resources;
  }

  public Table<String, String, Boolean> getAdditionalProperties() {
    return additionalProperties;
  }

  public Table<String, String, String> getStringFormat() {
    return stringFormat;
  }

  public Table<String, String, String> getFieldPattern() {
    return pattern;
  }

  public Map<String, Type> getTypes() {
    return types;
  }

  public Table<Type, String, Field> getFields() {
    return fields;
  }

  public ListMultimap<String, String> getAuthScopes() {
    return authScopes;
  }

  public Set<String> getMediaUpload() {
    return mediaUpload;
  }

  public Set<String> getMediaDownload() {
    return mediaDownload;
  }

  public String getApiTitle() {
    return apiTitle;
  }

  public void setApiTitle(String apiTitle) {
    this.apiTitle = apiTitle;
  }

  public String getApiName() {
    return apiName;
  }

  public void setApiName(String apiName) {
    this.apiName = apiName;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  public boolean getVersionModule() {
    return versionModule;
  }

  public void setVersionModule(boolean versionModule) {
    this.versionModule = versionModule;
  }

  public String getServiceCanonicalName() {
    return serviceCanonicalName;
  }

  public void setServiceCanonicalName(String serviceCanonicalName) {
    this.serviceCanonicalName = serviceCanonicalName;
  }

  public String getServiceVersion() {
    return serviceVersion;
  }

  public void setServiceVersion(String serviceVersion) {
    this.serviceVersion = serviceVersion;
  }

  public void setAuthInstructionsUrl(String authInstructionsUrl) {
    this.authInstructionsUrl = authInstructionsUrl;
  }

  /** @return the ordered list of parameters accepted by the given method */
  public List<String> getMethodParams(String methodName) {
    return methodParams.get(methodName);
  }

  /** @return the textual description corresponding to the given type name and field name */
  public String getDescription(String typeName, String fieldName) {
    return fieldDescription.get(typeName, fieldName);
  }

  /** @return the HTTP method kind corresponding to the given type name and field name */
  public String getHttpMethod(String methodName) {
    return fieldHttpMethod.get(methodName);
  }

  /** @return the ordered list of resources comprising the namespace of the given method */
  public List<String> getResources(String methodName) {
    return resources.get(methodName);
  }

  /** @return true if the given type name and field name appear as "additionalProperties" */
  @Nullable
  public Boolean getAdditionalProperties(String typeName, String fieldName) {
    return additionalProperties.get(typeName, fieldName);
  }

  /** @return the string format corresponding to the given type name and field name */
  public String getStringFormat(String typeName, String fieldName) {
    return stringFormat.get(typeName, fieldName);
  }

  /** @return type with given name */
  public Type getType(String typeName) {
    return types.get(typeName);
  }

  /** @return field of given type with given field name */
  public Field getField(Type type, String fieldName) {
    return fields.get(type, fieldName);
  }

  /** @return true if method has any auth scopes */
  public boolean hasAuthScopes(String methodName) {
    return authScopes.containsKey(methodName);
  }

  /** @return list of auth scopes for method of given name */
  public List<String> getAuthScopes(String methodName) {
    return authScopes.get(methodName);
  }
}

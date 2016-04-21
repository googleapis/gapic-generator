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
package io.gapi.vgen;

import com.google.api.Service;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Api;
import com.google.protobuf.Field;
import com.google.protobuf.Method;
import com.google.protobuf.Type;

import java.util.List;

import javax.annotation.Nullable;

/**
 * A LanguageContext that provides helpers specific to the Discovery use case.
 */
public abstract class DiscoveryContext extends LanguageContext {

  private final Service service;
  private final ApiaryConfig apiaryConfig;

  /**
   * Constructs the abstract instance of the language provider.
   */
  protected DiscoveryContext(Service service, ApiaryConfig apiaryConfig) {
    this.service = Preconditions.checkNotNull(service);
    this.apiaryConfig = Preconditions.checkNotNull(apiaryConfig);
  }

  /**
   * Returns the associated service.
   */
  public Service getService() {
    return service;
  }

  /**
   * Returns the associated config.
   */
  public ApiaryConfig getApiaryConfig() {
    return apiaryConfig;
  }

  /**
   * A map from method IDs in discovery doc to renamed counterparts in Java client libraries.
   */
  protected static ImmutableMap<String, String> RENAMED_METHOD_MAP;

  // Helpers for Subclasses and Snippets
  // ===================================

  // Note the below methods are instance-based, even if they don't depend on instance state,
  // so they can be accessed by templates.

  public Api getApi() {
    return this.getService().getApis(0);
  }

  public String getApiRevision() {
    return service.getDocumentation().getOverview();
  }

  /**
   * Returns the simple name of the method with given ID.
   */
  public String getMethodName(Method method) {
    return getSimpleName(getRename(method.getName(), RENAMED_METHOD_MAP));
  }

  public String getSimpleName(String name) {
    return name.substring(name.lastIndexOf('.') + 1);
  }

  @Nullable
  public Field getFirstRepeatedField(Type type) {
    for (Field field : type.getFieldsList()) {
      if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
        return field;
      }
    }
    return null;
  }

  @Nullable
  public Field getField(Type type, String fieldName) {
    return apiaryConfig.getField(type, fieldName);
  }

  public boolean isMapField(Type type, String fieldName) {
    return apiaryConfig.getAdditionalProperties(type.getName(), fieldName) != null;
  }

  public boolean isRequestField(String fieldName) {
    return fieldName.equals(DiscoveryImporter.REQUEST_FIELD_NAME);
  }

  public boolean hasRequestField(Method method) {
    // used to handle inconsistency in list methods for Cloud Monitoring API
    // remove if inconsistency is resolved in discovery docs
    if (isCloudMonitoringListMethod(method)) {
      return false;
    }

    List<String> params = apiaryConfig.getMethodParams(method.getName());
    return params.size() > 0 && isRequestField(getLast(params));
  }

  @Nullable
  public Field getRequestField(Method method) {
    return getField(
        apiaryConfig.getType(method.getRequestTypeUrl()), DiscoveryImporter.REQUEST_FIELD_NAME);
  }

  public boolean hasMediaUpload(Method method) {
    return apiaryConfig.getMediaUpload().contains(method.getName());
  }

  public List<String> getMethodParams(Method method) {
    // used to handle inconsistency in list methods for Cloud Monitoring API
    // remove if inconsistency is resolved in discovery docs
    if (isCloudMonitoringListMethod(method)) {
      return getMost(apiaryConfig.getMethodParams(method.getName()));
    }

    return apiaryConfig.getMethodParams(method.getName());
  }

  public List<String> getFlatMethodParams(Method method) {
    if (hasRequestField(method)) {
      return getMost(getMethodParams(method));
    } else {
      return getMethodParams(method);
    }
  }

  public String getResourcesName(Field resourceField) {
    // TODO(tcoffee): verify this rule holds outside of Cloud APIs
    if (resourceField.getName().equals("items")) {
      return "Items";
    } else {
      return lowerCamelToUpperCamel(resourceField.getName());
    }
  }

  public boolean isResponseEmpty(Method method) {
    return method.getResponseTypeUrl().equals(DiscoveryImporter.EMPTY_TYPE_NAME);
  }

  public boolean isPageStreaming(Method method) {
    // used to handle inconsistency in users list method for SQLAdmin API
    // remove if inconsistency is resolved
    if (isSQLAdminUsersListMethod(method)) {
      return false;
    }

    if (isResponseEmpty(method)) {
      return false;
    }
    for (Field field : apiaryConfig.getType(method.getResponseTypeUrl()).getFieldsList()) {
      if (field.getName().equals("nextPageToken")) {
        return true;
      }
    }
    return false;
  }

  public boolean isPatch(Method method) {
    return apiaryConfig.getHttpMethod(method.getName()).equals("PATCH");
  }

  // Flaggers for Exceptional Inconsistencies
  // ========================================

  // used to handle inconsistency in list methods for Cloud Monitoring API
  // remove if inconsistency is resolved in discovery docs
  public boolean isCloudMonitoringListMethod(Method method) {
    Api api = getApi();
    return api.getName().equals("cloudmonitoring")
        && api.getVersion().equals("v2beta2")
        && isPageStreaming(method);
  }

  // used to handle inconsistency in log entries list method for Logging API
  // remove if inconsistency is resolved
  public boolean isLogEntriesListMethod(Method method) {
    Api api = getApi();
    return api.getName().equals("logging")
        && api.getVersion().equals("v2beta1")
        && method.getName().equals("logging.entries.list");
  }

  // used to handle inconsistency in users list method for SQLAdmin API
  // remove if inconsistency is resolved
  public boolean isSQLAdminUsersListMethod(Method method) {
    Api api = getApi();
    return api.getName().equals("sqladmin")
        && api.getVersion().equals("v1beta4")
        && method.getName().equals("sql.users.list");
  }
}

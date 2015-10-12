package io.gapi.fx.aspects.http.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * System parameter defined in ESF. The list is FINAL and should not be changed.
 * See go/esf-special-request-params for details.
 */
public enum SystemParameter {

  ACCESS_TOKEN(
      "access_token",
      "OAuth access token."),
  ALT(
      "alt",
      "Data format for response.",
      "json",
      "query",
      "string",
      ImmutableMap.of(
          "json", "Responses with Content-Type of application/json",
          "media", "Media download with context-dependent Content-Type",
          "proto", "Responses with Content-Type of application/x-protobuf")),
  BEARER_TOKEN(
      "bearer_token",
      "OAuth bearer token."),
  CALLBACK(
      "callback",
      "JSONP"),
  FIELDS(
      "fields",
      "Selector specifying which fields to include in a partial response."),
  KEY("key",
      "API key. Your API key identifies your project and provides you with API access, quota, and"
      + " reports. Required unless you provide an OAuth 2.0 token."),
  OAUTH_TOKEN(
      "oauth_token",
      "OAuth 2.0 token for the current user."),
  PASSWD(
      "passwd",
      "Password, used only as a safeguard."),
  PASSWORD(
      "password",
      "Password, used only as a safeguard."),
  PP(
      "pp",
      "Pretty-print response.",
      "true",
      "query",
      "boolean",
      ImmutableMap.<String, String>of()),
  PRETTY_PRINT(
      "prettyPrint",
      "Returns response with indentations and line breaks.",
      "true",
      "query",
      "boolean",
      ImmutableMap.<String, String>of()),
  QUOTA_USER(
      "quotaUser",
      "Available to use for quota purposes for server-side applications. Can be any arbitrary "
      + "string assigned to a user, but should not exceed 40 characters."),
  UPLOAD_PROTOCOL(
      "upload_protocol",
      "Upload protocol for media (e.g. \\\"raw\\\", \\\"multipart\\\")."
  ),
  UPLOAD_TYPE(
      "uploadType",
      "Legacy upload protocol for media (e.g. \\\"media\\\", \\\"multipart\\\")."
  ),
  // prefix with $ since codegen disallows ".xgafv". OnePlatform API users actually cannot define
  // parameter with dot prefix, so this will not break validation for user-defined parameter.
  XGAFV(
      "$.xgafv",
      "V1 error format.",
      "",
      "query",
      "string",
      ImmutableMap.<String, String>of(
          "1", "v1 error format",
          "2", "v2 error format"));

  private static final Map<String, SystemParameter> lookup = Maps.newLinkedHashMap();

  static {
    for (SystemParameter param : SystemParameter.values()) {
      lookup.put(param.paramName(), param);
    }
  }

  private String name;

  private String description;

  private String location;

  private String type;

  private String defaultValue;

  // Map between enum value and enum description.
  private Map<String, String> enums;

  private SystemParameter(String name, String description, String defaultValue, String location,
      String type, Map<String, String> enums) {
    this.name = name;
    this.description = description;
    this.defaultValue = defaultValue;
    this.location = location;
    this.type = type;
    this.enums = enums;
  }

  private SystemParameter(String name, String description) {
    this(name, description, "", "query", "string", ImmutableMap.<String, String>of());
  }

  /**
   * The name of this system parameter.
   */
  public String paramName() {
    return name;
  }

  /**
   * The type of this system parameter.
   */
  public String type() {
    return type;
  }

  /**
   * The description of this system parameter.
   */
  public String description() {
    return description;
  }

  /**
   * The default value of this system parameter.
   */
  public String defaultValue() {
    return defaultValue;
  }

  /**
   * The location of this system parameter.
   */
  public String location() {
    return location;
  }

  /**
   * Returns true if this system parameter has enums.
   */
  public boolean hasEnums() {
    return !enums.isEmpty();
  }

  /**
   * Iterates through enum values of this system parameter. It keeps the same iteration order of
   * {@link SystemParameter#enumDescriptions()}.
   */
  public Iterable<String> enumValues() {
    return enums.keySet();
  }

  /**
   * Iterates through enum descriptions of this system parameter. It keeps the same iteration
   * order of {@link SystemParameter#enumValues()}.
   */
  public Iterable<String> enumDescriptions() {
    return enums.values();
  }

  /**
   * Returns true if the parameter is a system parameter.
   */
  public static boolean isSystemParameter(String name) {
    return lookup.containsKey(name);
  }

  /**
   * Returns all system parameters.
   */
  public static Iterable<SystemParameter> allSystemParameters() {
    return lookup.values();
  }
}

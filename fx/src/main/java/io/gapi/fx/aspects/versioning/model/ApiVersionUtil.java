package io.gapi.fx.aspects.versioning.model;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Util class that handles api version in //google/protobuf/api.proto
 */
public class ApiVersionUtil {
  public static final Pattern MAJOR_VERSION_REGEX_PATTERN =
      Pattern.compile("^(v\\d+(\\w+)?)$");

  public static final Pattern SEMANTIC_VERSION_REGEX_PATTERN =
      Pattern.compile("^(v\\d+(\\w+)?)(\\.\\d+){0,2}$"); // major-version.minor-version

  /**
   * Extract major version from package name. The major version is reflected in the package name of
   * the API, which must end in `v{major-version}`, as in `google.feature.v1`. For major versions 0
   * and 1, the suffix can be omitted. For that case, `v1` is returned.
   */
  public static String extractDefaultMajorVersionFromPackageName(String packageName) {
    String[] segs = packageName.split("\\.");
    String lastSeg = segs[segs.length - 1];
    Matcher matcher = MAJOR_VERSION_REGEX_PATTERN.matcher(lastSeg);
    if (matcher.find()) {
      return matcher.group(1);
    } else {
      return "v1";
    }
  }

  /**
   * Extract major version from REST element name. Rest element name has the form of "v1.foo.bar"
   * or "foo.bar". For the later case, "v1" will be returned.
   */
  public static String extractDefaultMajorVersionFromRestName(String restName) {
    Preconditions.checkNotNull(restName);
    String [] segs = restName.split("\\.");
    if (segs.length == 0) {
      return "v1";
    }
    Matcher matcher = MAJOR_VERSION_REGEX_PATTERN.matcher(segs[0]);
    return matcher.find() ? matcher.group(1) : "v1";
  }

  /**
   * Returns the REST name with version prefix stripped if it has.
   */
  public static String stripVersionFromRestName(String restName) {
    Preconditions.checkNotNull(restName);
    String version = extractDefaultMajorVersionFromRestName(restName) + ".";
    return restName.startsWith(version) ? restName.substring(version.length()) : restName;
  }

  /**
   * Return major version of the given semantic version. For example, `v2` is returned from `v2.10`.
   * Return null if major version cannot be extracted.
   */
  @Nullable
  public static String extractMajorVersionFromSemanticVersion(String semanticVersion) {
    Matcher matcher = SEMANTIC_VERSION_REGEX_PATTERN.matcher(semanticVersion);
    if (matcher.find()) {
      return matcher.group(1);
    } else {
      return null;
    }
  }

  /**
   *  Return true if apiVersion is a valid semantic version (http://semver.org). Otherwise, false.
   */
  public static boolean isValidApiVersion(String apiVersion) {
    return SEMANTIC_VERSION_REGEX_PATTERN.matcher(apiVersion).matches();
  }

  /**
   *  Return true if apiVersion has a valid major version format.
   */
  public static boolean isValidMajorVersion(String apiVersion) {
    return MAJOR_VERSION_REGEX_PATTERN.matcher(apiVersion).matches();
  }

  /**
   * Append version suffix, if defined, to the given apiVersion.
   */
  public static String appendVersionSuffix(String apiVersion, String versionSuffix) {
    return Strings.isNullOrEmpty(versionSuffix) ? apiVersion : apiVersion + versionSuffix;
  }
}

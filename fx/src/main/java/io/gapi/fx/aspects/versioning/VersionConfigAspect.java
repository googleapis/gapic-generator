package io.gapi.fx.aspects.versioning;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.google.protobuf.Api;

import io.gapi.fx.aspects.ConfigAspectBase;
import io.gapi.fx.aspects.http.HttpConfigAspect;
import io.gapi.fx.aspects.http.model.HttpAttribute;
import io.gapi.fx.aspects.http.model.HttpAttribute.LiteralSegment;
import io.gapi.fx.aspects.versioning.model.ApiVersionUtil;
import io.gapi.fx.aspects.versioning.model.RestVersionsAttribute;
import io.gapi.fx.aspects.versioning.model.VersionAttribute;
import io.gapi.fx.model.ConfigAspect;
import io.gapi.fx.model.Interface;
import io.gapi.fx.model.Method;
import io.gapi.fx.model.Model;
import io.gapi.fx.model.ProtoElement;
import io.gapi.fx.model.SimpleLocation;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Configuration aspect for versioning.
 */
public class VersionConfigAspect extends ConfigAspectBase {

  public static final Key<String> KEY = Key.get(String.class, Names.named("version"));

  public static VersionConfigAspect create(Model model) {
    return new VersionConfigAspect(model);
  }

  private VersionConfigAspect(Model model) {
    super(model, "versioning");
    registerLintRule(new ConfigVersionRule(this));
    registerLintRule(new HttpVersionRule(this));
  }

  /**
   * Returns dependencies. Depends on http attributes used for determining the version
   * from the HTTP path.
   */
  @Override
  public List<Class<? extends ConfigAspect>> mergeDependencies() {
    return ImmutableList.<Class<? extends ConfigAspect>>of(HttpConfigAspect.class);
  }

  @Override
  public void startMerging() {
    // Verify the config_version is explicitly specified in the service config.
    if (!getModel().getServiceConfig().hasConfigVersion()) {
      error(SimpleLocation.TOPLEVEL, "config_version is not specified in the service config file.");
    }

    // Verify that config_version 0 is only allowed in whitelisted services.
    if (getModel().getConfigVersion() == 0
        && !ConfigVersionUtil.isWhitelistedForConfigVersion0(
            getModel().getServiceConfig().getName())) {
      error(
          SimpleLocation.TOPLEVEL,
          "config_version 0 is no longer supported unless your service is whitelisted at "
          + ConfigVersionUtil.CONFIG_VERSION_0_WHITELIST_FILE);
    }
    if (getModel().getConfigVersion() > Model.getDevConfigVersion()) {
      error(
          SimpleLocation.TOPLEVEL,
          String.format("config_version %s is invalid, the latest config_version is %s.",
              getModel().getConfigVersion(), Model.getDevConfigVersion()));
    }
  }

  @Override
  public void merge(ProtoElement element) {
    if (element instanceof Interface) {
      merge((Interface) element);
    }
    if (element instanceof Method) {
      merge((Method) element);
    }
  }

  private void merge(Interface iface) {
    Api api = iface.getConfig();
    if (api == null) {
        return;
    }
    // Get user-defined api version, which is optional.
    String apiVersion = api.getVersion();
    String packageName = iface.getFile().getFullName();
    if (Strings.isNullOrEmpty(apiVersion)) {
      // If version is not provided by user, extract major version from package name.
      apiVersion = ApiVersionUtil.extractDefaultMajorVersionFromPackageName(packageName);
    } else {
      // Validate format of user-defined api version .
      if (!ApiVersionUtil.isValidApiVersion(apiVersion)) {
        error(iface, "Invalid version '%s' defined in API '%s'.", apiVersion, api.getName());
      }

      // Validate that the version in the package name is consistent with what user provides.
      String apiVersionFromPackageName =
          ApiVersionUtil.extractDefaultMajorVersionFromPackageName(packageName);
      if (!apiVersionFromPackageName.equals(
          ApiVersionUtil.extractMajorVersionFromSemanticVersion(apiVersion))) {
        error(iface,
            "User-defined api version '%s' is inconsistent with the one in package name '%s'.",
            apiVersion, packageName);
      }
    }
    iface.putAttribute(VersionAttribute.KEY, VersionAttribute.create(apiVersion));
  }

  private void merge(Method method) {
    String restVersion = deriveApiVersion(method);
    method.putAttribute(VersionAttribute.KEY, VersionAttribute.create(restVersion));
    // UM uses the logical version with a suffix appended, if defined.
    String versionSuffix = method.getModel().getApiV1VersionSuffix();
    method.putAttribute(VersionAttribute.USAGE_MANAGER_KEY,
        VersionAttribute.create(ApiVersionUtil.appendVersionSuffix(restVersion, versionSuffix)));

    if (getModel().hasAttribute(RestVersionsAttribute.KEY)) {
      getModel().getAttribute(RestVersionsAttribute.KEY).getVersions().add(restVersion);
    } else {
      getModel().putAttribute(RestVersionsAttribute.KEY,
          new RestVersionsAttribute(new LinkedHashSet<>(ImmutableList.of(restVersion))));
    }
  }

  @SuppressWarnings("deprecation")
  private String deriveApiVersion(Method element) {
    // TODO: MIGRATION
    /*
    // Support legacy legacy config...
    if (element.getModel().getLegacyConfig() != null
        && element.getModel().getLegacyConfig().hasVersion()) {
      return element.getModel().getLegacyConfig().getVersion();
    }
    */

    // Derive the version from the prefix of the http path. Validation of
    // syntax of version in path happens elsewhere, so we take just the first path segment
    // literal. If none is given, assume 'v1'.
    HttpAttribute http = element.getAttribute(HttpAttribute.KEY);
    if (http == null || http.getPath().isEmpty()
        || !(http.getPath().get(0) instanceof LiteralSegment)) {
      return "v1";
    }
    return ((LiteralSegment) http.getPath().get(0)).getLiteral();
  }
}

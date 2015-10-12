package io.gapi.fx.aspects.versioning;

import io.gapi.fx.aspects.ConfigAspectBase;
import io.gapi.fx.aspects.LintRule;
import io.gapi.fx.aspects.http.model.HttpAttribute;
import io.gapi.fx.aspects.http.model.HttpAttribute.LiteralSegment;
import io.gapi.fx.aspects.http.model.HttpAttribute.PathSegment;
import io.gapi.fx.aspects.versioning.model.ApiVersionUtil;
import io.gapi.fx.aspects.versioning.model.VersionAttribute;
import io.gapi.fx.model.Interface;
import io.gapi.fx.model.Method;
import com.google.common.base.Strings;

/**
 * Style rule for HTTP path version.
 */
class HttpVersionRule extends LintRule<Method> {

  HttpVersionRule(ConfigAspectBase aspect) {
    super(aspect, "http-version-prefix", Method.class);
  }

  @Override public void run(Method method) {
    if (!method.hasAttribute(HttpAttribute.KEY)) {
      return;
    }
    HttpAttribute httpBinding = method.getAttribute(HttpAttribute.KEY);
    String version = null;
    PathSegment firstPathSeg = httpBinding.getPath().get(0);
    if (firstPathSeg instanceof LiteralSegment) {
      if (ApiVersionUtil.isValidMajorVersion(((LiteralSegment) firstPathSeg).getLiteral())) {
        version = ((LiteralSegment) firstPathSeg).getLiteral();

        // Retrieve api version defined in service config.
        String apiVersion =
            ((Interface) method.getParent()).getAttribute(VersionAttribute.KEY).majorVersion();
        if (!Strings.isNullOrEmpty(apiVersion) && !version.equals(apiVersion)) {
          warning(method,
              "method '%s' has a different version prefix in HTTP path ('%s') than api "
              + "version '%s'.",
              method.getFullName(), version, apiVersion);
        }
      }
    }

    if (version == null) {
      warning(
          method,
          "'method %s' has a HTTP path that does not start with version, which must match '%s'.",
          method.getFullName(),
          ApiVersionUtil.MAJOR_VERSION_REGEX_PATTERN.pattern());
    }
  }
}

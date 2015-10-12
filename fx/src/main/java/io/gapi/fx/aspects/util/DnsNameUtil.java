package io.gapi.fx.aspects.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for service DNS name.
 */
public class DnsNameUtil {
  // Pattern for extracting API base name.
  // The non-prod expected DNS name pattern has changed from
  // {apiname}-{env}.sandbox.* to {env}-{apiname}.sandbox.*
  // See here for more details: https://g3doc.corp.google.com/google/api/doc/namespaces.md
  private static final Pattern SERVICE_NAME_PATTERN =
      Pattern.compile("^(staging-|test-)?(?<corp>.+)(\\.corp|-corp)(\\.sandbox)?\\.[^.]+\\.[^.]+$"
          + "|^(staging-|test-)(?<sandboxed>.+)\\.sandbox\\.[^.]+\\.[^.]+$"
          + "|^(?<legacySandboxed>.+)(-staging\\.sandbox|-test\\.sandbox)\\.[^.]+\\.[^.]+$"
          + "|^(?<regular>.+)\\.[^.]+\\.[^.]+$");
  
  public static boolean matchServiceNamePattern(String serviceName) {
    return SERVICE_NAME_PATTERN.matcher(serviceName).matches();
  }
  
  /**
   * Returns the Apiary v1 API name as derived from a service name.
   */
  public static String deriveApiNameFromServiceName(String serviceName) {
    Matcher matcher = SERVICE_NAME_PATTERN.matcher(serviceName);
    if (matcher.matches()) {
      serviceName = matcher.group("sandboxed");
      if (serviceName == null) {
        serviceName = matcher.group("corp");
        if (serviceName != null) {
          // Add a corp_ prefix to the corp APIs.
          serviceName = "corp_" + serviceName;
        }
      }
      if (serviceName == null) {
        serviceName = matcher.group("legacySandboxed");
      }
      if (serviceName == null) {
        serviceName = matcher.group("regular");
      }
    }
    return serviceName.replace('.', '_').replace('-', '_');
  }
}


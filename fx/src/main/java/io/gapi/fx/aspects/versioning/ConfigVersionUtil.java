package io.gapi.fx.aspects.versioning;

import com.google.common.collect.Sets;
import com.google.common.io.Resources;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Util class for config version.
 */
public class ConfigVersionUtil {
  public static final String CONFIG_VERSION_0_WHITELIST_FILE =
      "google/api/whitelist/config_version_0_whitelist";
  
  private static final Set<String> CONFIG_VERSION_0_WHITELIST = retrieveConfigVersion0Whitelist();

  public static boolean isWhitelistedForConfigVersion0(String serviceName) {
    if (CONFIG_VERSION_0_WHITELIST.contains(serviceName)) {
      return true;
    }
    // TODO(b/22565053) Dateflow team has a customized build rule which uses a dynamic service
    // name. For example, dataflow-sgmc-staging.sandbox.googleapis.com. Remove this extra checking
    // once dataflow team migrates to config_version 1.
    if (serviceName.startsWith("dataflow-")) {
      return true;
    }
    return false;
  }

  private static Set<String> retrieveConfigVersion0Whitelist() {
    Set<String> result = Sets.newHashSet();
    try {
      for (String line :
          Resources.readLines(
              Resources.getResource(CONFIG_VERSION_0_WHITELIST_FILE), StandardCharsets.UTF_8)) {
        line = line.trim();
        if (!line.startsWith("#")) {
          result.add(line);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return result;
  }
}

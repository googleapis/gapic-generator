package io.gapi.fx.aspects.versioning;

import io.gapi.fx.aspects.ConfigAspectBase;
import io.gapi.fx.aspects.LintRule;
import io.gapi.fx.model.Model;

/**
 * Checks if config_version value is not less than
 * Model.CURRENT_CONFIG_DEFAULT_VERSION.
 */
class ConfigVersionRule extends LintRule<Model> {

  ConfigVersionRule(ConfigAspectBase aspect) {
    super(aspect, "config", Model.class);
  }

  @Override public void run(Model model) {
    if (model.getServiceConfig().hasConfigVersion()
        && model.getConfigVersion() != Model.getDefaultConfigVersion()) {
      warning(model,
          "Specified config_version value '%d' is not equal to "
          + "the current default value '%d'. Consider changing this value "
          + "to the default config version. Consult release notes (http://go/api-release-notes) "
          + "for implications.",
          model.getConfigVersion(), Model.getDefaultConfigVersion());
    }
  }
}

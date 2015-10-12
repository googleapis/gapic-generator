package io.gapi.fx.aspects.system;

import com.google.api.Service.Builder;
import io.gapi.fx.aspects.ConfigAspectBase;
import io.gapi.fx.model.Model;
import com.google.common.collect.Sets;
import com.google.protobuf.Type;

import java.util.Set;

/**
 * Configuration aspect for system-level configuration (e.g. system_types).
 */
public class SystemConfigAspect extends ConfigAspectBase {

  public static SystemConfigAspect create(Model model) {
    return new SystemConfigAspect(model);
  }

  private SystemConfigAspect(Model model) {
    super(model, "system");
  }

  @Override
  public void startNormalization(Builder builder) {
    Set<String> userDefinedTypes = Sets.newHashSet();
    for (Type type : builder.getTypesList()) {
      userDefinedTypes.add(type.getName());
    }
    
    for (Type type : builder.getSystemTypesList()) {
      // Add system type into types list if it has not been defined by user.
      if (!userDefinedTypes.contains(type.getName())) {
        builder.addTypes(type);
      }
    }
  }
}


package io.gapi.fx.processors.normalizer;

import com.google.api.Service;
import com.google.api.Service.Builder;
import io.gapi.fx.model.ConfigAspect;
import io.gapi.fx.model.Model;
import io.gapi.fx.model.Processor;
import io.gapi.fx.model.ProtoElement;
import io.gapi.fx.model.Visitor;
import io.gapi.fx.model.stages.Linted;
import io.gapi.fx.model.stages.Normalized;
import io.gapi.fx.util.VisitsBefore;

import com.google.common.collect.ImmutableList;
import com.google.inject.Key;

/**
 * A processor that establishes the {@link Normalized} stage.
 * After {@link Normalized} stage, all wildcards in configuration
 * rules have been expanded, and proto elements (service, method, message, etc.)
 * will be propagated to individual elements in //tech/api/proto/service.proto.
 */
public class Normalizer implements Processor {

  @Override
  public ImmutableList<Key<?>> requires() {
    return ImmutableList.<Key<?>>of(Linted.KEY);
  }

  @Override
  public Key<?> establishes() {
    return Normalized.KEY;
  }

  @Override
  public boolean run(Model model) {
    Service.Builder normalizedConfig = model.getServiceConfig().toBuilder();

    // Normalize descriptor.
    new DescriptorNormalizer(model).run(normalizedConfig);

    // Run aspect normalizers.
    for (ConfigAspect aspect : model.getConfigAspects()) {
      aspect.startNormalization(normalizedConfig);
    }
    new AspectNormalizer(model, normalizedConfig).visit(model);
    for (ConfigAspect aspect : model.getConfigAspects()) {
      aspect.endNormalization(normalizedConfig);
    }

    // Set result.
    model.setNormalizedConfig(normalizedConfig.build());
    model.putAttribute(Normalized.KEY, new Normalized());
    return true;
  }

  private static class AspectNormalizer extends Visitor {

    private final Model model;
    private final Service.Builder normalizedConfig;

    private AspectNormalizer(Model model, Builder normalizedConfig) {
      super(model.getScoper(), false/*ignoreMapEntry*/);
      this.model = model;
      this.normalizedConfig = normalizedConfig;
    }

    @VisitsBefore void normalize(ProtoElement element) {
      for (ConfigAspect aspect : model.getConfigAspects()) {
        aspect.normalize(element, normalizedConfig);
      }
    }
  }
}

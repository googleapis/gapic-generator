package io.gapi.vgen;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import io.gapi.fx.model.Diag;
import io.gapi.fx.model.Interface;
import io.gapi.fx.model.Model;
import io.gapi.fx.model.SimpleLocation;
import io.gapi.fx.model.stages.Merged;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Code generator. Delegates most work to a {@link LanguageProvider} which is found by
 * dynamic class loading.
 */
public class CodeGenerator {

  /**
   * Constructs a code generator by dynamically loading the language provider as specified by the
   * config. If loading fails, errors will be reported on the model, and null is returned. The
   * provider aggregates model and config from which it can be accessed by the code generator.
   */
  public static CodeGenerator create(Model model, Config config) {
    Preconditions.checkNotNull(model);
    Preconditions.checkNotNull(config);
    Class<?> providerType;
    try {
      providerType = Class.forName(config.getLanguageProvider());
    } catch (ClassNotFoundException e) {
      error(model, "Cannot resolve provider class '%s'. Is it in the class path?",
          config.getLanguageProvider());
      return null;
    }
    if (!LanguageProvider.class.isAssignableFrom(providerType)) {
      error(model, "the provider class '%s' does not extend the expected class '%s'",
          providerType.getName(), LanguageProvider.class.getName());
      return null;
    }
    Constructor<?> ctor;
    try {
      ctor = providerType.getConstructor(Model.class, Config.class);
    } catch (NoSuchMethodException | SecurityException e) {
      error(model, "the provider class '%s' does not have the expected constructor with "
          + "parameters (%s, %s)",
          providerType.getName(), Model.class.getName(), Config.class.getName());
      return null;
    }
    try {
      return new CodeGenerator((LanguageProvider) ctor.newInstance(model, config));
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException e) {
      // At this point, this is likely a bug and not a user error, so propagate exception.
      throw Throwables.propagate(e);
    }
  }

  private final LanguageProvider provider;

  private CodeGenerator(LanguageProvider provider) {
    this.provider = Preconditions.checkNotNull(provider);
  }

  /**
   * Generates code for the model. Returns a map from service interface to code for the
   * service. Returns null if generation failed.
   */
  @Nullable public Map<Interface, GeneratedResult> generate(
      SnippetDescriptor snippetDescriptor) {
    // Establish required stage for generation.
    provider.getModel().establishStage(Merged.KEY);
    if (provider.getModel().getErrorCount() > 0) {
      return null;
    }

    // Run the generator for each service.
    ImmutableMap.Builder<Interface, GeneratedResult> generated = ImmutableMap.builder();
    for (Interface iface : provider.getModel().getSymbolTable().getInterfaces()) {
      if (!iface.isReachable()) {
        continue;
      }
      GeneratedResult result = provider.generate(iface, snippetDescriptor);
      generated.put(iface, result);
    }

    // Return result.
    if (provider.getModel().getErrorCount() > 0) {
      return null;
    }
    return generated.build();
  }

  /**
   * Delegates creating code to language provider. Takes the result map from
   * {@link LanguageProvider#outputCode(String, Multimap)} and stores it in a
   * language-specific way.
   */
  public void outputCode(String outputFile, Multimap<Interface, GeneratedResult> services)
      throws IOException {
    provider.outputCode(outputFile, services);
  }

  private static void error(Model model, String message, Object... args) {
    model.addDiag(Diag.error(SimpleLocation.TOPLEVEL, message, args));
  }
}

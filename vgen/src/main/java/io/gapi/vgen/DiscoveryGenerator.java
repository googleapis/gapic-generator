package io.gapi.vgen;

import com.google.api.Service;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Base class for discovery doc fragment generators. Delegates most work to a
 * {@link DiscoveryLanguageProvider}, which is found by dynamic class loading.
 */
public class DiscoveryGenerator {

  protected final DiscoveryLanguageProvider provider;

  public DiscoveryGenerator(DiscoveryLanguageProvider provider) {
    this.provider = Preconditions.checkNotNull(provider);
  }

  public static DiscoveryGenerator create(DiscoveryLanguageProvider provider) {
    // TODO(pongad): HACK!!!
    // return new DiscoveryGenerator(provider);
    return new DiscoveryFragmentGenerator(provider);
  }

  public static class Builder {

    private ConfigProto configProto;
    private DiscoveryImporter discovery;

    /**
     * Sets the ConfigProto to be used in build().
     */
    public Builder setConfigProto(ConfigProto configProto) {
      this.configProto = configProto;
      return this;
    }

    /**
     * Sets the DiscoveryImporter to be used in build().
     */
    public Builder setDiscovery(DiscoveryImporter discovery) {
      this.discovery = discovery;
      return this;
    }

    /**
     * Constructs a code generator by dynamically loading the language provider as specified by the
     * config. The provider aggregates config and discovery doc importer from which they can be
     * accessed by the code generator.
     */
    public DiscoveryGenerator build() {
      Preconditions.checkNotNull(configProto);
      Preconditions.checkNotNull(discovery);

      ApiaryConfig apiaryConfig = discovery.getConfig();

      DiscoveryLanguageProvider languageProvider =
          createDiscoveryLanguageProvider(configProto.getLanguageProvider(), apiaryConfig);
      if (languageProvider == null) {
        return null;
      }
      return create(languageProvider);
    }

    private DiscoveryLanguageProvider createDiscoveryLanguageProvider(String languageProviderName,
        ApiaryConfig apiaryConfig) {
      Class<?> providerType;
      try {
        providerType = Class.forName(languageProviderName);
      } catch (ClassNotFoundException e) {
        error("Cannot resolve provider class '%s'. Is it in the class path?",
            languageProviderName);
        return null;
      }
      if (!DiscoveryLanguageProvider.class.isAssignableFrom(providerType)) {
        error("the provider class '%s' does not extend the expected class '%s'",
            providerType.getName(), DiscoveryLanguageProvider.class.getName());
        return null;
      }
      Constructor<?> ctor;
      try {
        ctor = providerType.getConstructor(Service.class, ApiaryConfig.class);
      } catch (NoSuchMethodException | SecurityException e) {
        error("the provider class '%s' does not have the expected constructor with "
            + "parameters (%s, %s)",
            providerType.getName(), Service.class.getName(), ApiaryConfig.class.getName());
        return null;
      }
      try {
        return (DiscoveryLanguageProvider) ctor.newInstance(discovery.getService(), apiaryConfig);
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
          | InvocationTargetException e) {
        // At this point, this is likely a bug and not a user error, so propagate exception.
        throw Throwables.propagate(e);
      }
    }

    private void error(String message, Object... args) {
      System.err.printf(message, args);
    }
  }

}

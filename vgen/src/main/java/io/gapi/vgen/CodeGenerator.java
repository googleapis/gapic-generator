package io.gapi.vgen;

import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Code generator. Delegates most work to a {@link LanguageProvider} which is found by
 * dynamic class loading.
 */
public class CodeGenerator {

  private final LanguageProvider provider;

  public CodeGenerator(LanguageProvider provider) {
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

  @Nullable public Map<String, GeneratedResult> generateDocs(SnippetDescriptor snippetDescriptor) {
    Set<ProtoFile> files = new HashSet<ProtoFile>();
    for (Interface iface : provider.getModel().getSymbolTable().getInterfaces()) {
      for (Method method : iface.getMethods()) {
        for (Field field : method.getInputType().getMessageType().getFields()) {
          files.addAll(protoFiles(field));
        }
      }
    }
    Map<String, GeneratedResult> generated = new HashMap();
    for (ProtoFile file : files) {
      GeneratedResult result = provider.generateDoc(file, snippetDescriptor);
      generated.put(result.getFilename(), result);
    }
    if (provider.getModel().getErrorCount() > 0) {
      return null;
    }
    return generated;
  }

  private Set<ProtoFile> protoFiles(Field field) {
    Set<ProtoFile> fields = new HashSet<ProtoFile>();
    if (field.getType().getKind() != Type.TYPE_MESSAGE) {
      return fields;
    }
    MessageType messageType = field.getType().getMessageType();
    fields.add(messageType.getFile());
    for (Field f : messageType.getNonCyclicFields()) {
      fields.addAll(protoFiles(f));
    }
    return fields;
  }

  /**
   * Delegates creating code to language provider. Takes the result list from
   * {@link LanguageProvider#outputCode(String, List)} and stores it in a
   * language-specific way.
   */
  public void outputCode(String outputFile, List<GeneratedResult> results,
      boolean archive) throws IOException {
    provider.outputCode(outputFile, results, archive);
  }

  public static class Builder {

    private ConfigProto configProto;
    private Model model;

    /**
     * Sets the model to be used in build().
     */
    public Builder setModel(Model model) {
      this.model = model;
      return this;
    }

    /**
     * Sets the ConfigProto to be used in build().
     */
    public Builder setConfigProto(ConfigProto configProto) {
      this.configProto = configProto;
      return this;
    }

    /**
     * Constructs a code generator by dynamically loading the language provider as specified by the
     * config. If loading fails, errors will be reported on the model, and null is returned. The
     * provider aggregates model and config from which it can be accessed by the code generator.
     */
    public CodeGenerator build() {
      Preconditions.checkNotNull(model);
      Preconditions.checkNotNull(configProto);

      model.establishStage(Merged.KEY);
      if (model.getErrorCount() > 0) {
        return null;
      }

      ApiConfig apiConfig = ApiConfig.createApiConfig(model, configProto);
      if (apiConfig == null) {
        return null;
      }

      LanguageProvider languageProvider =
          createLanguageProvider(configProto.getLanguageProvider(), apiConfig);
      if (languageProvider == null) {
        return null;
      }

      return new CodeGenerator(languageProvider);
    }

    private LanguageProvider createLanguageProvider(String languageProviderName,
        ApiConfig apiConfig) {
      Class<?> providerType;
      try {
        providerType = Class.forName(languageProviderName);
      } catch (ClassNotFoundException e) {
        error("Cannot resolve provider class '%s'. Is it in the class path?",
            languageProviderName);
        return null;
      }
      if (!LanguageProvider.class.isAssignableFrom(providerType)) {
        error("the provider class '%s' does not extend the expected class '%s'",
            providerType.getName(), LanguageProvider.class.getName());
        return null;
      }
      Constructor<?> ctor;
      try {
        ctor = providerType.getConstructor(Model.class, ApiConfig.class);
      } catch (NoSuchMethodException | SecurityException e) {
        error("the provider class '%s' does not have the expected constructor with "
            + "parameters (%s, %s)",
            providerType.getName(), Model.class.getName(), ApiConfig.class.getName());
        return null;
      }
      try {
        return (LanguageProvider) ctor.newInstance(model, apiConfig);
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
          | InvocationTargetException e) {
        // At this point, this is likely a bug and not a user error, so propagate exception.
        throw Throwables.propagate(e);
      }
    }

    private void error(String message, Object... args) {
      model.addDiag(Diag.error(SimpleLocation.TOPLEVEL, message, args));
    }
  }

}

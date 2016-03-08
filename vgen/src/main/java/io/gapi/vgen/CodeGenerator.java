package io.gapi.vgen;

import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Code generator.
 */
public class CodeGenerator extends Generator {

  public CodeGenerator(LanguageProvider provider) {
    super(provider);
  }

  public static CodeGenerator create(LanguageProvider provider) {
    return new CodeGenerator(provider);
  }

  public static CodeGenerator create(Generator generator) {
    return create(generator.provider);
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
  public void outputCode(String outputFile,
      Multimap<Interface, GeneratedResult> services, boolean archive)
      throws IOException {
    provider.outputCode(outputFile, services, archive);
  }
}

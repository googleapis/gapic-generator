package io.gapi.vgen;

import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Fragment generator.
 */
public class FragmentGenerator extends CodeGenerator {

  public FragmentGenerator(LanguageProvider provider) {
    super(provider);
  }

  public static FragmentGenerator create(LanguageProvider provider) {
    return new FragmentGenerator(provider);
  }

  public static FragmentGenerator create(Generator generator) {
    return create(generator.provider);
  }

  /**
   * Generates fragments for the model. Returns a map from each method to a fragment for the
   * method. Returns null if generation failed.
   */
  @Nullable public Map<Method, GeneratedResult> generateFragments(
      SnippetDescriptor snippetDescriptor) {
    // Establish required stage for generation.
    provider.getModel().establishStage(Merged.KEY);
    if (provider.getModel().getErrorCount() > 0) {
      return null;
    }

    // Run the generator for each method of each service.
    ImmutableMap.Builder<Method, GeneratedResult> generated = ImmutableMap.builder();
    for (Interface iface : provider.getModel().getSymbolTable().getInterfaces()) {
      if (!iface.isReachable()) {
        continue;
      }
      for (Method method : iface.getMethods()) {
        GeneratedResult result = provider.generateFragment(method, snippetDescriptor);
        generated.put(method, result);
      }
    }

    // Return result.
    if (provider.getModel().getErrorCount() > 0) {
      return null;
    }
    return generated.build();
  }

  /**
   * Delegates creating fragments to language provider. Takes the result map from
   * {@link LanguageProvider#outputFragments(String, Multimap)} and stores it in a
   * language-specific way.
   */
  public void outputFragments(String outputFile,
      Multimap<Method, GeneratedResult> methods, boolean archive)
      throws IOException {
    provider.outputFragments(outputFile, methods, archive);
  }
}

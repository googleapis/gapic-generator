package io.gapi.vgen;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Api;
import com.google.protobuf.Method;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Discovery doc fragment generator.
 */
public class DiscoveryFragmentGenerator extends DiscoveryGenerator {

  public DiscoveryFragmentGenerator(DiscoveryLanguageProvider provider) {
    super(provider);
  }

  public static DiscoveryFragmentGenerator create(DiscoveryLanguageProvider provider) {
    return new DiscoveryFragmentGenerator(provider);
  }

  public static DiscoveryFragmentGenerator create(DiscoveryGenerator generator) {
    return create(generator.provider);
  }

  /**
   * Generates fragments for the model. Returns a map from each method to a fragment for the
   * method. Returns null if generation failed.
   */
  @Nullable public Map<Method, GeneratedResult> generateFragments(
      SnippetDescriptor snippetDescriptor) {

    // Run the generator for each method of each API.
    ImmutableMap.Builder<Method, GeneratedResult> generated = ImmutableMap.builder();
    for (Api api : provider.getService().getApisList()) {
      for (Method method : api.getMethodsList()) {
          GeneratedResult result = provider.generateFragment(method, snippetDescriptor);
          generated.put(method, result);
      }
    }

    return generated.build();
  }

  /**
   * Delegates creating fragments to language provider. Takes the result map from
   * {@link DiscoveryLanguageProvider#outputFragments(String, Multimap)} and stores it in a
   * language-specific way.
   */
  public void outputFragments(String outputFile,
      Multimap<Method, GeneratedResult> methods, boolean archive)
      throws IOException {
    provider.outputFragments(outputFile, methods, archive);
  }
}
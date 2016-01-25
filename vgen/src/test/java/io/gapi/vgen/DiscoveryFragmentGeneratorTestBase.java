package io.gapi.vgen;

import com.google.api.tools.framework.snippet.Doc;
import com.google.common.truth.Truth;
import com.google.protobuf.Method;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Map;

/**
 * Fragment generator baseline tests.
 */
@RunWith(Parameterized.class)
public abstract class DiscoveryFragmentGeneratorTestBase extends DiscoveryGeneratorTestBase {

  public DiscoveryFragmentGeneratorTestBase(String name, String discoveryDocFileName,
      String[] veneerConfigFileNames) {
    super(name, discoveryDocFileName, veneerConfigFileNames);
  }

  @Override
  protected Object run() {
    Truth.assertThat(this.generator).isNotNull();

    String snippetInputName = config.getFragmentFilesList().get(0);
    SnippetDescriptor resourceDescriptor =
          new SnippetDescriptor(snippetInputName);
    Map<Method, GeneratedResult> result =
        DiscoveryFragmentGenerator.create(generator).generateFragments(resourceDescriptor);
    if (result == null) {
      return null;
    } else {
      Doc output = Doc.EMPTY;
      for (GeneratedResult fragment : result.values()) {
        output = Doc.joinWith(Doc.BREAK, output, fragment.getDoc());
      }
      return Doc.vgroup(output);
    }
  }
}

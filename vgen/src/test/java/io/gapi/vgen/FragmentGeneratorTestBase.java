package io.gapi.vgen;

import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.Method;
import com.google.common.truth.Truth;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Map;

/**
 * Fragment generator baseline tests.
 */
@RunWith(Parameterized.class)
public abstract class FragmentGeneratorTestBase extends GeneratorTestBase {

  public FragmentGeneratorTestBase(String name, String[] veneerConfigFileNames) {
    super(name, veneerConfigFileNames);
  }

  @Override
  protected Object run() {
    Truth.assertThat(this.generator).isNotNull();

    String snippetInputName = config.getFragmentFilesList().get(0);
    SnippetDescriptor resourceDescriptor =
          new SnippetDescriptor(snippetInputName);
    Map<Method, GeneratedResult> result =
        FragmentGenerator.create(generator).generateFragments(resourceDescriptor);
    if (result == null) {
      // Report diagnosis to baseline file.
      for (Diag diag : model.getDiags()) {
        testOutput().println(diag.toString());
      }
      return null;
    }
    return result.values().iterator().next().getDoc();
  }
}

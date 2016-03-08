package io.gapi.vgen;

import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.Interface;
import com.google.common.truth.Truth;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Map;

/**
 * Base class for code generator baseline tests.
 */
@RunWith(Parameterized.class)
public abstract class CodeGeneratorTestBase extends GeneratorTestBase {

  public CodeGeneratorTestBase(String name, String[] veneerConfigFileNames, String snippetName) {
    super(name, veneerConfigFileNames, snippetName);
  }

  public CodeGeneratorTestBase(String name, String[] veneerConfigFileNames) {
    super(name, veneerConfigFileNames);
  }

  protected GeneratedResult generateForSnippet(int index) {
    if (index >= config.getSnippetFilesCount()) {
      return null;
    }
    String snippetInputName = config.getSnippetFilesList().get(index);
    SnippetDescriptor resourceDescriptor =
          new SnippetDescriptor(snippetInputName);
    Map<Interface, GeneratedResult> result =
        CodeGenerator.create(generator).generate(resourceDescriptor);
    if (result == null) {
      // Report diagnosis to baseline file.
      for (Diag diag : model.getDiags()) {
        testOutput().println(diag.toString());
      }
      return null;
    }
    Truth.assertThat(result.size()).isEqualTo(1);
    return result.values().iterator().next();
  }

  @Override
  protected Object run() {
    Truth.assertThat(this.generator).isNotNull();
    GeneratedResult result = generateForSnippet(0);
    Truth.assertThat(result).isNotNull();
    return result.getDoc();
  }
}

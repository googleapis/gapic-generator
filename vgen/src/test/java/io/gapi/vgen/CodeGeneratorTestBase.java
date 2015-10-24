package io.gapi.vgen;

import com.google.common.truth.Truth;

import io.gapi.fx.model.Diag;
import io.gapi.fx.model.Interface;
import io.gapi.fx.testing.ApiConfigBaselineTestCase;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for code generator baseline tests.
 */
@RunWith(Parameterized.class)
public abstract class CodeGeneratorTestBase extends ApiConfigBaselineTestCase {

  private static final Pattern BASELINE_PATTERN = Pattern.compile("(\\w+)\\[(\\w+)\\]");

  // Wiring
  // ======

  private final String name;
  private final Config config;
  private CodeGenerator generator;

  public CodeGeneratorTestBase(String name, Config config) {
    this.name = name;
    this.config = config;
  }

  @Override protected void setupModel() {
    super.setupModel();
    generator = CodeGenerator.create(model, config);
    Truth.assertThat(this.generator).isNotNull();
  }

  @Override protected boolean suppressDiagnosis() {
    // Suppress linter warnings
    return true;
  }

  @Override
  protected Object run() {
    String snippetInputName = config.getSnippetFilesList().get(0);
    SnippetDescriptor resourceDescriptor =
          new SnippetDescriptor(snippetInputName);
    Map<Interface, GeneratedResult> result = generator.generate(resourceDescriptor);
    if (result == null) {
      // Report diagnosis to baseline file.
      for (Diag diag : model.getDiags()) {
        testOutput().println(diag.toString());
      }
      return null;
    }
    Truth.assertThat(result.size()).isEqualTo(1);
    return result.values().iterator().next().getDoc();
  }

  @Override
  protected String baselineFileName() {
    String methodName = testName.getMethodName();
    Matcher m = BASELINE_PATTERN.matcher(methodName);
    if (m.find()) {
      return m.group(2) + "_" + m.group(1) + ".baseline";
    } else {
      return name + "_" + methodName + ".baseline";
    }
  }
}

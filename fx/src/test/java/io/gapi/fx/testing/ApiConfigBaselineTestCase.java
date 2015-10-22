package io.gapi.fx.testing;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import io.gapi.fx.aspects.context.ContextConfigAspect;
import io.gapi.fx.aspects.documentation.DocumentationConfigAspect;
import io.gapi.fx.aspects.http.HttpConfigAspect;
import io.gapi.fx.aspects.naming.NamingConfigAspect;
import io.gapi.fx.aspects.system.SystemConfigAspect;
import io.gapi.fx.aspects.versioning.VersionConfigAspect;
import io.gapi.fx.aspects.visibility.VisibilityConfigAspect;
import io.gapi.fx.aspects.visibility.model.VisibilityUtil;
import io.gapi.fx.model.Diag;
import io.gapi.fx.model.Model;
import io.gapi.fx.processors.linter.Linter;
import io.gapi.fx.processors.merger.Merger;
import io.gapi.fx.processors.normalizer.Normalizer;
import io.gapi.fx.processors.resolver.Resolver;
import io.gapi.fx.snippet.Doc;
import io.gapi.gax.testing.BaselineTestCase;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * A baseline test case which prepares a model from proto and yaml config
 * and handles printing the result of a test run to the baseline.
 */
public abstract class ApiConfigBaselineTestCase extends BaselineTestCase {

  /**
   * The test configuration.
   */
  protected TestApiConfig apiConfig;

  /**
   * The model on which the test runs.
   */
  protected Model model;

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  /**
   * The map between test name and visibility labels.
   */
  protected Map<String, List<String>> testVisibilityLabelsMap = Maps.newLinkedHashMap();

  /**
   * Run test specific logic. The returned object will be printed to the baseline if not null.
   * The object can be a map from string to object, in which case the map will be decomposed
   * for the baseline output. If a {@link Doc} appears it will be pretty printed before
   * writing it.
   */
  @Nullable protected abstract Object run() throws Exception;

  /**
   * Configures standard setup of processors and config aspects. Override to use a non-standard
   * setup.
   */
  protected void setupModel() {
    model.registerProcessor(new Resolver());
    model.registerProcessor(new Merger());
    model.registerProcessor(new Normalizer());
    model.registerProcessor(new Linter());
    model.registerConfigAspect(DocumentationConfigAspect.create(model));
    model.registerConfigAspect(ContextConfigAspect.create(model));
    model.registerConfigAspect(HttpConfigAspect.create(model));
    model.registerConfigAspect(VisibilityConfigAspect.create(model));
    model.registerConfigAspect(VersionConfigAspect.create(model));
    model.registerConfigAspect(NamingConfigAspect.create(model));
    model.registerConfigAspect(SystemConfigAspect.create(model));
  }

  /**
   * Whether to suppress outputing diags to the baseline file.
   */
  protected boolean suppressDiagnosis() {
    return false;
  }

  /**
   * Run a test for the given file base name(s). Collects all .proto and .yaml files with the given
   * base name (i.e. baseName.proto or baseName.yaml), constructs model, and calls {@link #run()}.
   * Post that, prints diags and the result of the run to the baseline.
   */
  protected void test(String... baseNames) throws Exception {
    // Determine proto and yaml files.
    List<String> protoFiles = Lists.newArrayList();
    List<String> yamlFiles = Lists.newArrayList();
    for (String baseName : baseNames) {
      String name = baseName + ".proto";
      URL url = testDataLocator().findTestData(name);
      if (url != null) {
        protoFiles.add(name);
      }
      name = baseName + ".yaml";
      url = testDataLocator().findTestData(name);
      if (url != null) {
        yamlFiles.add(name);
      }
    }
    if (protoFiles.isEmpty()) {
      throw new IllegalArgumentException("No proto files found");
    }
    this.apiConfig = new TestApiConfig(testDataLocator(), tempDir.getRoot().toString(), protoFiles);
    this.model = apiConfig.createModel(yamlFiles);

    // Setup
    setupModel();
    if (testVisibilityLabelsMap.containsKey(baseNames[0])) {
      VisibilityUtil.scopeModel(model,
          Sets.newLinkedHashSet((testVisibilityLabelsMap.get(baseNames[0]))));
    }

    // Run test specific logic.
    Object result = run();

    // Output diag into baseline file.
    if (!suppressDiagnosis()) {
      for (Diag diag : model.getDiags()) {
        testOutput().println(diag.toString());
      }
    }

    if (model.getErrorCount() == 0 && result != null) {
      // Output the result depending on its type.
      if (result instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
          testOutput().printf("============== file: %s ==============%n", entry.getKey());
          testOutput().println(displayValue(entry.getValue()));
        }
      } else {
        testOutput().println(displayValue(result));
      }
    }
  }

  /**
   * Fetches content from various values for a content source (File, Doc, etc.)
   */
  private String displayValue(Object value) throws IOException {
    if (value instanceof Doc) {
      return ((Doc) value).prettyPrint(100);
    } else if (value instanceof File) {
      return Files.toString((File) value, StandardCharsets.UTF_8);
    }
    // TODO: MIGRATION
    // else if (value instanceof MessageOrBuilder) {
    // Convert proto to text format
    //  return TextFormatter.printToString((MessageOrBuilder) value);
    //}
    else {
      return value.toString();
    }
  }
}
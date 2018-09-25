package com.google.api.codegen.protoannotations;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.AnnotationsProto;
import com.google.api.Resource;
import com.google.api.Retry;
import com.google.api.codegen.CodegenTestUtil;
import com.google.api.codegen.GapicTestConfig;
import com.google.api.codegen.configgen.transformer.LanguageTransformer;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.testing.TestConfig;
import com.google.api.tools.framework.model.testing.TestDataLocator;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.longrunning.OperationTypes;
import com.google.longrunning.OperationsProto;
import com.google.protobuf.Api;
import com.google.protobuf.Message;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ProtoParserTest {
  @ClassRule
  public static TemporaryFolder tempDir = new TemporaryFolder();
  private static Model model;
  private static TestDataLocator testDataLocator;
  private static TestConfig testConfig;
  private static ProtoFile libraryProtoFile;
  private static Field bookNameField;

  // Object under test.
  private static ProtoParser protoParser = ProtoParser.getProtoParser();

  @BeforeClass
  public static void startUp() {
    // Load and parse protofile.
    List<String> protoFiles = Lists.newArrayList("library.proto");
    List<String> yamlFiles = Lists.newArrayList("myproto.yaml");
    testDataLocator = TestDataLocator.create(ProtoParserTest.class);
    testDataLocator.addTestDataSource(CodegenTestUtil.class, "testsrc/common");
    testConfig = new GapicTestConfig(testDataLocator, tempDir.getRoot().getPath(), protoFiles);
    model = testConfig.createModel(Lists.newArrayList());

    libraryProtoFile = model.getFiles().stream()
        .filter(f -> f.getSimpleName().equals("library.proto"))
        .findFirst().get();

    model.addRoot(libraryProtoFile);
    MessageType book = libraryProtoFile.getMessages().stream()
        .filter(m -> m.getSimpleName().equals("Book")).findFirst().get();
    bookNameField = book.getFields().stream().filter(f -> f.getSimpleName().equals("name"))
        .findFirst().get();

  }

  @Test
  public void testGetResourcePath() {
    assertThat(protoParser.getResourcePath(bookNameField)).isEqualTo("shelves/*/books/*");
  }

  @Test
  public void testGetPackageName() {
    assertThat(ProtoParser.getPackageName(model)).isEqualTo("google.example.library.v1");
  }

  /** Return the entity name, e.g. "shelf" for a resource field. */
  @Test
  public void getResourceEntityName() {
    assertThat(ProtoParser.getResourceEntityName(bookNameField)).isEqualTo("book");
  }

  @Test
  public void getLongRunningOperation() {
    Method getBigBookMethod = libraryProtoFile.getInterfaces().get(0).lookupMethod("google.example.library.v1.LibraryService.GetBigBook");
    OperationTypes operationTypes = protoParser.getLongRunningOperation(getBigBookMethod);

    OperationTypes expected = OperationTypes.newBuilder()
        .setResponse("google.example.library.v1.Book")
        .setMetadata("google.example.library.v1.GetBigBookMetadata")
        .build();
    assertThat(operationTypes).isEqualTo(expected);
  }

  @Nullable
  public static String getFormattedPackageName(String language, String basePackageName) {
    LanguageTransformer.LanguageFormatter formatter =
        LanguageTransformer.LANGUAGE_FORMATTERS.get(language.toLowerCase());
    return formatter.getFormattedPackageName(basePackageName);
  }

  /** Return the extra retry codes for the given method. */
  public Retry getRetry(Method method) {
    return method.getDescriptor().getMethodAnnotation(AnnotationsProto.retry);
  }

  /** Return whether the method has the HttpRule for GET. */
  public boolean isHttpGetMethod(Method method) {
    return !Strings.isNullOrEmpty(
        method.getDescriptor().getMethodAnnotation(AnnotationsProto.http).getGet());
  }

  /** The hostname for this service (e.g. "foo.googleapis.com"). */
  public static String getServiceAddress(Interface service) {
    return service.getProto().getOptions().getExtension(AnnotationsProto.defaultHost);
  }

  /** The OAuth scopes for this service (e.g. "https://cloud.google.com/auth/cloud-platform"). */
  public List<String> getAuthScopes(Interface service) {
    return service.getProto().getOptions().getExtension(AnnotationsProto.oauth).getScopesList();
  }
}

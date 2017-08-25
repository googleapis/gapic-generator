/* Copyright 2017 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.advising;

import com.google.api.codegen.CodegenTestUtil;
import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.LanguageSettingsProto;
import com.google.api.codegen.PageStreamingConfigProto;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.model.testing.ConfigBaselineTestCase;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

public class AdviserTest extends ConfigBaselineTestCase {
  private ConfigProto configProto;
  private Adviser adviser;

  @Override
  public Object run() throws Exception {
    model.addSupressionDirective(model, "control-*");
    model.getDiagSuppressor().addPattern(model, "http:.*");
    model.establishStage(Merged.KEY);
    adviser.advise(model, configProto);
    return "";
  }

  @Before
  public void setup() {
    getTestDataLocator().addTestDataSource(CodegenTestUtil.class, "testsrc");
  }

  @Test
  public void missing_language_settings() throws Exception {
    configProto =
        ConfigProto.newBuilder()
            .putLanguageSettings(
                "java", buildLanguageSettings("com.google.cloud.example.library.spi.v1"))
            .putLanguageSettings(
                "python", buildLanguageSettings("google.cloud.gapic.example.library.v1"))
            .putLanguageSettings(
                "go", buildLanguageSettings("cloud.google.com/go/example/library/apiv1"))
            .putLanguageSettings("csharp", buildLanguageSettings("Google.Example.Library.V1"))
            .putLanguageSettings(
                "ruby", buildLanguageSettings("Google::Cloud::Example::Library::V1"))
            .putLanguageSettings(
                "php", buildLanguageSettings("Google\\Cloud\\Example\\Library\\V1"))
            .build();
    adviser =
        new Adviser(
            ImmutableList.<AdviserRule>of(new LanguageSettingsRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void missing_package_name() throws Exception {
    configProto =
        ConfigProto.newBuilder()
            .putLanguageSettings(
                "java", buildLanguageSettings("com.google.cloud.example.library.spi.v1"))
            .putLanguageSettings(
                "python", buildLanguageSettings("google.cloud.gapic.example.library.v1"))
            .putLanguageSettings(
                "go", buildLanguageSettings("cloud.google.com/go/example/library/apiv1"))
            .putLanguageSettings("csharp", buildLanguageSettings("Google.Example.Library.V1"))
            .putLanguageSettings(
                "ruby", buildLanguageSettings("Google::Cloud::Example::Library::V1"))
            .putLanguageSettings(
                "php", buildLanguageSettings("Google\\Cloud\\Example\\Library\\V1"))
            .putLanguageSettings("nodejs", LanguageSettingsProto.getDefaultInstance())
            .build();
    adviser =
        new Adviser(
            ImmutableList.<AdviserRule>of(new LanguageSettingsRule()), ImmutableList.<String>of());
    test("library");
  }

  private LanguageSettingsProto buildLanguageSettings(String packageName) {
    return LanguageSettingsProto.newBuilder().setPackageName(packageName).build();
  }

  @Test
  public void missing_license_header() throws Exception {
    configProto = ConfigProto.getDefaultInstance();
    adviser =
        new Adviser(
            ImmutableList.<AdviserRule>of(new LicenseHeaderRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void missing_copyright_file() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    configProtoBuilder.getLicenseHeaderBuilder().setLicenseFile("license-header-apache-2.0.txt");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(
            ImmutableList.<AdviserRule>of(new LicenseHeaderRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void missing_license_file() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    configProtoBuilder.getLicenseHeaderBuilder().setCopyrightFile("copyright-google.txt");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(
            ImmutableList.<AdviserRule>of(new LicenseHeaderRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void missing_interface_name() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    configProtoBuilder.addInterfacesBuilder();
    configProtoBuilder.addInterfacesBuilder().setName("google.example.library.v1.LibraryService");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(ImmutableList.<AdviserRule>of(new InterfaceRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void missing_interface() throws Exception {
    configProto = ConfigProto.getDefaultInstance();
    adviser =
        new Adviser(ImmutableList.<AdviserRule>of(new InterfaceRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void extra_interface() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    configProtoBuilder.addInterfacesBuilder().setName("google.example.extra.v1.ExtraService");
    configProtoBuilder.addInterfacesBuilder().setName("google.example.library.v1.LibraryService");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(ImmutableList.<AdviserRule>of(new InterfaceRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void duplicate_interface() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    configProtoBuilder.addInterfacesBuilder().setName("google.example.library.v1.LibraryService");
    configProtoBuilder.addInterfacesBuilder().setName("google.example.library.v1.LibraryService");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(ImmutableList.<AdviserRule>of(new InterfaceRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void path_template_not_a_field_name() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    InterfaceConfigProto.Builder interfaceProtoBuilder =
        configProtoBuilder
            .addInterfacesBuilder()
            .setName("google.example.library.v1.LibraryService");
    interfaceProtoBuilder
        .addCollectionsBuilder()
        .setNamePattern("bookShelves/{book_shelf}")
        .setEntityName("book_shelf");
    interfaceProtoBuilder.addMethodsBuilder().setName("CreateShelf");
    interfaceProtoBuilder
        .addMethodsBuilder()
        .setName("GetShelf")
        .putFieldNamePatterns("not_a_field", "book_shelf");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(
            ImmutableList.<AdviserRule>of(new CollectionRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void entity_name_not_in_collections() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    InterfaceConfigProto.Builder interfaceProtoBuilder =
        configProtoBuilder
            .addInterfacesBuilder()
            .setName("google.example.library.v1.LibraryService");
    interfaceProtoBuilder
        .addCollectionsBuilder()
        .setNamePattern("bookShelves/{book_shelf}")
        .setEntityName("book_shelf");
    interfaceProtoBuilder
        .addMethodsBuilder()
        .setName("GetShelf")
        .putFieldNamePatterns("name", "not_an_entity");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(
            ImmutableList.<AdviserRule>of(new CollectionRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void collections_not_in_interface() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    configProtoBuilder
        .addCollectionsBuilder()
        .setNamePattern("bookShelves/{book_shelf}")
        .setEntityName("book_shelf");
    InterfaceConfigProto.Builder interfaceProtoBuilder =
        configProtoBuilder
            .addInterfacesBuilder()
            .setName("google.example.library.v1.LibraryService");
    interfaceProtoBuilder
        .addMethodsBuilder()
        .setName("GetShelf")
        .putFieldNamePatterns("name", "book_shelf");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(
            ImmutableList.<AdviserRule>of(new CollectionRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void missing_method_name() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    InterfaceConfigProto.Builder interfaceProtoBuilder =
        configProtoBuilder
            .addInterfacesBuilder()
            .setName("google.example.library.v1.LibraryService");
    interfaceProtoBuilder.addMethodsBuilder();
    interfaceProtoBuilder.addMethodsBuilder().setName("CreateShelf");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetShelf");
    interfaceProtoBuilder.addMethodsBuilder().setName("ListShelves");
    interfaceProtoBuilder.addMethodsBuilder().setName("DeleteShelf");
    interfaceProtoBuilder.addMethodsBuilder().setName("MergeShelves");
    interfaceProtoBuilder.addMethodsBuilder().setName("CreateBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("PublishSeries");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("ListBooks");
    interfaceProtoBuilder.addMethodsBuilder().setName("DeleteBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("UpdateBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("MoveBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("ListStrings");
    interfaceProtoBuilder.addMethodsBuilder().setName("AddComments");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBookFromArchive");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBookFromAnywhere");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBookFromAbsolutelyAnywhere");
    interfaceProtoBuilder.addMethodsBuilder().setName("UpdateBookIndex");
    interfaceProtoBuilder.addMethodsBuilder().setName("StreamShelves");
    interfaceProtoBuilder.addMethodsBuilder().setName("StreamBooks");
    interfaceProtoBuilder.addMethodsBuilder().setName("DiscussBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("MonologAboutBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("FindRelatedBooks");
    interfaceProtoBuilder.addMethodsBuilder().setName("AddTag");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBigBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBigNothing");
    interfaceProtoBuilder.addMethodsBuilder().setName("TestOptionalRequiredFlatteningParams");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(ImmutableList.<AdviserRule>of(new MethodRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void missing_method() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    InterfaceConfigProto.Builder interfaceProtoBuilder =
        configProtoBuilder
            .addInterfacesBuilder()
            .setName("google.example.library.v1.LibraryService");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetShelf");
    interfaceProtoBuilder.addMethodsBuilder().setName("ListShelves");
    interfaceProtoBuilder.addMethodsBuilder().setName("DeleteShelf");
    interfaceProtoBuilder.addMethodsBuilder().setName("MergeShelves");
    interfaceProtoBuilder.addMethodsBuilder().setName("CreateBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("PublishSeries");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("ListBooks");
    interfaceProtoBuilder.addMethodsBuilder().setName("DeleteBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("UpdateBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("MoveBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("ListStrings");
    interfaceProtoBuilder.addMethodsBuilder().setName("AddComments");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBookFromArchive");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBookFromAnywhere");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBookFromAbsolutelyAnywhere");
    interfaceProtoBuilder.addMethodsBuilder().setName("UpdateBookIndex");
    interfaceProtoBuilder.addMethodsBuilder().setName("StreamShelves");
    interfaceProtoBuilder.addMethodsBuilder().setName("StreamBooks");
    interfaceProtoBuilder.addMethodsBuilder().setName("DiscussBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("MonologAboutBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("FindRelatedBooks");
    interfaceProtoBuilder.addMethodsBuilder().setName("AddTag");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBigBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBigNothing");
    interfaceProtoBuilder.addMethodsBuilder().setName("TestOptionalRequiredFlatteningParams");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(ImmutableList.<AdviserRule>of(new MethodRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void extra_method() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    InterfaceConfigProto.Builder interfaceProtoBuilder =
        configProtoBuilder
            .addInterfacesBuilder()
            .setName("google.example.library.v1.LibraryService");
    interfaceProtoBuilder.addMethodsBuilder().setName("CreateShelf");
    interfaceProtoBuilder.addMethodsBuilder().setName("ExtraMethod");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetShelf");
    interfaceProtoBuilder.addMethodsBuilder().setName("ListShelves");
    interfaceProtoBuilder.addMethodsBuilder().setName("DeleteShelf");
    interfaceProtoBuilder.addMethodsBuilder().setName("MergeShelves");
    interfaceProtoBuilder.addMethodsBuilder().setName("CreateBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("PublishSeries");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("ListBooks");
    interfaceProtoBuilder.addMethodsBuilder().setName("DeleteBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("UpdateBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("MoveBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("ListStrings");
    interfaceProtoBuilder.addMethodsBuilder().setName("AddComments");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBookFromArchive");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBookFromAnywhere");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBookFromAbsolutelyAnywhere");
    interfaceProtoBuilder.addMethodsBuilder().setName("UpdateBookIndex");
    interfaceProtoBuilder.addMethodsBuilder().setName("StreamShelves");
    interfaceProtoBuilder.addMethodsBuilder().setName("StreamBooks");
    interfaceProtoBuilder.addMethodsBuilder().setName("DiscussBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("MonologAboutBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("FindRelatedBooks");
    interfaceProtoBuilder.addMethodsBuilder().setName("AddTag");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBigBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBigNothing");
    interfaceProtoBuilder.addMethodsBuilder().setName("TestOptionalRequiredFlatteningParams");
    interfaceProtoBuilder.addMethodsBuilder().setName("AnotherExtraMethod");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(ImmutableList.<AdviserRule>of(new MethodRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void duplicate_method() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    InterfaceConfigProto.Builder interfaceProtoBuilder =
        configProtoBuilder
            .addInterfacesBuilder()
            .setName("google.example.library.v1.LibraryService");
    interfaceProtoBuilder.addMethodsBuilder().setName("CreateShelf");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetShelf");
    interfaceProtoBuilder.addMethodsBuilder().setName("ListShelves");
    interfaceProtoBuilder.addMethodsBuilder().setName("DeleteShelf");
    interfaceProtoBuilder.addMethodsBuilder().setName("MergeShelves");
    interfaceProtoBuilder.addMethodsBuilder().setName("CreateBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("PublishSeries");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("ListBooks");
    interfaceProtoBuilder.addMethodsBuilder().setName("DeleteBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("UpdateBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("MoveBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("ListStrings");
    interfaceProtoBuilder.addMethodsBuilder().setName("AddComments");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBookFromArchive");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBookFromAnywhere");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBookFromAbsolutelyAnywhere");
    interfaceProtoBuilder.addMethodsBuilder().setName("UpdateBookIndex");
    interfaceProtoBuilder.addMethodsBuilder().setName("StreamShelves");
    interfaceProtoBuilder.addMethodsBuilder().setName("StreamBooks");
    interfaceProtoBuilder.addMethodsBuilder().setName("DiscussBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("MonologAboutBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("FindRelatedBooks");
    interfaceProtoBuilder.addMethodsBuilder().setName("AddTag");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBigBook");
    interfaceProtoBuilder.addMethodsBuilder().setName("GetBigNothing");
    interfaceProtoBuilder.addMethodsBuilder().setName("TestOptionalRequiredFlatteningParams");
    interfaceProtoBuilder.addMethodsBuilder().setName("CreateShelf");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(ImmutableList.<AdviserRule>of(new MethodRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void missing_retry_codes_def() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    InterfaceConfigProto.Builder interfaceProtoBuilder =
        configProtoBuilder
            .addInterfacesBuilder()
            .setName("google.example.library.v1.LibraryService");
    interfaceProtoBuilder.addRetryParamsDefBuilder().setName("default");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(ImmutableList.<AdviserRule>of(new RetryRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void missing_retry_params_def() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    InterfaceConfigProto.Builder interfaceProtoBuilder =
        configProtoBuilder
            .addInterfacesBuilder()
            .setName("google.example.library.v1.LibraryService");
    interfaceProtoBuilder.addRetryCodesDefBuilder().setName("non_idempotent");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(ImmutableList.<AdviserRule>of(new RetryRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void missing_retry_codes_def_name() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    InterfaceConfigProto.Builder interfaceProtoBuilder =
        configProtoBuilder
            .addInterfacesBuilder()
            .setName("google.example.library.v1.LibraryService");
    interfaceProtoBuilder.addRetryCodesDefBuilder();
    interfaceProtoBuilder.addRetryCodesDefBuilder();
    interfaceProtoBuilder.addRetryParamsDefBuilder().setName("default");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(ImmutableList.<AdviserRule>of(new RetryRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void missing_retry_params_def_name() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    InterfaceConfigProto.Builder interfaceProtoBuilder =
        configProtoBuilder
            .addInterfacesBuilder()
            .setName("google.example.library.v1.LibraryService");
    interfaceProtoBuilder.addRetryCodesDefBuilder().setName("non_idempotent");
    interfaceProtoBuilder.addRetryParamsDefBuilder();
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(ImmutableList.<AdviserRule>of(new RetryRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void missing_retry_codes_name() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    InterfaceConfigProto.Builder interfaceProtoBuilder =
        configProtoBuilder
            .addInterfacesBuilder()
            .setName("google.example.library.v1.LibraryService");
    interfaceProtoBuilder.addRetryCodesDefBuilder().setName("non_idempotent");
    interfaceProtoBuilder.addRetryParamsDefBuilder().setName("default");
    interfaceProtoBuilder.addMethodsBuilder().setName("CreateShelf").setRetryParamsName("default");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(ImmutableList.<AdviserRule>of(new RetryRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void missing_retry_params_name() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    InterfaceConfigProto.Builder interfaceProtoBuilder =
        configProtoBuilder
            .addInterfacesBuilder()
            .setName("google.example.library.v1.LibraryService");
    interfaceProtoBuilder.addRetryCodesDefBuilder().setName("non_idempotent");
    interfaceProtoBuilder.addRetryParamsDefBuilder().setName("default");
    interfaceProtoBuilder
        .addMethodsBuilder()
        .setName("CreateShelf")
        .setRetryCodesName("non_idempotent");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(ImmutableList.<AdviserRule>of(new RetryRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void retry_codes_name_not_in_retry_codes_defs() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    InterfaceConfigProto.Builder interfaceProtoBuilder =
        configProtoBuilder
            .addInterfacesBuilder()
            .setName("google.example.library.v1.LibraryService");
    interfaceProtoBuilder.addRetryCodesDefBuilder().setName("non_idempotent");
    interfaceProtoBuilder.addRetryParamsDefBuilder().setName("default");
    interfaceProtoBuilder
        .addMethodsBuilder()
        .setName("CreateShelf")
        .setRetryCodesName("not_a_retry_codes_name")
        .setRetryParamsName("default");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(ImmutableList.<AdviserRule>of(new RetryRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void retry_params_name_not_in_retry_params_defs() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    InterfaceConfigProto.Builder interfaceProtoBuilder =
        configProtoBuilder
            .addInterfacesBuilder()
            .setName("google.example.library.v1.LibraryService");
    interfaceProtoBuilder.addRetryCodesDefBuilder().setName("non_idempotent");
    interfaceProtoBuilder.addRetryParamsDefBuilder().setName("default");
    interfaceProtoBuilder
        .addMethodsBuilder()
        .setName("CreateShelf")
        .setRetryCodesName("non_idempotent")
        .setRetryParamsName("not_a_retry_params_name");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(ImmutableList.<AdviserRule>of(new RetryRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void flattening_group_parameter_not_a_field_name() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    InterfaceConfigProto.Builder interfaceProtoBuilder =
        configProtoBuilder
            .addInterfacesBuilder()
            .setName("google.example.library.v1.LibraryService");
    interfaceProtoBuilder
        .addMethodsBuilder()
        .setName("CreateShelf")
        .getFlatteningBuilder()
        .addGroupsBuilder()
        .addParameters("not_a_field");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(ImmutableList.<AdviserRule>of(new FieldRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void required_field_not_a_field_name() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    InterfaceConfigProto.Builder interfaceProtoBuilder =
        configProtoBuilder
            .addInterfacesBuilder()
            .setName("google.example.library.v1.LibraryService");
    interfaceProtoBuilder
        .addMethodsBuilder()
        .setName("CreateShelf")
        .addRequiredFields("not_a_field");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(ImmutableList.<AdviserRule>of(new FieldRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void missing_page_streaming_request() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    InterfaceConfigProto.Builder interfaceProtoBuilder =
        configProtoBuilder
            .addInterfacesBuilder()
            .setName("google.example.library.v1.LibraryService");
    interfaceProtoBuilder
        .addMethodsBuilder()
        .setName("ListShelves")
        .getPageStreamingBuilder()
        .getResponseBuilder()
        .setTokenField("next_page_token")
        .setResourcesField("shelves");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(
            ImmutableList.<AdviserRule>of(new PageStreamingRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void missing_page_streaming_request_token_field() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    InterfaceConfigProto.Builder interfaceProtoBuilder =
        configProtoBuilder
            .addInterfacesBuilder()
            .setName("google.example.library.v1.LibraryService");
    PageStreamingConfigProto.Builder pageStreamingProtoBuilder =
        interfaceProtoBuilder.addMethodsBuilder().setName("ListShelves").getPageStreamingBuilder();
    pageStreamingProtoBuilder.getRequestBuilder().setPageSizeField("page_size");
    pageStreamingProtoBuilder
        .getResponseBuilder()
        .setTokenField("next_page_token")
        .setResourcesField("shelves");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(
            ImmutableList.<AdviserRule>of(new PageStreamingRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void missing_page_streaming_response() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    InterfaceConfigProto.Builder interfaceProtoBuilder =
        configProtoBuilder
            .addInterfacesBuilder()
            .setName("google.example.library.v1.LibraryService");
    interfaceProtoBuilder
        .addMethodsBuilder()
        .setName("ListShelves")
        .getPageStreamingBuilder()
        .getRequestBuilder()
        .setTokenField("page_token");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(
            ImmutableList.<AdviserRule>of(new PageStreamingRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void missing_page_streaming_response_token_field() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    InterfaceConfigProto.Builder interfaceProtoBuilder =
        configProtoBuilder
            .addInterfacesBuilder()
            .setName("google.example.library.v1.LibraryService");
    PageStreamingConfigProto.Builder pageStreamingProtoBuilder =
        interfaceProtoBuilder.addMethodsBuilder().setName("ListShelves").getPageStreamingBuilder();
    pageStreamingProtoBuilder.getRequestBuilder().setTokenField("page_token");
    pageStreamingProtoBuilder.getResponseBuilder().setResourcesField("shelves");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(
            ImmutableList.<AdviserRule>of(new PageStreamingRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void missing_page_streaming_response_resources_field() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    InterfaceConfigProto.Builder interfaceProtoBuilder =
        configProtoBuilder
            .addInterfacesBuilder()
            .setName("google.example.library.v1.LibraryService");
    PageStreamingConfigProto.Builder pageStreamingProtoBuilder =
        interfaceProtoBuilder.addMethodsBuilder().setName("ListShelves").getPageStreamingBuilder();
    pageStreamingProtoBuilder.getRequestBuilder().setTokenField("page_token");
    pageStreamingProtoBuilder.getResponseBuilder().setTokenField("next_page_token");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(
            ImmutableList.<AdviserRule>of(new PageStreamingRule()), ImmutableList.<String>of());
    test("library");
  }

  @Test
  public void suppress_advice() throws Exception {
    ConfigProto.Builder configProtoBuilder = ConfigProto.newBuilder();
    configProtoBuilder.addInterfacesBuilder().setName("google.example.library.v1.LibraryService");
    configProto = configProtoBuilder.build();
    adviser =
        new Adviser(
            ImmutableList.of(new LanguageSettingsRule(), new LicenseHeaderRule(), new RetryRule()),
            ImmutableList.of("language-settings", "interface"));
    test("library");
  }
}

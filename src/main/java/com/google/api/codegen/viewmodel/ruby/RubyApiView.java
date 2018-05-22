/* Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.viewmodel.ruby;

import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.gapic.CommonGapicCodePathMapper;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.VersionMatcher;
import com.google.api.codegen.util.ruby.RubyCommentReformatter;
import com.google.api.codegen.util.ruby.RubyNameFormatter;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

public class RubyApiView implements ViewModel {

  private static final String TEMPLATE_FILENAME = "ruby/main.snip";

  private final GapicInterfaceContext context;

  public RubyApiView(GapicInterfaceContext context) {
    this.context = context;
  }

  public Name name() {
    return Name.anyCamel(context.getInterfaceConfig().getName(), "Client");
  }

  public String protoFilename() {
    return context.getInterface().getFile().getSimpleName();
  }

  public String interfaceKey() {
    return context.getInterface().getFullName();
  }

  public String serviceAddress() {
    return context.getApiModel().getServiceAddress();
  }

  public Integer servicePort() {
    return context.getApiModel().getServicePort();
  }

  public String fullyQualifiedCredentialsClassName() {
    ImmutableList.Builder<String> paths = ImmutableList.builder();
    if (isLongrunningClient()) {
      paths.add("Google");
      paths.add("Auth");
    } else {
      paths.addAll(getTopLevelApiModules());
    }

    paths.add("Credentials");
    return Joiner.on("::").join(paths.build());
  }

  public String defaultCredentialsInitializerCall() {
    return isLongrunningClient() ? "default(scopes: scopes)" : "default";
  }

  public String gapicPackageName() {
    if (isLongrunningClient()) {
      return "google-gax";
    }

    List<String> paths = new ArrayList<>();
    for (String part : modules()) {
      String path = Name.upperCamel(part).toLowerUnderscore();
      paths.add(path);
      if (VersionMatcher.isVersion(path)) {
        break;
      }
    }
    return Joiner.on('-').join(paths.subList(0, paths.size() - 1));
  }

  public String clientConfigPath() {
    return Name.upperCamel(context.getInterfaceModel().getSimpleName())
            .join("client_config")
            .toLowerUnderscore()
        + ".json";
  }

  public Iterable<String> docLines() {
    RubyCommentReformatter reformatter = new RubyCommentReformatter();
    ImmutableList.Builder<String> lines = ImmutableList.builder();
    lines.addAll(
        CommonRenderingUtil.getDocLines(reformatter.reformat(context.getInterfaceDescription())));
    String manualDoc = context.getInterfaceConfig().getManualDoc();
    if (!manualDoc.isEmpty()) {
      lines.addAll(CommonRenderingUtil.getDocLines(reformatter.reformat(manualDoc)));
    }

    return lines.build();
  }

  public Iterable<String> copyrightLines() {
    return context.getProductConfig().getCopyrightLines();
  }

  public Iterable<String> licenseLines() {
    return context.getProductConfig().getLicenseLines();
  }

  public Iterable<String> authScopes() {
    return context.getApiModel().getAuthScopes();
  }

  public Iterable<String> appImports() {
    ImmutableList.Builder<String> imports = ImmutableList.builder();
    for (String filename : getImportFilenames()) {
      imports.add(getProtoFileName(filename) + "_pb");
    }

    if (!isLongrunningClient()) {
      imports.add(getCredentialsFileName());
    }

    return imports.build();
  }

  private String getCredentialsFileName() {
    ImmutableList.Builder<String> paths = ImmutableList.builder();
    // Place credentials in top-level namespace.
    for (String path : getTopLevelApiModules()) {
      paths.add(Name.upperCamel(path).toLowerUnderscore());
    }

    paths.add("credentials");
    return Joiner.on(File.separator).join(paths.build());
  }

  public Iterable<String> serviceImports() {
    ImmutableList.Builder<String> imports = ImmutableList.builder();
    imports.add("google/gax/grpc");
    for (String filename : getImportFilenames()) {
      imports.add(getProtoFileName(filename) + "_services_pb");
    }

    return imports.build();
  }

  public Iterable<String> modules() {
    return Splitter.on("::").split(context.getProductConfig().getPackageName());
  }

  public Iterable<Map.Entry<String, String>> stubs() {
    Map<String, String> stubs = new TreeMap<>();
    for (MethodModel method : context.getSupportedMethods()) {
      GapicMethodContext methodContext = context.asDynamicMethodContext(method);
      InterfaceModel targetInterface = methodContext.getTargetInterface();
      String stubName =
          Name.upperCamel(targetInterface.getSimpleName(), "Stub").toLowerUnderscore();
      String grpcClientTypeName =
          NamePath.dotted(targetInterface.getFullName())
              .append("Stub")
              .withUpperPieces()
              .toDoubleColoned();
      stubs.put(grpcClientTypeName, stubName);
    }

    return stubs.entrySet();
  }

  public Iterable<RubyPageStreamingDescriptorView> pageStreamingDescriptors() {
    ImmutableList.Builder<RubyPageStreamingDescriptorView> descriptors = ImmutableList.builder();
    for (MethodModel method : context.getPageStreamingMethods()) {
      descriptors.add(new RubyPageStreamingDescriptorView(context.getMethodConfig(method)));
    }

    return descriptors.build();
  }

  public Iterable<RubyBatchingDescriptorView> batchingDescriptors() {
    ImmutableList.Builder<RubyBatchingDescriptorView> descriptors = ImmutableList.builder();
    for (MethodModel method : context.getBatchingMethods()) {
      descriptors.add(new RubyBatchingDescriptorView(context.getMethodConfig(method)));
    }

    return descriptors.build();
  }

  public Iterable<RubyPathTemplateView> pathTemplates() {
    Map<String, RubyPathTemplateView> resourceNameConfigs = new HashMap<>();
    for (SingleResourceNameConfig config :
        context.getInterfaceConfig().getSingleResourceNameConfigs()) {
      resourceNameConfigs.put(config.getEntityId(), new RubyPathTemplateView(config));
    }

    for (MethodModel method : context.getSupportedMethods()) {
      GapicMethodContext methodContext = context.asDynamicMethodContext(method);
      for (String fieldNamePattern :
          methodContext.getMethodConfig().getFieldNamePatterns().values()) {
        SingleResourceNameConfig resourceNameConfig =
            methodContext.getSingleResourceNameConfig(fieldNamePattern);
        if (resourceNameConfig != null
            && !resourceNameConfigs.containsKey(resourceNameConfig.getEntityId())) {
          resourceNameConfigs.put(
              resourceNameConfig.getEntityId(), new RubyPathTemplateView(resourceNameConfig));
        }
      }
    }

    return resourceNameConfigs.values();
  }

  public Iterable<RubyApiMethodView> apiMethods() {
    ImmutableList.Builder<RubyApiMethodView> apiMethods = ImmutableList.builder();
    for (MethodModel method : context.getSupportedMethods()) {
      apiMethods.add(new RubyApiMethodView(context.asDynamicMethodContext(method)));
    }

    return apiMethods.build();
  }

  public boolean hasLongRunningOperations() {
    return hasMatchingMethod(MethodConfig::isLongRunningOperation);
  }

  public boolean isLongrunningClient() {
    return context.getProductConfig().getPackageName().equals("Google::Longrunning");
  }

  @Override
  public String outputPath() {
    return CommonGapicCodePathMapper.newBuilder()
            .setPrefix("lib")
            .setShouldAppendPackage(true)
            .setPackageFilePathNameFormatter(new RubyNameFormatter())
            .build()
            .getOutputPath(interfaceKey(), context.getProductConfig())
        + File.separator
        + name().toLowerUnderscore()
        + ".rb";
  }

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  @Override
  public String templateFileName() {
    return TEMPLATE_FILENAME;
  }

  private boolean hasMatchingMethod(Predicate<GapicMethodConfig> predicate) {
    return context
        .getSupportedMethods()
        .stream()
        .anyMatch(method -> predicate.test(context.getMethodConfig(method)));
  }

  private List<String> getTopLevelApiModules() {
    ImmutableList.Builder<String> paths = ImmutableList.builder();
    for (String path : modules()) {
      if (VersionMatcher.isVersion(Name.upperCamel(path).toLowerUnderscore())) {
        break;
      }
      paths.add(path);
    }
    return paths.build();
  }

  private String getProtoFileName(String filename) {
    return filename.substring(0, filename.length() - ".proto".length());
  }

  private Iterable<String> getImportFilenames() {
    Set<String> filenames = new TreeSet<>();
    filenames.add(context.getInterface().getFile().getSimpleName());
    for (MethodModel method : context.getSupportedMethods()) {
      GapicMethodContext methodContext = context.asDynamicMethodContext(method);
      filenames.add(methodContext.getTargetInterface().getInterface().getFile().getSimpleName());
    }
    return filenames;
  }
}

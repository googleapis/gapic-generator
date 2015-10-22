package io.gapi.fx.testing;

import com.google.api.AnnotationsProto;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;

import io.gapi.fx.model.DiagCollector;
import io.gapi.fx.model.ExtensionPool;
import io.gapi.fx.model.Model;
import io.gapi.fx.yaml.YamlReader;
import io.gapi.gax.testing.TestDataLocator;

import autovalue.shaded.com.google.common.common.base.Throwables;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class to represent a test api configuration.
 *
 * <p>Compiles proto sources found in the classpath and converts yaml service config files,
 * in order to create a {@link Model} object from them.
 */
public class TestApiConfig {

  // TODO(wrwg): figure how we can make this independent of an installation of protoc
  // in the path.
  private static final String PROTOCOL_COMPILER = "protoc";

  private static final Pattern PROTO_IMPORT_PATTERN =
      Pattern.compile("\\s*import\\s*\"(.*)\"");

  private static final ExtensionRegistry EXTENSIONS;
  static {
    EXTENSIONS = ExtensionRegistry.newInstance();
    AnnotationsProto.registerAllExtensions(EXTENSIONS);
  }

  private final List<String> protoFiles;
  private final Path descriptorFile;
  private final TestDataLocator testDataLocator;

  /**
   * Creates a test api. The passed temp dir is managed by the caller; in a test, it is usally
   * created by the TemporaryFolder rule of junit. The passed proto files as well as their
   * imports must be retrievable via the passed test data locator.
   */
  public TestApiConfig(TestDataLocator testDataLocator, String tempDir, List<String> protoFiles) {
    this.testDataLocator = testDataLocator;
    this.protoFiles = ImmutableList.copyOf(protoFiles);
    // Extract all needed proto files.
    Set<String> extracted = Sets.newHashSet();
    for (String source : protoFiles) {
      extractProtoSources(extracted, tempDir, source);
    }
    // Compile the protos
    this.descriptorFile = Paths.get(tempDir, "_descriptor.dsc");
    compileProtos(tempDir, protoFiles, descriptorFile.toString());
  }

  /**
   * Reads test data associated with this test api. Uses the {@link TestDataLocator}
   * provided at creation time.
   */
  public String readTestData(String name) {
    URL url = testDataLocator.findTestData(name);
    if (url == null) {
      throw new IllegalArgumentException(String.format("Cannot find resource '%s'", name));
    }
    return testDataLocator.readTestData(url);
  }

  /**
   * Gets the descriptor file generated from the proto sources.
   */
  public Path getDescriptorFile() {
    return descriptorFile;
  }

  /**
   * Returns the file descriptor set generated from the sources of this api.
   */
  public FileDescriptorSet getDescriptor() throws IOException {
    return FileDescriptorSet.parseFrom(Files.newInputStream(descriptorFile), EXTENSIONS);
  }

  /**
   * Parses the config files, in Yaml format.
   */
  public ImmutableList<Message> getApiYamlConfig(DiagCollector diag,
      List<String> configFileNames) {
    ImmutableList.Builder<Message> builder = ImmutableList.builder();

    for (String fileName : configFileNames) {
      Message message = YamlReader.read(diag, fileName, readTestData(fileName));
      if (message != null) {
        builder.add(message);
      }
    }
    return builder.build();
  }

  /**
   * Creates a model, based on the provided config files.
   */
  public Model createModel(List<String> configFileNames) {
    try {
      Model model = Model.create(getDescriptor(), protoFiles, ImmutableList.of(),
          ExtensionPool.EMPTY);
      model.setConfigs(getApiYamlConfig(model, configFileNames));
      return model;
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * Collect all needed proto files as resources from the classpath and store them in the
   * temporary directory, so protoc can find them.
   */
  private void extractProtoSources(Set<String> extracted, String tempDir, String protoFile) {
    if (!extracted.add(protoFile)) {
      return;
    }
    String content = readTestData(protoFile);
    Path targetPath = Paths.get(tempDir, protoFile);
    try {
      Files.createDirectories(targetPath.getParent());
      Files.write(targetPath, content.getBytes(Charsets.UTF_8));
    } catch (IOException e) {
      throw new IllegalArgumentException(String.format("Cannot copy '%s': %s",
          targetPath, e.getMessage()));
    }
    Matcher matcher = PROTO_IMPORT_PATTERN.matcher(content);
    while (matcher.find()) {
      extractProtoSources(extracted, tempDir, matcher.group(1));
    }
  }


  /**
   * Calls the protocol compiler to compile the given sources into a descriptor.
   */
  private void compileProtos(String tempDir, List<String> sourceFiles, String outputFile) {
    List<String> commandLine = Lists.newArrayList();
    commandLine.add(PROTOCOL_COMPILER);
    commandLine.add("--include_imports");
    commandLine.add("--proto_path=" + tempDir);
    commandLine.add("--include_source_info");
    commandLine.add("-o");
    commandLine.add(outputFile);
    for (String source : sourceFiles) {
      commandLine.add(tempDir + File.separator + source);
    }
    ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
    Path output = Paths.get(tempDir, "_protoc.out");
    processBuilder.redirectErrorStream(true);
    processBuilder.redirectOutput(output.toFile());
    try {
      Process process = processBuilder.start();
      if (process.waitFor() != 0) {
        throw new IllegalArgumentException(
            String.format("proto compilation failed: %s:\n%s",
                Joiner.on(" ").join(commandLine), new String(Files.readAllBytes(output))));
      }
    } catch (Exception e) {
      throw new IllegalArgumentException(
          String.format("proto compilation failed with internal error: %s", e.getMessage()));
    }
  }
}

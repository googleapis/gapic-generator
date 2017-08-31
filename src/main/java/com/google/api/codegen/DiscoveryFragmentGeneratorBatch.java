/* Copyright 2016 Google Inc
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
package com.google.api.codegen;

import com.google.api.codegen.discoverybatch.DiscoveryBatch;
import com.google.api.codegen.discoverybatch.DiscoverySet;
import com.google.api.tools.framework.model.ConfigSource;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.SimpleDiagCollector;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class DiscoveryFragmentGeneratorBatch {
  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      throw new IllegalArgumentException("need exactly one config file");
    }
    ImmutableList<File> configFile = ImmutableList.<File>of(new File(args[0]));
    ImmutableMap<String, Message> supportedConfigTypes =
        ImmutableMap.<String, Message>of(
            DiscoveryBatch.getDescriptor().getFullName(), DiscoveryBatch.getDefaultInstance());
    DiagCollector diagCollector = new SimpleDiagCollector();
    ConfigSource configSource =
        MultiYamlReader.read(diagCollector, configFile, supportedConfigTypes);
    if (diagCollector.getErrorCount() > 0) {
      System.err.println(diagCollector.toString());
      return;
    }
    if (configSource == null) {
      return;
    }
    DiscoveryBatch batchConfig = (DiscoveryBatch) configSource.getConfig();
    if (batchConfig == null) {
      return;
    }

    Set<Job> jobs = new HashSet<>();
    for (DiscoverySet set : batchConfig.getIncludeSetsList()) {
      jobs.addAll(cartesianProduct(set, batchConfig.getOutput()));
    }
    for (DiscoverySet set : batchConfig.getIgnoreSetsList()) {
      jobs.removeAll(cartesianProduct(set, batchConfig.getOutput()));
    }

    Path basePath = Paths.get(args[0]).getParent();
    for (Job job : jobs) {
      String discoPath = basePath.resolve(Paths.get(job.getDiscoPath())).toString();
      String outputDir = basePath.resolve(Paths.get(job.getOutputDir())).toString();
      String yamlPath = basePath.resolve(Paths.get(job.getYamlPath())).toString();

      ToolOptions options = ToolOptions.create();
      options.set(DiscoveryFragmentGeneratorApi.DISCOVERY_DOC, discoPath);
      options.set(DiscoveryFragmentGeneratorApi.OUTPUT_FILE, outputDir);
      options.set(
          DiscoveryFragmentGeneratorApi.GENERATOR_CONFIG_FILES, ImmutableList.<String>of(yamlPath));
      new DiscoveryFragmentGeneratorApi(options).run();
    }
  }

  private static Set<Job> cartesianProduct(DiscoverySet discoSet, String outputPath) {
    Set<Job> jobSet = new HashSet<>();
    for (String yaml : discoSet.getYamlPathsList()) {
      for (String disco : discoSet.getDiscoPathsList()) {
        jobSet.add(Job.create(yaml, disco, outputPath));
      }
    }
    return jobSet;
  }

  @AutoValue
  abstract static class Job {
    abstract String getYamlPath();

    abstract String getDiscoPath();

    abstract String getOutputDir();

    private static Job create(String yamlPath, String discoPath, String outputDir) {
      return new AutoValue_DiscoveryFragmentGeneratorBatch_Job(yamlPath, discoPath, outputDir);
    }
  }
}

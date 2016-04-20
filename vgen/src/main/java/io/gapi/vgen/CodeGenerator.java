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
package io.gapi.vgen;

import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Code generator.
 */
public class CodeGenerator {

  private final GapicLanguageProvider provider;

  public CodeGenerator(GapicLanguageProvider provider) {
    this.provider = Preconditions.checkNotNull(provider);
  }

  public static CodeGenerator create(ConfigProto configProto, Model model) {
    return new CodeGenerator(GeneratorBuilderUtil.createLanguageProvider(configProto, model));
  }

  /**
   * Generates code for the model. Returns a map from service interface to code for the service.
   * Returns null if generation failed.
   */
  @Nullable
  public Map<Interface, GeneratedResult> generate(SnippetDescriptor snippetDescriptor) {
    // Establish required stage for generation.
    provider.getModel().establishStage(Merged.KEY);
    if (provider.getModel().getErrorCount() > 0) {
      return null;
    }

    // Run the generator for each service.
    ImmutableMap.Builder<Interface, GeneratedResult> generated = ImmutableMap.builder();
    for (Interface iface : provider.getModel().getSymbolTable().getInterfaces()) {
      if (!iface.isReachable()) {
        continue;
      }
      GeneratedResult result = provider.generateCode(iface, snippetDescriptor);
      generated.put(iface, result);
    }

    // Return result.
    if (provider.getModel().getErrorCount() > 0) {
      return null;
    }
    return generated.build();
  }

  @Nullable
  public Map<String, GeneratedResult> generateDocs(SnippetDescriptor snippetDescriptor) {
    Set<ProtoFile> files = new HashSet<ProtoFile>();
    for (Interface iface : provider.getModel().getSymbolTable().getInterfaces()) {
      for (Method method : iface.getMethods()) {
        for (Field field : method.getInputType().getMessageType().getFields()) {
          files.addAll(protoFiles(field));
        }
      }
    }
    Map<String, GeneratedResult> generated = new HashMap();
    for (ProtoFile file : files) {
      GeneratedResult result = provider.generateDoc(file, snippetDescriptor);
      generated.put(result.getFilename(), result);
    }
    if (provider.getModel().getErrorCount() > 0) {
      return null;
    }
    return generated;
  }

  private Set<ProtoFile> protoFiles(Field field) {
    Set<ProtoFile> fields = new HashSet<ProtoFile>();
    if (field.getType().getKind() != Type.TYPE_MESSAGE) {
      return fields;
    }
    MessageType messageType = field.getType().getMessageType();
    fields.add(messageType.getFile());
    for (Field f : messageType.getNonCyclicFields()) {
      fields.addAll(protoFiles(f));
    }
    return fields;
  }

  /**
   * Delegates creating code to language provider. Takes the result list from
   * {@link GapicLanguageProvider#outputCode(String, List)} and stores it in a language-specific
   * way.
   */
  public <Element> void output(
      String outputFile, Multimap<Element, GeneratedResult> results, boolean archive)
      throws IOException {
    provider.output(outputFile, results, archive);
  }
}

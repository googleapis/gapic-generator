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
package com.google.api.codegen.util;

import com.google.api.AnnotationsProto;
import com.google.api.ClientProto;
import com.google.api.FieldBehavior;
import com.google.api.FieldBehaviorProto;
import com.google.api.HttpRule;
import com.google.api.ResourceDescriptor;
import com.google.api.ResourceProto;
import com.google.api.ResourceReference;
import com.google.api.codegen.config.ResourceDescriptorConfig;
import com.google.api.codegen.config.ResourceDescriptorConfigs;
import com.google.api.pathtemplate.PathTemplate;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.longrunning.OperationInfo;
import com.google.longrunning.OperationsProto;
import com.google.protobuf.*;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

// Utils for parsing possibly-annotated protobuf API IDL.
public class ProtoParser {
  private final boolean enableProtoAnnotations;

  public boolean isProtoAnnotationsEnabled() {
    return enableProtoAnnotations;
  }

  public ProtoParser(boolean enableProtoAnnotations) {
    this.enableProtoAnnotations = enableProtoAnnotations;
  }

  @SuppressWarnings("unchecked")
  @Nullable
  private <T, O extends Message, E extends ProtoElement> T getProtoExtension(
      E element, GeneratedExtension<O, T> extension) {
    // Use this method as the chokepoint for all field annotations processing, so we can
    // toggle on/off annotations processing in one place.
    if (!enableProtoAnnotations) {
      return null;
    }
    return (T) element.getOptionFields().get(extension.getDescriptor());
  }

  /** Get a method annotation. */
  @Nullable
  private <T extends Message> T getMethodAnnotation(
      Method method, Extension<DescriptorProtos.MethodOptions, T> extension) {
    // We may want to parse the annotation even if enableProtoAnnotations is false.
    return method.getDescriptor().getMethodAnnotation(extension);
  }

  @SuppressWarnings("unchecked")
  @Nullable
  private <T extends ProtocolMessageEnum, O extends Message, E extends ProtoElement>
      List<EnumValueDescriptor> getProtoExtensionForEnumValue(
          E element, GeneratedExtension<O, List<T>> extension) {
    // Use this method as the chokepoint for all annotations processing for enum values
    // so we can toggle on/off annotations processing in one place.
    if (!enableProtoAnnotations) {
      return null;
    }
    return (List<EnumValueDescriptor>) element.getOptionFields().get(extension.getDescriptor());
  }

  /* Return the name of the field representing the header parameter. */
  public ImmutableSet<String> getHeaderParams(Method method) {
    ImmutableSet.Builder<String> allParams = ImmutableSet.builder();

    if (!enableProtoAnnotations) {
      return allParams.build();
    }

    HttpRule topRule = getHttpRule(method);
    if (topRule == null) {
      return allParams.build();
    }
    String firstParam = getHeaderParam(topRule);
    if (firstParam != null) allParams.add(firstParam);

    // Additional bindings should only be one-deep, according to the API client config spec.
    // No need to recurse on additional bindings' additional bindings.
    for (HttpRule rule : topRule.getAdditionalBindingsList()) {
      String headerParam = getHeaderParam(rule);
      if (headerParam != null) allParams.add(headerParam);
    }

    return allParams.build();
  }

  // Finds the header param from a HttpRule and add the non-null value to the running set.
  @Nullable
  private String getHeaderParam(HttpRule httpRule) {

    String urlVar;
    if (!Strings.isNullOrEmpty(httpRule.getPost())) {
      urlVar = httpRule.getPost();
    } else if (!Strings.isNullOrEmpty(httpRule.getDelete())) {
      urlVar = httpRule.getDelete();
    } else if (!Strings.isNullOrEmpty(httpRule.getGet())) {
      urlVar = httpRule.getGet();
    } else if (!Strings.isNullOrEmpty(httpRule.getPatch())) {
      urlVar = httpRule.getPatch();
    } else if (!Strings.isNullOrEmpty(httpRule.getPut())) {
      urlVar = httpRule.getPut();
    } else {
      return null;
    }

    PathTemplate pathTemplate = PathTemplate.create(urlVar);
    return pathTemplate.singleVar();
  }

  @Nullable
  public ResourceDescriptor getResourceDescriptor(MessageType element) {
    return getProtoExtension(element, ResourceProto.resource);
  }

  @Nullable
  public ResourceReference getResourceReference(Field element) {
    return getProtoExtension(element, ResourceProto.resourceReference);
  }

  public boolean hasResourceReference(Field element) {
    ResourceReference ref = getProtoExtension(element, ResourceProto.resourceReference);
    return ref != null && ref != ResourceReference.getDefaultInstance();
  }

  /** Get long running settings. */
  @Nullable
  public OperationInfo getLongRunningOperation(Method method) {
    return getMethodAnnotation(method, OperationsProto.operationInfo);
  }

  /** Get HTTP settings. */
  @Nullable
  private HttpRule getHttpRule(Method method) {
    return getMethodAnnotation(method, AnnotationsProto.http);
  }

  /** Return a Map of Unified Resource Types to a ResourceDescriptorConfig object. */
  public Map<String, ResourceDescriptorConfig> getResourceDescriptorConfigMap(
      List<ProtoFile> protoFiles, DiagCollector diagCollector) {
    // Skip unnecessary file parsing.
    if (!enableProtoAnnotations) return ImmutableMap.of();

    // Descriptors parsed from IAM methods might be duplicates of those parsed from annotations,
    // so we can't use an ImmutableMap.Builder
    HashMap<String, ResourceDescriptorConfig> resourceDescriptors = new HashMap<>();

    for (ProtoFile protoFile : protoFiles) {
      // Maps base names to ResourceDescriptors.
      Map<String, ResourceDescriptor> localDefs = new LinkedHashMap<>();

      // Get Resource[Set] definitions from fields in message types.
      for (MessageType message : protoFile.getMessages()) {
        ResourceDescriptor definition = getResourceDescriptor(message);
        if (definition != null) {
          if (localDefs.put(definition.getType(), definition) != null) {
            diagCollector.addDiag(
                Diag.error(
                    SimpleLocation.TOPLEVEL,
                    "Multiple ResourceDescriptor defintions with the type"
                        + " %s are defined in proto file %s. Values for type must be unique.",
                    definition.getType(),
                    protoFile.getFullName()));
            continue;
          }
          ResourceDescriptorConfig config =
              ResourceDescriptorConfig.from(definition, message.getFile());
          resourceDescriptors.put(config.getUnifiedResourceType(), config);
        }
      }

      for (Interface service : protoFile.getInterfaces()) {
        resourceDescriptors.putAll(
            ResourceDescriptorConfigs.createResourceNameDescriptorsFromIamMethods(
                protoFile, service, getServiceAddress(service)));
      }
    }
    return ImmutableMap.<String, ResourceDescriptorConfig>builder()
        .putAll(resourceDescriptors)
        .build();
  }

  /* Return a list of method signatures, aka flattenings, specified on a given method.
   * This flattens the repeated additionalSignatures into the returned list of MethodSignatures. */
  public List<List<String>> getMethodSignatures(Method method) {
    List<String> commaDelimSignatures = getProtoExtension(method, ClientProto.methodSignature);
    if (commaDelimSignatures == null) {
      return ImmutableList.of();
    }
    return ImmutableList.copyOf(
        commaDelimSignatures
            .stream()
            .map(
                s ->
                    (Arrays.stream(s.split(","))
                        .map(String::trim)
                        .filter(param -> !Strings.isNullOrEmpty(param))
                        .collect(Collectors.toList())))
            .collect(Collectors.toList()));
  }

  /** Return the names of required parameters of a method. */
  public List<String> getRequiredFields(Method method) {
    MessageType inputMessage = method.getInputMessage();
    return inputMessage
        .getFields()
        .stream()
        .filter(this::isFieldRequired)
        .map(Field::getSimpleName)
        .collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  /* Returns if a field is required, according to the proto annotations. */
  private boolean isFieldRequired(Field field) {
    List<EnumValueDescriptor> fieldBehaviors =
        getProtoExtensionForEnumValue(field, FieldBehaviorProto.fieldBehavior);
    return fieldBehaviors != null
        && fieldBehaviors.contains(FieldBehavior.REQUIRED.getValueDescriptor());
  }

  /** Return whether the method has the HttpRule for GET. */
  public boolean isHttpGetMethod(Method method) {
    HttpRule httpRule = getHttpRule(method);
    if (httpRule == null) {
      return false;
    }
    return !Strings.isNullOrEmpty(httpRule.getGet());
  }

  /** The hostname for this service (e.g. "foo.googleapis.com"). */
  public String getServiceAddress(Interface service) {
    return getProtoExtension(service, ClientProto.defaultHost);
  }

  /** The OAuth scopes for this service (e.g. "https://cloud.google.com/auth/cloud-platform"). */
  public List<String> getAuthScopes(Interface service) {
    String oAuth = getProtoExtension(service, ClientProto.oauthScopes);
    if (!Strings.isNullOrEmpty(oAuth)) {
      return ImmutableList.copyOf(
          Arrays.stream(oAuth.split(",")).map(String::trim).collect(Collectors.toList()));
    }
    return ImmutableList.of();
  }

  public String getProtoPackage(ProtoFile file) {
    return file.getProto().getPackage();
  }

  @VisibleForTesting
  // Exposed for test mocking.
  public String getProtoPackage(Method method) {
    return getProtoPackage(method.getFile());
  }

  /** Register all extensions needed to process API protofiles. */
  public static void registerAllExtensions(ExtensionRegistry extensionRegistry) {
    OperationsProto.registerAllExtensions(extensionRegistry);
    AnnotationsProto.registerAllExtensions(extensionRegistry);
    ClientProto.registerAllExtensions(extensionRegistry);
    ResourceProto.registerAllExtensions(extensionRegistry);
    FieldBehaviorProto.registerAllExtensions(extensionRegistry);
  }
}

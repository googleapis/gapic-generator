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
package com.google.api.codegen.transformer;

import com.google.api.codegen.OutputSpec;
import com.google.api.codegen.SampleParameters;
import com.google.api.codegen.SampleValueSet;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.SampleParameterConfig;
import com.google.api.codegen.config.SampleSpec.SampleType;
import com.google.api.codegen.config.SampleSpec.ValueSetAndTags;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.MethodSampleView;
import com.google.api.codegen.viewmodel.OutputView;
import com.google.api.codegen.viewmodel.SampleValueSetView;
import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A class that performs the transformations needed to generate the MethodSampleView for the
 * applicable sample types.
 */
public class SampleTransformer {

  private final SampleType sampleType;
  private final OutputTransformer outputTransformer;

  /**
   * A functional interface provided by clients to generate an InitCodeView given an
   * InitCodeContext.
   */
  @FunctionalInterface
  public interface Generator {
    InitCodeView generate(InitCodeContext initCodeContext);
  }

  /**
   * Constructs the SampleTransfomer
   *
   * @param sampleType The SampleType this transformer should concern itself with. Other SampleTypes
   *     configured on the methods are ignored.
   */
  public SampleTransformer(SampleType sampleType) {
    this(sampleType, new OutputTransformer());
  }

  public SampleTransformer(SampleType sampleType, OutputTransformer outputTransformer) {
    this.sampleType = sampleType;
    this.outputTransformer = outputTransformer;
  }

  /**
   * A placeholder id for the ValueSet we synthesize in generateSamples() to accommodate legacy
   * configurations that still rely on sample_code_init_fields. The format of this string is
   * purposefully set to something that will cause errors if it makes it to an artifact.
   */
  private static final String INIT_CODE_SHIM = "[ shim for initcode ]";

  /**
   * Populates {@code methodViewBuilder.samples} with the appropriate {@code MethodSampleView}s.
   * This method provides backward compatibility for configuring in-code samples via {@code
   * MethodView.initCode}.
   *
   * @param methodViewBuilder
   * @param context
   * @param fieldConfigs
   * @param initCodeOutputType
   * @param generator The function (typically a lambda) to generate the InitCode for a sample given
   *     an InitCodeContext
   * @param callingForms The list of calling forms applicable to this method, for which we will
   *     generate samples if so configured via {@code context.getMethodConfig()}
   */
  public void generateSamples(
      ApiMethodView.Builder methodViewBuilder,
      MethodContext context,
      Collection<FieldConfig> fieldConfigs,
      InitCodeOutputType initCodeOutputType,
      Generator generator,
      List<CallingForm> callingForms) {
    generateSamples(
        methodViewBuilder,
        context,
        null,
        fieldConfigs,
        initCodeOutputType,
        generator,
        callingForms);
  }

  /**
   * Populates {@code methodViewBuilder.samples} with the appropriate {@code MethodSampleView}s.
   * This method provides backward compatibility for configuring in-code samples via {@code
   * MethodView.initCode}.
   *
   * <p>TODO: Once MethodView.initCode is removed, this function becomes a one-liner (calling the
   * overloaded form of generateSamples) that should then be inlined at the call sites:
   * methodViewBuilder.samples( generateSamples(context, initContext, fieldConfigs,
   * initCodeOutputType, generator, callingForms));
   *
   * @param methodViewBuilder
   * @param context
   * @param initContext If null, an {@code InitCodeContext} will be created for each sample based on
   *     the {@code fieldConfigs} and {@code initCodeOutputType}.
   * @param fieldConfigs Only used if {@code initContext} is null to create an {@code
   *     InitCodeContext}. Can be null otherwise.
   * @param initCodeOutputType Only used if {@code initContext} is null to create an {@code
   *     InitCodeContext}. Can be null otherwise.
   * @param generator The function (typically a lambda) to generate the InitCode for a sample given
   *     an InitCodeContext
   * @param callingForms The list of calling forms applicable to this method, for which we will
   *     generate samples if so configured via {@code context.getMethodConfig()}
   */
  public void generateSamples(
      ApiMethodView.Builder methodViewBuilder,
      MethodContext context,
      InitCodeContext initContext,
      Collection<FieldConfig> fieldConfigs,
      InitCodeOutputType initCodeOutputType,
      Generator generator,
      List<CallingForm> callingForms) {

    List<MethodSampleView> methodSampleViews =
        generateSamples(
            context, initContext, fieldConfigs, initCodeOutputType, generator, callingForms);

    // Until we get rid of the non-nullable StaticLangApiMethodView.initCode field, set it to a
    // non-null value. For an in-code sample not specified through the SampleSpec, the correct value
    // to
    // use is the first one on the list, since we set it in the overload of generateSamples.
    if (methodSampleViews.size() > 0) {
      methodViewBuilder.initCode(methodSampleViews.get(0).methodInitCode());

      // Don't emit samples that were specifically generated for backward-compatibility. We only
      // wanted them for initCode above.
      if (methodSampleViews.get(0).valueSet().id().equals(INIT_CODE_SHIM)) {
        methodSampleViews = Collections.emptyList();
      }
    }

    methodViewBuilder.samples(methodSampleViews);
  }

  /**
   * Returns a list of MethodSampleViews for the given MethodContext.
   *
   * @param initContext
   * @param methodContext
   * @param fieldConfigs
   * @param initCodeOutputType
   * @param sampleGenerator The function (typically a lambda) to generate the InitCode for a sample
   *     given an InitCodeContext
   * @param callingForms The list of calling forms applicable to this method, for which we will
   *     generate samples if so configured via context.getMethodConfig()
   * @return A list of of the MethodSampleView, each of which corresponds to a specific sample
   *     forthe method
   */
  public List<MethodSampleView> generateSamples(
      MethodContext methodContext,
      InitCodeContext initContext,
      Collection<FieldConfig> fieldConfigs,
      InitCodeOutputType initCodeOutputType,
      Generator sampleGenerator,
      List<CallingForm> callingForms) {

    List<MethodSampleView> methodSampleViews = new ArrayList<>();

    for (CallingForm form : callingForms) {
      MethodConfig methodConfig = methodContext.getMethodConfig();

      List<ValueSetAndTags> matchingValueSets =
          methodConfig.getSampleSpec().getMatchingValueSets(form, sampleType);

      // For backwards compatibility in the configs, we need to use sample_code_init_fields instead
      // to generate the samples in various scenarios. Once all the configs have been migrated to
      // use the SampleSpec, we can delete the code below as well as sample_code_init_fields.
      String id = null;
      if (sampleType == SampleType.IN_CODE) {
        // for IN_CODE, have the source of truth be sample_code_init_fields for
        // now even if otherwise configured
        id = "sample_code_init_field";
      } else if (!methodConfig.getSampleSpec().isConfigured()
          // if not configured, make the sample_code_init_fields available to
          // all sample types
          || matchingValueSets.isEmpty()) { // ApiMethodView.initCode still needs to be set for now
        id = INIT_CODE_SHIM;
      }
      if (id != null) {
        SampleValueSet valueSet =
            SampleValueSet.newBuilder()
                .setParameters(
                    SampleParameters.newBuilder()
                        .addAllDefaults(methodConfig.getSampleCodeInitFields())
                        .build())
                .setId(id)
                .setDescription("value set imported from sample_code_init_fields")
                .setTitle("Sample Values")
                .build();
        matchingValueSets =
            ImmutableList.of(ValueSetAndTags.newBuilder().values(valueSet).regionTag("").build());
      }

      for (ValueSetAndTags setAndTag : matchingValueSets) {
        // Don't overwrite the initContext in outer scope.
        InitCodeContext thisContext = initContext;
        SampleValueSet valueSet = setAndTag.values();
        if (thisContext == null) {
          thisContext =
              createInitCodeContext(
                  methodContext,
                  fieldConfigs,
                  initCodeOutputType,
                  valueSet.getParameters().getDefaultsList(),
                  valueSet
                      .getParameters()
                      .getAttributesList()
                      .stream()
                      .collect(
                          ImmutableMap.toImmutableMap(
                              attr -> attr.getParameter(),
                              attr ->
                                  SampleParameterConfig.newBuilder()
                                      .identifier(attr.getParameter())
                                      .readFromFile(attr.getReadFile())
                                      .sampleArgumentName(attr.getSampleArgumentName())
                                      .build())));
        }
        InitCodeView initCodeView = sampleGenerator.generate(thisContext);
        List<OutputSpec> outputs = valueSet.getOnSuccessList();
        if (outputs.isEmpty()) {
          outputs = OutputTransformer.defaultOutputSpecs(methodContext.getMethodModel());
        }

        ImmutableList<OutputView> outputViews =
            outputTransformer.toViews(outputs, methodContext, valueSet);

        methodSampleViews.add(
            MethodSampleView.newBuilder()
                .callingForm(form)
                .valueSet(SampleValueSetView.of(valueSet))
                .methodInitCode(initCodeView)
                .outputs(outputViews)
                .outputImports(
                    outputTransformer
                        .getOutputImportTransformer()
                        .generateOutputImports(methodContext, outputViews))
                .regionTag(
                    regionTagFromSpec(
                        setAndTag.regionTag(),
                        methodContext.getMethodModel().getSimpleName(),
                        form,
                        valueSet.getId()))
                .sampleFunctionName(
                    methodContext.getNamer().getSampleFunctionName(methodContext.getMethodModel()))
                .build());
      }
    }
    return methodSampleViews;
  }

  /**
   * Creates a region tag from the spec, replacing any {@code %m}, {@code %c}, and {@code %v}
   * placeholders with the method name, calling form name, and value set id, respectively.
   */
  private static String regionTagFromSpec(
      String spec, String methodName, CallingForm callingForm, String valueSetId) {
    final String DEFAULT_REGION_SPEC = "sample";

    if (Strings.isNullOrEmpty(spec)) {
      spec = DEFAULT_REGION_SPEC;
    }
    return spec.replace("%m", CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, methodName))
        .replace("%c", callingForm.toLowerCamel())
        .replace("%v", valueSetId);
  }

  private InitCodeContext createInitCodeContext(
      MethodContext context,
      Collection<FieldConfig> fieldConfigs,
      InitCodeOutputType initCodeOutputType,
      List<String> sampleCodeDefaultValues,
      ImmutableMap<String, SampleParameterConfig> sampleParamConfigMap) {
    return InitCodeContext.newBuilder()
        .initObjectType(context.getMethodModel().getInputType())
        .suggestedName(Name.from("request"))
        .initFieldConfigStrings(sampleCodeDefaultValues)
        .sampleParamConfigMap(sampleParamConfigMap)
        .initValueConfigMap(InitCodeTransformer.createCollectionMap(context))
        .initFields(FieldConfig.toFieldTypeIterable(fieldConfigs))
        .outputType(initCodeOutputType)
        .fieldConfigMap(FieldConfig.toFieldConfigMap(fieldConfigs))
        .build();
  }
}

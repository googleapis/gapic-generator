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

import com.google.api.codegen.SampleValueSet;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.SampleSpec.SampleType;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.MethodSampleView;
import com.google.api.codegen.viewmodel.SampleValueSetView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class that performs the transformations needed to generate the MethodSampleView for the
 * applicable sample types.
 */
public class SampleTransformer {

  private final SampleType sampleType;

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
    this.sampleType = sampleType;
  }

  /**
   * A placeholder id for the ValueSet we synthesize in generateSamples() to accommodate legacy
   * configurations that still rely on sample_code_init_fields. The format of this string is
   * purposefully set to something that will cause errors if it makes it to an artifact.
   */
  private static final String LEGACY_SAMPLE_CODE_INIT_VALUES = "[ sample_code_init_fields ]";

  /**
   * Populates methodViewBuilder.samples with the appropriate MethodSampleViews. This method is
   * provided to provide backward compatibility for configuring in-code samples via
   * MethodView.initCode. Once that configuration is removed, this function becomes a one-liner that
   * should then be inlined at the callsites.
   *
   * @param methodViewBuilder
   * @param context
   * @param fieldConfigs
   * @param initCodeOutputType
   * @param generator The function (typically a lambda) to generate the InitCode for a sample given
   *     an InitCodeContext
   * @param callingForms The list of calling forms applicable to this method, for which we will
   *     generate samples if so configured via context.getMethodConfig()
   */
  public void generateSamples(
      ApiMethodView.Builder methodViewBuilder,
      MethodContext context,
      Iterable<FieldConfig> fieldConfigs,
      InitCodeOutputType initCodeOutputType,
      Generator generator,
      List<CallingForm> callingForms) {

    List<MethodSampleView> methodSampleViews =
        generateSamples(context, fieldConfigs, initCodeOutputType, generator, callingForms);

    // Until we get rid of the non-nullable StaticLangApiMethodView.initCode field, set it to a
    // non-null value. For an in-code sample not specified through the SampleSpec, the correct value to
    // use is the first one on the list, since we set it in the overload of generateSamples.
    if (methodSampleViews.size() > 0) {
      methodViewBuilder.initCode(methodSampleViews.get(0).initCode());

      // Don't emit samples that were specifically generated for backward-compatibility. We only
      // wanted them for initCode above.
      if (methodSampleViews.get(0).valueSet().id().equals(LEGACY_SAMPLE_CODE_INIT_VALUES)) {
        methodSampleViews = Collections.emptyList();
      }
    }

    methodViewBuilder.samples(methodSampleViews);
  }

  /**
   * Returns a list of MethodSampleViews for the given MethodContext.
   *
   * @param context
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
      MethodContext context,
      Iterable<FieldConfig> fieldConfigs,
      InitCodeOutputType initCodeOutputType,
      Generator sampleGenerator,
      List<CallingForm> callingForms) {

    List<MethodSampleView> methodSampleViews = new ArrayList<>();

    for (CallingForm form : callingForms) {
      MethodConfig methodConfig = context.getMethodConfig();

      List<SampleValueSet> matchingValueSets =
          methodConfig.getSampleSpec().getMatchingValueSets(form, sampleType);

      // For backwards compatibility in the configs, we need to use sample_code_init_fields instead
      // to generate the samples in various scenarios. Once all the configs have been migrated to
      // use the SampleSpec, we can delete the code below as well as sample_code_init_fields.
      if (!methodConfig
              .getSampleSpec()
              .isConfigured() // if not configured, make the sample_code_init_fields available to all sample types
          || sampleType
              == SampleType
                  .IN_CODE // for IN_CODE, have the source of truth be sample_code_init_fields for now even if otherwise configured
          || matchingValueSets.isEmpty()) { // ApiMethodView.initCode still needs to be set for now
        matchingValueSets =
            Collections.singletonList(
                SampleValueSet.newBuilder()
                    .addAllParameters(methodConfig.getSampleCodeInitFields())
                    .setId(LEGACY_SAMPLE_CODE_INIT_VALUES) // only use these samples for initCode
                    .setDescription("value set imported from sample_code_init_fields")
                    .setTitle("Sample Values")
                    .build());
      }

      for (SampleValueSet valueSet : matchingValueSets) {
        InitCodeView initCodeView =
            sampleGenerator.generate(
                createInitCodeContext(
                    context, fieldConfigs, initCodeOutputType, valueSet.getParametersList()));
        methodSampleViews.add(
            MethodSampleView.newBuilder()
                .callingForm(form)
                .valueSet(SampleValueSetView.of(valueSet))
                .initCode(initCodeView)
                .build());
      }
    }
    return methodSampleViews;
  }

  private InitCodeContext createInitCodeContext(
      MethodContext context,
      Iterable<FieldConfig> fieldConfigs,
      InitCodeOutputType initCodeOutputType,
      List<String> sampleCodeInitFields) {
    return InitCodeContext.newBuilder()
        .initObjectType(context.getMethodModel().getInputType())
        .suggestedName(Name.from("request"))
        .initFieldConfigStrings(sampleCodeInitFields)
        .initValueConfigMap(InitCodeTransformer.createCollectionMap(context))
        .initFields(FieldConfig.toFieldTypeIterable(fieldConfigs))
        .outputType(initCodeOutputType)
        .fieldConfigMap(FieldConfig.toFieldConfigMap(fieldConfigs))
        .build();
  }
}

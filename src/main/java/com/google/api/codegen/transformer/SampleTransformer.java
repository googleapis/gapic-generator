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
import com.google.api.codegen.SampleInitAttribute;
import com.google.api.codegen.SampleParameters;
import com.google.api.codegen.SampleValueSet;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodContext;
import com.google.api.codegen.config.OutputContext;
import com.google.api.codegen.config.SampleConfig;
import com.google.api.codegen.config.SampleContext;
import com.google.api.codegen.config.SampleParameterConfig;
import com.google.api.codegen.config.SampleSpec.SampleType;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.samplegen.v1.RequestFieldProto;
import com.google.api.codegen.samplegen.v1.ResponseStatementProto;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.MethodSampleView;
import com.google.api.codegen.viewmodel.OutputView;
import com.google.api.codegen.viewmodel.SampleFunctionDocView;
import com.google.api.codegen.viewmodel.SampleFunctionParameterView;
import com.google.auto.value.AutoValue;
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
@AutoValue
public abstract class SampleTransformer {

  private static final int DOC_LINE_MAX_WIDTH = 80;

  private static final InitCodeTransformer defaultInitCodeTransformer = new InitCodeTransformer();
  private static final OutputTransformer defaultOutputTransformer = new OutputTransformer();
  private static final SampleImportTransformer defaultSampleImportTransformer =
      new StandardSampleImportTransformer(new StandardImportSectionTransformer());

  public abstract SampleType sampleType();

  public abstract InitCodeTransformer initCodeTransformer();

  public abstract OutputTransformer outputTransformer();

  public abstract SampleImportTransformer sampleImportTransformer();

  public static Builder newBuilder() {
    return new AutoValue_SampleTransformer.Builder()
        .sampleType(SampleType.IN_CODE)
        .initCodeTransformer(defaultInitCodeTransformer)
        .outputTransformer(defaultOutputTransformer)
        .sampleImportTransformer(defaultSampleImportTransformer);
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder sampleType(SampleType val);

    public abstract Builder initCodeTransformer(InitCodeTransformer val);

    public abstract Builder outputTransformer(OutputTransformer val);

    public abstract Builder sampleImportTransformer(SampleImportTransformer val);

    public abstract SampleTransformer build();
  }

  /**
   * Constructs the SampleTransfomer
   *
   * @param sampleType The SampleType this transformer should concern itself with. Other SampleTypes
   *     configured on the methods are ignored.
   */
  public static SampleTransformer create(SampleType sampleType) {
    return newBuilder().sampleType(sampleType).build();
  }

  public static SampleTransformer create(
      SampleType sampleType, OutputTransformer outputTransformer) {
    return newBuilder().sampleType(sampleType).outputTransformer(outputTransformer).build();
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
   * @param callingForms The list of calling forms applicable to this method, for which we will
   *     generate samples if so configured via {@code context.getMethodConfig()}
   */
  public void generateSamples(
      ApiMethodView.Builder methodViewBuilder,
      MethodContext context,
      Collection<FieldConfig> fieldConfigs,
      InitCodeOutputType initCodeOutputType,
      List<CallingForm> callingForms) {
    generateSamples(
        methodViewBuilder, context, null, fieldConfigs, initCodeOutputType, callingForms);
  }

  /**
   * Populates {@code methodViewBuilder.samples} with the appropriate {@code MethodSampleView}s.
   * This method provides backward compatibility for configuring in-code samples via {@code
   * MethodView.initCode}.
   *
   * <p>TODO: Once MethodView.initCode is removed, this function becomes a one-liner (calling the
   * overloaded form of generateSamples) that should then be inlined at the call sites:
   * methodViewBuilder.samples( generateSamples(context, initContext, fieldConfigs,
   * initCodeOutputType, callingForms));
   *
   * @param methodViewBuilder
   * @param context
   * @param initContext If null, an {@code InitCodeContext} will be created for each sample based on
   *     the {@code fieldConfigs} and {@code initCodeOutputType}.
   * @param fieldConfigs Only used if {@code initContext} is null to create an {@code
   *     InitCodeContext}. Can be null otherwise.
   * @param initCodeOutputType Only used if {@code initContext} is null to create an {@code
   *     InitCodeContext}. Can be null otherwise.
   * @param callingForms The list of calling forms applicable to this method, for which we will
   *     generate samples if so configured via {@code context.getMethodConfig()}
   */
  public void generateSamples(
      ApiMethodView.Builder methodViewBuilder,
      MethodContext context,
      InitCodeContext initContext,
      Collection<FieldConfig> fieldConfigs,
      InitCodeOutputType initCodeOutputType,
      List<CallingForm> callingForms) {

    List<MethodSampleView> methodSampleViews =
        generateSamples(context, initContext, fieldConfigs, initCodeOutputType, callingForms);

    // Until we get rid of the non-nullable StaticLangApiMethodView.initCode field, set it to a
    // non-null value. For an in-code sample not specified through the SampleSpec, the correct value
    // to
    // use is the first one on the list, since we set it in the overload of generateSamples.
    if (methodSampleViews.size() > 0) {
      methodViewBuilder.initCode(methodSampleViews.get(0).sampleInitCode());

      // Don't emit samples that were specifically generated for backward-compatibility. We only
      // wanted them for initCode above.
      if (methodSampleViews.get(0).id().equals(INIT_CODE_SHIM)) {
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
      List<CallingForm> callingForms) {

    CallingForm defaultCallingForm = methodContext.getNamer().getDefaultCallingForm(methodContext);
    List<MethodSampleView> methodSampleViews = new ArrayList<>();
    MethodConfig methodConfig = methodContext.getMethodConfig();
    SampleValueSet defaultValueSet = defaultValueSet(methodConfig);

    for (SampleConfig sampleConfig :
        methodConfig
            .getSampleSpec()
            .getSampleConfigs(callingForms, defaultCallingForm, defaultValueSet, sampleType())) {
      InitCodeContext thisContext = initContext; // Do not override outer initContext
      if (thisContext == null) {
        thisContext =
            createInitCodeContext(
                methodContext, fieldConfigs, initCodeOutputType, sampleConfig.valueSet());
      }
      methodSampleViews.add(generateSample(sampleConfig, methodContext, thisContext));
    }
    return methodSampleViews;
  }

  // entry point for generating standalone samples using sample config.
  public MethodSampleView generateSample(MethodContext methodContext, SampleContext sampleContext) {
    // add new type table in sample context?
    methodContext = methodContext.cloneWithEmptyTypeTable();

    // request
    InitCodeContext initCodeContext = createInitCodeContext(methodContext, sampleContext);
    InitCodeView initCodeView =
        initCodeTransformer().generateInitCode(methodContext, initCodeContext);

    if (initCodeView == null || initCodeView.topLevelIndexFileImportName() == null) {
      System.out.println(sampleContext.sampleConfig().id());
    }

    // response
    OutputContext outputContext = OutputContext.create();
    List<ResponseStatementProto> outputs = sampleContext.sampleConfig().responseConfigs();
    if (outputs.isEmpty()) {
      outputs = OutputTransformer.defaultResponseStatements(methodContext);
    }
    ImmutableList<OutputView> outputViews =
        outputTransformer().toViews(outputs, methodContext, sampleContext, outputContext);

    // imports
    ImportSectionView sampleImportSectionView =
        sampleImportTransformer()
            .generateImportSection(
                methodContext.cloneWithEmptyTypeTable(),
                sampleContext.callingForm(),
                outputContext,
                methodContext.getTypeTable(),
                initCodeTransformer()
                    .getInitCodeNodes(
                        methodContext,
                        initCodeContext
                            .cloneWithEmptySymbolTable())); // to avoid symbol collision);

    // Documentation
    SampleFunctionDocView sampleFunctionDocView =
        SampleFunctionDocView.newBuilder()
            .paramDocLines(paramDocLines(methodContext, initCodeView))
            .mainDocLines(
                ImmutableList.<String>builder()
                    .addAll(
                        methodContext
                            .getNamer()
                            .getWrappedDocLines(sampleContext.sampleConfig().description(), true))
                    .build())
            .build();

    // metadata
    ImmutableList<String> metadataDescription =
        ImmutableList.<String>builder()
            .addAll(
                methodContext
                    .getNamer()
                    .getWrappedDocLines(sampleContext.sampleConfig().description(), false))
            .build();

    String descriptionLine = metadataDescription.isEmpty() ? "" : metadataDescription.get(0);
    ImmutableList<String> additionalDescriptionLines =
        metadataDescription.isEmpty()
            ? ImmutableList.of()
            : metadataDescription.subList(1, metadataDescription.size());

    // assemble
    return MethodSampleView.newBuilder()
        .id(sampleContext.uniqueSampleId())
        .callingForm(sampleContext.callingForm())
        .sampleInitCode(initCodeView)
        .outputs(outputViews)
        .hasMultipleFileOutputs(outputContext.hasMultipleFileOutputs())
        .usesAsyncAwaitPattern(
            methodContext
                .getNamer()
                .usesAsyncAwaitPattern(sampleContext.callingForm())) // Used by C# and Node.js
        .sampleImports(sampleImportSectionView)
        .regionTag(sampleContext.sampleConfig().regionTag())
        .sampleFunctionName(
            methodContext.getNamer().getSampleFunctionName(methodContext.getMethodModel()))
        .sampleFunctionDoc(sampleFunctionDocView)
        .title(sampleContext.sampleConfig().title())
        .descriptionLine(descriptionLine)
        .additionalDescriptionLines(additionalDescriptionLines)
        .build();
  }

  private MethodSampleView generateSample(
      SampleConfig config, MethodContext methodContext, InitCodeContext initCodeContext) {
    methodContext = methodContext.cloneWithEmptyTypeTable();
    InitCodeView initCodeView =
        initCodeTransformer().generateInitCode(methodContext, initCodeContext);
    SampleValueSet valueSet = config.valueSet();
    CallingForm form = config.callingForm();
    String regionTag = config.regionTag();
    List<OutputSpec> outputs = valueSet.getOnSuccessList();
    if (outputs.isEmpty()) {
      outputs = OutputTransformer.defaultOutputSpecs(methodContext);
    }
    OutputContext outputContext = OutputContext.create();
    ImmutableList<OutputView> outputViews =
        outputTransformer().toViews(outputs, methodContext, valueSet, form, outputContext);
    ImportSectionView sampleImportSectionView =
        sampleImportTransformer()
            .generateImportSection(
                methodContext.cloneWithEmptyTypeTable(),
                form,
                outputContext,
                methodContext.getTypeTable(),
                initCodeTransformer()
                    .getInitCodeNodes(
                        methodContext,
                        initCodeContext
                            .cloneWithEmptySymbolTable())); // to avoid symbol collision);
    SampleFunctionDocView sampleFunctionDocView =
        SampleFunctionDocView.newBuilder()
            .paramDocLines(paramDocLines(methodContext, initCodeView))
            .mainDocLines(
                ImmutableList.<String>builder()
                    .addAll(
                        methodContext
                            .getNamer()
                            .getWrappedDocLines(valueSet.getDescription(), true))
                    .build())
            .build();

    ImmutableList<String> metadataDescription =
        ImmutableList.<String>builder()
            .addAll(methodContext.getNamer().getWrappedDocLines(valueSet.getDescription(), false))
            .build();

    String descriptionLine = metadataDescription.isEmpty() ? "" : metadataDescription.get(0);
    ImmutableList<String> additionalDescriptionLines =
        metadataDescription.isEmpty()
            ? ImmutableList.of()
            : metadataDescription.subList(1, metadataDescription.size());

    return MethodSampleView.newBuilder()
        .callingForm(form)
        .id(valueSet.getId())
        .sampleInitCode(initCodeView)
        .outputs(outputViews)
        .hasMultipleFileOutputs(outputContext.hasMultipleFileOutputs())
        .usesAsyncAwaitPattern(
            methodContext.getNamer().usesAsyncAwaitPattern(form)) // Used by C# and Node.js
        .sampleImports(sampleImportSectionView)
        .regionTag(
            regionTagFromSpec(
                regionTag, methodContext.getMethodModel().getSimpleName(), form, valueSet.getId()))
        .sampleFunctionName(
            methodContext.getNamer().getSampleFunctionName(methodContext.getMethodModel()))
        .sampleFunctionDoc(sampleFunctionDocView)
        .title(config.valueSet().getTitle())
        .descriptionLine(descriptionLine)
        .additionalDescriptionLines(additionalDescriptionLines)
        .build();
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
      SampleValueSet valueSet) {
    return InitCodeContext.newBuilder()
        .initObjectType(context.getMethodModel().getInputType())
        .suggestedName(Name.from("request"))
        .initFieldConfigStrings(valueSet.getParameters().getDefaultsList())
        .sampleParamConfigMap(sampleParamConfigMapFromValueSet(valueSet))
        .initValueConfigMap(InitCodeTransformer.createCollectionMap(context))
        .initFields(FieldConfig.toFieldTypeIterable(fieldConfigs))
        .outputType(initCodeOutputType)
        .fieldConfigMap(FieldConfig.toFieldConfigMap(fieldConfigs))
        .build();
  }

  private InitCodeContext createInitCodeContext(
      MethodContext methodContext, SampleContext sampleContext) {
    // Temporary solution
    List<String> initFieldConfigStrings =
        sampleContext
            .sampleConfig()
            .requestConfigs()
            .stream()
            .filter(req -> !req.getValue().isEmpty())
            .map(req -> String.format("%s=%s", req.getField(), req.getValue()))
            .collect(ImmutableList.toImmutableList());
    List<FieldConfig> requiredFieldConfigs =
        methodContext.getMethodConfig().getRequiredFieldConfigs();
    return InitCodeContext.newBuilder()
        .initObjectType(methodContext.getMethodModel().getInputType())
        .suggestedName(Name.from("request"))
        .initFieldConfigStrings(initFieldConfigStrings)
        .sampleParamConfigMap(
            sampleParamConfigMapFromRequestConfigs(sampleContext.sampleConfig().requestConfigs()))
        .initValueConfigMap(InitCodeTransformer.createCollectionMap(methodContext))
        .initFields(FieldConfig.toFieldTypeIterable(requiredFieldConfigs))
        .outputType(sampleContext.initCodeOutputType())
        .fieldConfigMap(FieldConfig.toFieldConfigMap(requiredFieldConfigs))
        .build();
  }

  private SampleValueSet defaultValueSet(MethodConfig methodConfig) {
    // For backwards compatibility in the configs, we need to use sample_code_init_fields instead
    // to generate the samples in various scenarios. Once all the configs have been migrated to
    // use the SampleSpec, we can delete the code below as well as sample_code_init_fields.
    String defaultId =
        (sampleType() == SampleType.IN_CODE) ? "sample_code_init_field" : INIT_CODE_SHIM;

    return SampleValueSet.newBuilder()
        .setParameters(
            SampleParameters.newBuilder()
                .addAllDefaults(methodConfig.getSampleCodeInitFields())
                .build())
        .setId(defaultId)
        .setDescription("value set imported from sample_code_init_fields")
        .setTitle("Sample Values")
        .build();
  }

  private ImmutableMap<String, SampleParameterConfig> sampleParamConfigMapFromValueSet(
      SampleValueSet valueSet) {
    ImmutableMap.Builder<String, SampleParameterConfig> builder = ImmutableMap.builder();
    for (SampleInitAttribute attr : valueSet.getParameters().getAttributesList()) {
      String field = attr.getParameter();
      SampleParameterConfig config =
          SampleParameterConfig.newBuilder()
              .field(field)
              .isFile(attr.getReadFile())
              .inputParameter(attr.getSampleArgumentName())
              .comment(attr.getDescription())
              .build();
      builder.put(field, config);
    }
    return builder.build();
  }

  private ImmutableMap<String, SampleParameterConfig> sampleParamConfigMapFromRequestConfigs(
      List<RequestFieldProto> requests) {
    ImmutableMap.Builder<String, SampleParameterConfig> builder = ImmutableMap.builder();
    for (RequestFieldProto request : requests) {
      String field = request.getField();
      SampleParameterConfig config =
          SampleParameterConfig.newBuilder()
              .field(field)
              .isFile(request.getValueIsFile())
              .inputParameter(request.getInputParameter())
              .comment(request.getComment())
              .build();
      builder.put(field, config);
    }
    return builder.build();
  }

  /** Generate parameter descriptions in sample function documentation. */
  private ImmutableList<List<String>> paramDocLines(
      MethodContext context, InitCodeView initCodeView) {
    SurfaceNamer namer = context.getNamer();
    ImmutableList.Builder<List<String>> builder = ImmutableList.builder();
    for (SampleFunctionParameterView param : initCodeView.argDefaultParams()) {
      if (param.description().isEmpty()) {
        continue;
      }
      List<String> paramDoc =
          namer.getWrappedDocLines(
              namer.getParamDocText(param.identifier(), param.typeName(), param.description()),
              false);
      builder.add(paramDoc);
    }
    return builder.build();
  }
}

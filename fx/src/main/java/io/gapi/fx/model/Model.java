package io.gapi.fx.model;

import com.google.api.Service;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.inject.Key;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;
import com.google.protobuf.UInt32Value;

import io.gapi.fx.model.stages.Merged;
import io.gapi.fx.model.stages.Normalized;
import io.gapi.fx.model.stages.Requires;
import io.gapi.fx.model.stages.Resolved;

import java.io.File;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Model of an API service. Also manages processing pipelines and accumulation of
 * diagnostics.
 */
public class Model extends Element implements DiagCollector {

  // The proto files that will be excluded when generating API service configuration.
  // Only put files that are ABSOLUTELY UNRELATED to the API Service but are
  // still included in the FileDescriptorSet by protoc. Each file should be
  // accompanied by a comment justifying the reason for exclusion.
  // This is to workaround b/15001812.
  private static final Set<String> BLACK_LISTED_FILES = ImmutableSet.<String>builder()
      // sawzall_message_set.proto is pulled twice in by protoc for some internal reason
      // and caused a duplicate symbol definition.
      .add("net/proto/sawzall_message_set.proto")
      .build();

  // The current default config version.
  private static final int CURRENT_CONFIG_DEFAULT_VERSION = 2;

  // The latest version of tools under development.
  private static final int DEV_CONFIG_VERSION = 2;

  private static final String CORP_DNS_SUFFIX = ".corp.googleapis.com";
  private static final String SANDBOX_DNS_SUFFIX = ".sandbox.googleapis.com";
  private static final String PRIVATE_API_DNS_SUFFIX = "-pa.googleapis.com";

  /**
   * Creates a new model based on the given file descriptor, list of source file
   * names and list of experiments to be enabled for the model.
   */
  public static Model create(FileDescriptorSet proto, Iterable<String> sources,
      Iterable<String> experiments, ExtensionPool extensionPool) {
    return new Model(proto, sources, experiments, extensionPool);
  }

  /**
   * Creates a new model based on the given file descriptor set and list of source file
   * names. The file descriptor set is self-contained and contains the descriptors for
   * the source files as well as for all dependencies.
   */
  public static Model create(FileDescriptorSet proto, Iterable<String> sources) {
    return new Model(proto, sources, null, ExtensionPool.EMPTY);
  }

  /**
   * Creates an model where all protos in the descriptor are considered to be sources.
   */
  public static Model create(FileDescriptorSet proto) {
    return new Model(proto, null, null, ExtensionPool.EMPTY);
  }

  /**
   * Creates a model from a normalized service config, rather than from descriptor and .yaml files.
   */
  // TODO: MIGRATION
  /*
  public static Model create(Service normalizedConfig) {
    FileDescriptorSet regeneratedDescriptor = DescriptorGenerator.generate(normalizedConfig);
    Model model = create(regeneratedDescriptor);

    // Configured with a stripped Service
    Service.Builder builder = normalizedConfig.toBuilder();
    ImmutableList.Builder<Api> strippedApis = ImmutableList.builder();
    for (Api api : normalizedConfig.getApisList()) {
      strippedApis.add(Api.newBuilder().setName(api.getName()).build());
    }
    // NOTE: Documentation may still contain text from the original protos.
    builder.clearEnums();
    builder.clearTypes();
    builder.clearApis();
    builder.addAllApis(strippedApis.build());
    Service strippedConfig = builder.build();
    model.setConfigs(ImmutableList.<Message>of(strippedConfig));

    return model;
  }
  */

  /**
   * Returns the default config version.
   */
  public static int getDefaultConfigVersion() {
    return CURRENT_CONFIG_DEFAULT_VERSION;
  }

  /**
   * Returns the config version under development.
   */
  public static int getDevConfigVersion() {
    return DEV_CONFIG_VERSION;
  }

  private ImmutableList<ProtoFile> files;
  private ImmutableSet<String> experiments;
  private final Map<Key<?>, Processor> processors = Maps.newLinkedHashMap();
  private final List<ConfigAspect> configAspects = Lists.newArrayList();
  private final List<Diag> diags = Lists.newArrayList();
  private Set<String> visibilityLabels;
  private Set<Set<String>> declaredVisibilityCombinations;
  private int errorCount;

  private Model(FileDescriptorSet proto, @Nullable Iterable<String> sources,
      @Nullable Iterable<String> experiments, ExtensionPool extensionPool) {
    System.err.println("Loading model");
    Set<String> sourcesSet = sources == null ? null : Sets.newHashSet(sources);
    ImmutableList.Builder<ProtoFile> builder = ImmutableList.builder();
    // To de-dup FileDescriptorProto in the descriptor set generated by protoc.
    Set<String> includedFiles = Sets.newHashSet();
    for (FileDescriptorProto file : proto.getFileList()) {
      if (BLACK_LISTED_FILES.contains(file.getName()) || includedFiles.contains(file.getName())) {
        continue;
      }
      includedFiles.add(file.getName());
      System.err.println(".. proto file: " + file.getName());
      builder.add(ProtoFile.create(this,  file,
          sourcesSet == null || sourcesSet.contains(file.getName()), extensionPool));
    }
    System.err.flush();
    if (extensionPool.getDescriptor() != null) {
      for (FileDescriptorProto file : extensionPool.getDescriptor().getFileList()) {
        if (BLACK_LISTED_FILES.contains(file.getName()) || includedFiles.contains(file.getName())) {
          continue;
        }
        includedFiles.add(file.getName());
        builder.add(ProtoFile.create(this,  file,
            sourcesSet == null || sourcesSet.contains(file.getName()), extensionPool));
      }
    }
    files = builder.build();
    this.experiments =
        experiments == null ? ImmutableSet.<String>of() : ImmutableSet.copyOf(experiments);
  }

  // -------------------------------------------------------------------------
  // Syntax

  @Override public Model getModel() {
    return this;
  }

  @Override
  public Location getLocation() {
    return SimpleLocation.TOPLEVEL;
  }

  @Override
  public String getFullName() {
    return "APIModel";
  }

  @Override
  public String getSimpleName() {
    return getFullName();
  }

  /**
   * Returns the list of (proto) files.
   */
  public ImmutableList<ProtoFile> getFiles() {
    return files;
  }

  /**
   * Set the list of (proto) files.
   */
  public void setFiles(ImmutableList<ProtoFile> files) {
    this.files = files;
  }

  /**
   * Returns the visibility labels the caller wants to apply to this model.
   * If it is null, no visibility rules will be applied. If it is an empty set,
   * only unrestricted elements will be visible.
   */
  @Nullable public Set<String> getVisibilityLabels() {
    return visibilityLabels;
  }

  /**
   * Set visibility labels from the given list. Validity of labels will be checked
   * in later stages.
   */
  public Set<String> setVisibilityLabels(@Nullable Set<String> visibilityLabels) {
    Set<String> current = this.visibilityLabels;
    this.visibilityLabels = visibilityLabels;
    return current;
  }

  /**
   * Get a collection of declared visibility combinations.
   */
  public Set<Set<String>> getDeclaredVisibilityCombinations() {
    return declaredVisibilityCombinations;
  }

  /**
   * Set a collection of declared visibility combinations.
   */
  public void setDeclaredVisibilityCombinations(Set<Set<String>> declaredVisibilityCombinations) {
    this.declaredVisibilityCombinations = declaredVisibilityCombinations;
  }

  /**
   * Checks whether a given experiment is enabled.
   */
  public boolean isExperimentEnabled(String experiment) {
    return experiments.contains(experiment);
  }

  /**
   * Enables the given experiment (for testing).
   */
  @VisibleForTesting
  public void enableExperiment(String experiment) {
    this.experiments = FluentIterable.from(experiments).append(experiment).toSet();
  }

  // API v1 version suffix.
  private String apiV1VersionSuffix;

  /**
   * Sets API v1 version suffix in the model.
   */
  public void setApiV1VersionSuffix(String value) {
    apiV1VersionSuffix = value;
  }

  /**
   * Gets API v1 version suffix.
   */
  public String getApiV1VersionSuffix() {
    return apiV1VersionSuffix;
  }

  //-------------------------------------------------------------------------
  // Attributes belonging to resolved stage

  @Requires(Resolved.class) private SymbolTable symbolTable;

  /**
   * Returns the symbolTable
   */
  @Requires(Resolved.class) public SymbolTable getSymbolTable() {
    return symbolTable;
  }

  /**
   * For setting the symbol table.
   */
  public void setSymbolTable(SymbolTable symbolTable) {
    this.symbolTable = symbolTable;
  }

  // -------------------------------------------------------------------------
  // Attributes belonging to merged stage

  @Requires(Merged.class) private Service serviceConfig;
  private Scoper scoper = Scoper.UNRESTRICTED;
  private List<ProtoElement> roots = Lists.newArrayList();

  /**
   * Add a root element to the model. Root elements are collected during merging
   * and used to compute the transitively reachable set of referenced elements.
   */
  public void addRoot(ProtoElement root) {
    roots.add(root);
  }

  /**
   * Get the roots collected for the model.
   */
  public Iterable<ProtoElement> getRoots() {
    return roots;
  }

  /**
   * Sets the scoper used for traversing this model. Returns the previous scoper.
   */
  public Scoper setScoper(Scoper scoper) {
    Scoper result = this.scoper;
    this.scoper = scoper;
    return result;
  }

  /**
   * Gets the scoper used for this model.
   */
  public Scoper getScoper() {
    return scoper;
  }

  /**
   * Returns the iterable scoped to the reachable elements.
   */
  public <E extends ProtoElement> Iterable<E> reachable(Iterable<E> elems) {
    return scoper.filter(elems);
  }

  /**
   * Sets the service config from a proto.
   */
  public void setServiceConfig(Service config) {
    this.serviceConfig = config;
  }

  /**
   * Sets the service config based on a sequence of protos of heterogenous types.
   * Protos of the same type will be merged together, and those applicable to the
   * service will be attached to it.
   */
  public void setConfigs(Iterable<Message> configs) {

    // Merge configs of same type.
    Map<Descriptor, Message.Builder> mergedConfigs = Maps.newHashMap();
    for (Message config : configs) {
      Descriptor descriptor = config.getDescriptorForType();
      Message.Builder builder = mergedConfigs.get(descriptor);
      if (builder == null) {
        mergedConfigs.put(descriptor, config.toBuilder());
      } else {
        builder.mergeFrom(config);
      }
    }

    // Pick the configs we know and care about (currently, Service and Legacy).
    Message.Builder serviceConfig = mergedConfigs.get(Service.getDescriptor());
    if (serviceConfig != null) {
      setServiceConfig((Service) serviceConfig.build());
    } else {
      // Set empty config.
      setServiceConfig(
          Service.newBuilder()
              .setConfigVersion(UInt32Value.newBuilder().setValue(Model.getDefaultConfigVersion()))
              .build());
    }

    // TODO: MIGRATION
    /*
    Message.Builder legacyConfig = mergedConfigs.get(Legacy.getDescriptor());
    if (legacyConfig != null) {
      setLegacyConfig((Legacy) legacyConfig.build());
    } else {
      // Set empty config.
      setLegacyConfig(Legacy.getDefaultInstance());
    }
    */
  }

  /**
   * Returns the associated service config.
   */
  @Requires(Merged.class) public Service getServiceConfig() {
    return serviceConfig;
  }

  /**
   * Returns the effective config version. Chooses the current default if the service config does
   * not specify it.
   */
  @Requires(Merged.class) public int getConfigVersion() {
    if (serviceConfig.hasConfigVersion()) {
      return serviceConfig.getConfigVersion().getValue();
    }
    return CURRENT_CONFIG_DEFAULT_VERSION;
  }


  /**
   * Sets config version for testing.
   */
  @VisibleForTesting
  public void setConfigVersionForTesting(int configVersion) {
    serviceConfig = serviceConfig.toBuilder()
        .setConfigVersion(UInt32Value.newBuilder().setValue(configVersion)).build();
  }

  /**
   * Holds the legacy config required for backward compatibility with Apiary v1.
   */
  // TODO: MIGRATION
  //@Requires(Merged.class) private Legacy legacyConfig;

  /**
   * Sets the legacy config
   */
  // TODO: MIGRATION
//  public void setLegacyConfig(Legacy config) {
//    this.legacyConfig = config;
//  }
//
//  /**
//   * Returns the associated legacy config
//   */
//  @Requires(Merged.class) public Legacy getLegacyConfig() {
//    return this.legacyConfig;
//  }


  // -------------------------------------------------------------------------
  // Attributes belonging to normalized stage

  @Requires(Normalized.class) private Service normalizedConfig;

  /**
   * Returns the normalized service config
   */
  @Requires(Normalized.class) public Service getNormalizedConfig() {
    return normalizedConfig;
  }

  public void setNormalizedConfig(Service normalizedConfig) {
    this.normalizedConfig = normalizedConfig;
  }

  // -------------------------------------------------------------------------
  // Diagnosis

  // The user can add directives in comments such as
  //
  // (== suppress_warning http-* ==)
  //
  // This suppresses all lint warnings of the http aspect. Such warnings
  // use an identifier of the form <aspect>-<rule>. In the suppress_warning directive,
  // '*' can be used as a wildcard for <rule>.
  //
  // The underlying implementation maintains a regular expression for each model element
  // which accumulates patterns for all suppression directives associated with this element
  // -- and possibly additional programmatic sources.

  // Regexp to match a suppression directive argument.
  private static final Pattern SUPPRESSION_DIRECTIVE_PATTERN = Pattern.compile(
      "\\s*(?<aspect>[^-]+)-(?<rule>.*)");

  /**
   * Returns a prefix to be used in diag messages for general errors and warnings.
   */
  public static String diagPrefix(String aspectName) {
    return String.format("%s: ", aspectName);
  }

  /**
   * Returns a prefix to be used in diag messages representing linter warnings.
   */
  public static String diagPrefixForLint(String aspectName, String ruleName) {
    return String.format("(lint) %s-%s: ", aspectName, ruleName);
  }

  // Returns a pattern string to match a style warning with given aspect and rule name.
  private static String suppressionPattern(String aspectName, String ruleName) {
    return String.format("\\(lint\\)\\s*%s-%s:.*",
        Pattern.quote(aspectName),
        "*".equals(ruleName) ? "\\w*" : Pattern.quote(ruleName));
  }

  // A map of per-element regexp patterns which characterize suppressed warnings.
  private Map<Element, String> suppressions = Maps.newHashMap();

  // The same as above, but with compiled patterns.
  private Map<Element, Pattern> compiledSuppressions = Maps.newHashMap();

  // Adds a pattern for given element.
  private void addPattern(Element elem, String pattern) {
    String elemPattern = suppressions.get(elem);
    if (elemPattern == null) {
      elemPattern = pattern;
    } else {
      elemPattern = elemPattern + "|(" + pattern + ")";
      compiledSuppressions.remove(elem);
    }
    suppressions.put(elem, elemPattern);
  }

  // Gets a pattern for given element. This compiles the pattern on demand.
  @Nullable private Pattern getPattern(Element elem) {
    Pattern pattern = compiledSuppressions.get(elem);
    if (pattern == null) {
      String source = suppressions.get(elem);
      if (source == null) {
        return null;
      }
      pattern = Pattern.compile(source, Pattern.DOTALL);
      compiledSuppressions.put(elem, pattern);
    }
    return pattern;
  }

  /**
   * Set a filter for warnings based on regular expression for aspect name. Only warnings
   * containing the aspect name pattern are produced.
   */
  @VisibleForTesting
  public void setWarningFilter(@Nullable String aspectNamePattern) {
    // Add as a pattern to the model.
    addPattern(this, "^(?!.*(" + aspectNamePattern + ")).*");
  }

  /**
   * Shortcut for suppressing all warnings.
   */
  public void suppressAllWarnings() {
    addPattern(this, ".*");
  }

  /**
   * Adds a user-level suppression directive. The directive must be given in the form 'aspect-rule',
   * or 'aspect-*' to match any rule. Is used in comments such as '(== suppress_warning http-* ==)'
   * which will suppress all lint warnings generated by the http aspect.
   */
  public void addSupressionDirective(Element elem, String directive) {
    // Validate the directive syntax.
    Matcher matcher = SUPPRESSION_DIRECTIVE_PATTERN.matcher(directive);
    if (!matcher.matches()) {
      addDiag(Diag.error(elem.getLocation(),
          "The warning_supression '%s' does not match the expected pattern "
          + "'<aspect>-<rule>' or '<aspect>-*'.",
          directive));
      return;
    }

    // Get aspect and rule name and search for matching aspect and rule.
    String aspectName = matcher.group("aspect");
    String ruleName = matcher.group("rule");
    ConfigAspect matching = null;
    for (ConfigAspect aspect : configAspects) {
      if (aspect.getAspectName().equals(aspectName)) {
        matching = aspect;
        break;
      }
    }
    if (matching == null) {
      addDiag(Diag.error(elem.getLocation(),
          "The config aspect '%s' used in warning suppression '%s' is unknown.",
          aspectName, directive));
      return;
    }
    if (!"*".equals(ruleName) && !matching.getLintRuleNames().contains(ruleName)) {
      addDiag(Diag.warning(elem.getLocation(),
          "The rule '%s' in aspect '%s' used in warning suppression '%s' is unknown.",
          ruleName, aspectName, directive));
      return;
    }

    // Add the suppression pattern.
    addPattern(elem, suppressionPattern(aspectName, ruleName));
  }

  /**
   * Checks whether the given diagnosis is suppressed for the given element. This checks
   * the suppression pattern for this element and all elements, inserting the model
   * for global suppressions as a virtual parent.
   */
  public boolean isSuppressedDiag(Diag diag, Element elem) {
    if (diag.getKind() != Diag.Kind.WARNING) {
      return false;
    }
    Element current = elem;
    while (current != null) {
      Pattern pattern = getPattern(current);
      if (pattern != null && pattern.matcher(diag.getMessage()).matches()) {
        return true;
      }
      if (current instanceof Model) {
        // Top-most parent tried.
        break;
      }
      if (current instanceof ProtoElement) {
        current = ((ProtoElement) current).getParent();
      } else {
        current = null;
      }
      if (current == null) {
        // Insert the model as a virtual parent.
        current = elem.getModel();
      }
    }
    return false;
  }

  /**
   * Adds diagnosis to the model if it is not suppressed.
   */
  public void addDiagIfNotSuppressed(Object elementOrLocation, Diag diag) {
    if (elementOrLocation instanceof Element
        && isSuppressedDiag(diag, (Element) elementOrLocation)) {
      return;
    }
    addDiag(diag);
  }

  /**
   * Adds a diagnosis.
   */
  @Override
  public void addDiag(Diag diag) {
    diags.add(diag);
    if (diag.getKind() == Diag.Kind.ERROR) {
      errorCount++;
    }
  }

  /**
   * Returns the number of diagnosed proper errors.
   */
  @Override
  public int getErrorCount() {
    return errorCount;
  }

  /**
   * Returns the diagnosis accumulated.
   */
  public List<Diag> getDiags() {
    return diags;
  }

  // -------------------------------------------------------------------------
  // Configuration aspects

  /**
   * Registers the configuration aspect with the model.
   */
  public void registerConfigAspect(ConfigAspect aspect) {
    configAspects.add(aspect);
  }

  /**
   * Returns the registered configuration aspects.
   */
  public Iterable<ConfigAspect> getConfigAspects() {
    return configAspects;
  }

  // -------------------------------------------------------------------------
  // Stage processing

  /**
   * Registers a stage processor. Returns the old processor or null if there wasn't one.
   */
  @Nullable public Processor registerProcessor(Processor processor) {
    return processors.put(processor.establishes(), processor);
  }

  /**
   * Establishes a processing stage. Runs the chain of all processors required to
   * guarantee the given key is attached at the model. Returns true on success.
   */
  public boolean establishStage(Key<?> key) {
    Deque<Key<?>> computing = Queues.newArrayDeque();
    return establishStage(computing, key);
  }

  private boolean establishStage(Deque<Key<?>> computing, Key<?> stage) {
    if (hasAttribute(stage)) {
      return true;
    }
    if (computing.contains(stage)) {
      throw new IllegalStateException(
          String.format("Cyclic dependency of stages: %s => %s",
              Joiner.on(" => ").join(computing), stage));
    }
    computing.addLast(stage);
    Processor processor = processors.get(stage);
    if (processor == null) {
      throw new IllegalArgumentException(
          String.format("No processor registered to establish stage '%s'", stage));
    }
    for (Key<?> subStage : processor.requires()) {
      if (!establishStage(computing, subStage)) {
        return false;
      }
    }
    computing.removeLast();
    if (!processor.run(this)) {
      return false;
    }
    if (!hasAttribute(stage)) {
      throw new IllegalStateException(
          String.format("Processor '%s' failed to establish stage '%s'", processor, stage));
    }
    return true;
  }

  // -------------------------------------------------------------------------
  // Data path.

  private String dataPath = ".";

  /**
   * Returns a search path for data dependencies. The path is a list of directories separated
   * by File.pathSeparator.
   */
  public String getDataPath() {
    return dataPath;
  }

  /**
   * Sets the data dependency search path.
   */
  public void setDataPath(String dataPath) {
    this.dataPath = dataPath;
  }

  /**
   * Finds a file on the data path. Returns null if not found.
   */
  @Nullable
  public File findDataFile(String name) {
    for (String path : Splitter.on(File.pathSeparator).split(dataPath)) {
      File file = new File(path, name);
      if (file.exists()) {
        return file;
      }
    }
    return null;
  }

  /**
   * Returns the control environment string of this model.
   */
  // TODO: MIGRATION
  //public String getControlEnvironment() {
  //  return getServiceConfig().getControl().getEnvironment();
  //}

  @VisibleForTesting
  static boolean isPrivateService(String serviceName) {
    return serviceName.endsWith(PRIVATE_API_DNS_SUFFIX)
        || serviceName.endsWith(SANDBOX_DNS_SUFFIX)
        || serviceName.endsWith(CORP_DNS_SUFFIX);
  }

  /**
   * Returns true if the service is a private API, corp API, or on sandbox.googles.com non
   * production environent.
   */
  public boolean isPrivateService() {
    return isPrivateService(getServiceConfig().getName());
  }

  // -------------------------------------------------------------------------
  // Generation of derived discovery docs.
  private boolean deriveDiscoveryDoc = true;

  /**
   * Returns true if the derived discovery doc should be generated and added into service config.
   */
  public boolean shouldDerivedDiscoveryDoc() {
    return deriveDiscoveryDoc;
  }

  public void enableDiscoveryDocDerivation(boolean generateDerivedDiscovery) {
    this.deriveDiscoveryDoc = generateDerivedDiscovery;
  }
}

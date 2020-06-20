package com.google.api.codegen.bazel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// A class representing versioned API directory.
// For example: google/example/library/v1
class ApiVersionedDir {
  private static final Pattern PROTO_PACKAGE =
      Pattern.compile("(?m)^package\\s+(?<protoPackage>[\\w+\\.]+)\\s*;\\s*$");
  private static final Pattern IMPORTS =
      Pattern.compile("(?m)^import\\s+\"(?<import>[\\w+\\\\./]+)\"\\s*;\\s*$");
  private static final Pattern PROTO_OPTIONS =
      Pattern.compile(
          "(?m)^option\\s+(?<optName>\\w+)\\s+=\\s+\"(?<optValue>[\\w./;\\\\\\-]+)\"\\s*;\\s*$");
  private static final Pattern SERVICE =
      Pattern.compile("(?m)^service\\s+(?<service>\\w+)\\s+(\\{)*\\s*$");
  private static final Pattern LANG_PACKAGES =
      // Yes, this is an unreadable mess. Replace it with proper yaml parsing if regex
      // ever goes worse than this.
      Pattern.compile(
          "(?m)\\b(?<lang>java|python|go|csharp|ruby|php|nodejs)\\s*:\\s*$\\s+"
              + "package_name\\s*:\\s*(?<package>[\\w\\.\\/:\\\\]*)\\s*$\\s+"
              + "(interface_names:"
              + "(?<interfaceNames>(\\s*$\\s+\\w+\\.[\\w\\.]*\\s*:\\s*\\w+\\s*$)*))?");

  private static final Pattern INTERFACE_NAMES =
      Pattern.compile("(?m)\\s*(?<name>[\\w\\.]*)\\s*:\\s*(?<val>\\w+)\\s*");

  private static Pattern GAPIC_YAML_TYPE =
      Pattern.compile("(?m)^type\\s*:\\s*com.google.api.codegen.ConfigProto\\s*$");

  private static Pattern SERVICE_YAML_TYPE =
      Pattern.compile("(?m)^type\\s*:\\s*google.api.Service\\s*$");

  private static String CLOUD_AUTH_SCOPE = "https://www.googleapis.com/auth/cloud-platform";

  private static final String[] PRESERVED_PROTO_LIBRARY_STRING_ATTRIBUTES = {
    // TypeScript:
    "package_name", "main_service", "bundle_config", "iam_service",
    // Other languages: add below
  };

  private static final String[] PRESERVED_PROTO_LIBRARY_LIST_ATTRIBUTES = {
    // All languages:
    "extra_protoc_parameters", "extra_protoc_file_parameters",
    // Other languages: add below
  };

  // A reference to the object representing the parent dir of this versioned API dir.
  // For example: google/example/library.
  private ApiDir parent;

  // Values of the 'package' properties parsed from all protos in this versioned directory.
  // All protos in same directory must belong to the same package.
  // For example: "google.example.library.v1".
  private String protoPackage;

  // A map of '<lang>' to '<lang>_package` properties (like 'go_package' or 'java_package' in all
  // proto files in this versioned dir).
  // For example: {
  //   "go": "google.golang.org/genproto/googleapis/example/library/v1;library",
  //   "java": "com.google.example.library.v1"
  // }
  private final Map<String, String> langProtoPackages = new HashMap<>();

  // A set of proto imports parsed from all protos in this versioned directory.
  // For example: {"google/api/annotations.proto", "google/api/client.proto"}.
  private final Set<String> imports = new TreeSet<>();

  // A set of all protos found in this versioned directory.
  // For example: {"library.proto"}
  private final Set<String> protos = new TreeSet<>();

  // A set of all 'service <ServiceName>' definitions found in all protos in this versioned dir.
  // For example: {"LibraryService"}
  private final Set<String> services = new TreeSet<>();

  // A map of '<lang>' to 'language_settings.<lang>.package_name' property values parsed from gapic
  // yaml in this versioned directory.
  // For example: {
  //   "java": "com.google.cloud.example.library.v1",
  //   "python": "google.cloud.example.library_v1.gapic",
  //   // ...
  // }
  private final Map<String, String> langGapicPackages = new HashMap<>();

  // A map with service interface names overrides ('language_settings.<lang>.interface_names'
  // values from gapic yaml).
  // For example: {
  //   "java": "google.example.library.v1.LibraryService: Library"
  // }
  private final Map<String, Map<String, String>> langGapicNameOverrides = new HashMap<>();

  // Path to service yaml (file name if it is in this directory or path starting from repo root if
  // it is in a different directory).
  private String serviceYamlPath;

  // Path to gapic yaml (file name if it is in this directory or path starting from repo root if
  // it is in a different directory).
  private String gapicYamlPath;

  // Path to service config json  (file name if it is in this directory or path starting from repo
  // root if it is in a different directory).
  private String serviceConfigJsonPath;

  // Name of this API. It is calculated from protos package name and is equal to the last
  // non-versioned component of a package name.
  // For example, if package='google.example.library.v1', then name is 'library'.
  private String name;

  // Name of assembly.
  // For example:
  //   - if it is a top-level API: "library"
  //   - if it is a sub-API (like library/admin): "library-admin"
  //
  private String assemblyName;

  // Version of this verioned API.
  // For example: "v1"
  private String version;

  // Flag indicating Cloud Auth Scope presence in the service yaml.
  // For example:
  // authentication:
  //   rules:
  //   - selector: 'google.example.library.v1.LibraryService.*'
  //     oauth:
  //       canonical_scopes: |-
  //         https://www.googleapis.com/auth/cloud-platform
  private boolean cloudScope;

  // Names of *_gapic_assembly_* rules (since they may be overridden by the user)
  private final Map<String, String> assemblyPkgRulesNames = new HashMap<>();

  // Attributes of *_gapic_library rules to be overridden
  private final Map<String, Map<String, String>> overriddenStringAttributes = new HashMap<>();
  private final Map<String, Map<String, List<String>>> overriddenListAttributes = new HashMap<>();

  void setParent(ApiDir parent) {
    this.parent = parent;
  }

  String getProtoPackage() {
    return protoPackage;
  }

  Map<String, String> getLangProtoPackages() {
    return Collections.unmodifiableMap(langProtoPackages);
  }

  Set<String> getImports() {
    return Collections.unmodifiableSet(imports);
  }

  Set<String> getProtos() {
    return Collections.unmodifiableSet(protos);
  }

  Set<String> getServices() {
    return Collections.unmodifiableSet(services);
  }

  Map<String, String> getLangGapicPackages() {
    return Collections.unmodifiableMap(langGapicPackages);
  }

  Map<String, Map<String, String>> getLangGapicNameOverrides() {
    return Collections.unmodifiableMap(langGapicNameOverrides);
  }

  String getServiceYamlPath() {
    return serviceYamlPath;
  }

  String getGapicYamlPath() {
    return gapicYamlPath;
  }

  String getServiceConfigJsonPath() {
    return serviceConfigJsonPath;
  }

  String getName() {
    return name;
  }

  String getAssemblyName() {
    return assemblyName;
  }

  String getVersion() {
    return version;
  }

  boolean getCloudScope() {
    return cloudScope;
  }

  Map<String, Map<String, String>> getOverriddenStringAttributes() {
    return overriddenStringAttributes;
  }

  Map<String, Map<String, List<String>>> getOverriddenListAttributes() {
    return overriddenListAttributes;
  }

  Map<String, String> getAssemblyPkgRulesNames() {
    return assemblyPkgRulesNames;
  }

  void parseYamlFile(String fileName, String fileBody) {
    // It is a gapic yaml
    Matcher m = GAPIC_YAML_TYPE.matcher(fileBody);
    if (m.find()) {
      gapicYamlPath = fileName;

      m = LANG_PACKAGES.matcher(fileBody);
      while (m.find()) {
        String lang = m.group("lang");
        langGapicPackages.put(lang, m.group("package"));
        String interfaceNames = m.group("interfaceNames");
        Map<String, String> interfaceSubs = new HashMap<>();
        if (interfaceNames != null) {
          Matcher subM = INTERFACE_NAMES.matcher(interfaceNames);
          while (subM.find()) {
            interfaceSubs.put(subM.group("name"), subM.group("val"));
          }
        }
        langGapicNameOverrides.put(lang, interfaceSubs);
      }

      return;
    }

    // It is a service yaml
    m = SERVICE_YAML_TYPE.matcher(fileBody);
    if (m.find()) {
      serviceYamlPath = fileName;

      if (fileBody.contains(CLOUD_AUTH_SCOPE)) {
        cloudScope = true;
      }
    }
  }

  void parseProtoFile(String fileName, String fileBody) {
    protos.add(fileName);

    // Parse proto package
    Matcher m = PROTO_PACKAGE.matcher(fileBody);
    if (m.find() && (protoPackage == null || name == null || version == null)) {
      protoPackage = m.group("protoPackage");
      String[] tokens = protoPackage.split("\\.");
      if (tokens.length >= 1 && name == null) {
        name = tokens[tokens.length - 1];
        assemblyName = name;
      }
      // Trying to figure out version of the versioned API in the most straightforward way,
      // by looking into the versioned part of the package, which should be the last component
      // of the package. Assuming that package must have at least 2 components (otherwise version
      // would be the top-level name and probably mean something different).
      if (tokens.length >= 2) {
        String ver = tokens[tokens.length - 1];
        // Count as "version" anything which is the last package component, starts with 'v'
        // character and is followed by a number
        if (ver.length() >= 2 && ver.startsWith("v") && Character.isDigit(ver.charAt(1))) {
          version = ver;
          name = tokens[tokens.length - 2];
          assemblyName = name;
          // In case if package has at least 3 components it might be a sub-api
          // (like bigtable/admin). Include name of the top package (i.e. name of the parent
          // api) to the assemblyName of this sub API.
          if (tokens.length >= 3) {
            String topPackage = tokens[tokens.length - 3];
            if (!"google".equals(topPackage) && !"cloud".equals(topPackage)) {
              assemblyName = topPackage + '-' + name;
            }
          }
        }
      }
    }

    // Parse imports
    m = IMPORTS.matcher(fileBody);
    while (m.find()) {
      imports.add(m.group("import"));
    }

    // Parse file-level options
    m = PROTO_OPTIONS.matcher(fileBody);
    while (m.find()) {
      String optName = m.group("optName");
      String optValue = m.group("optValue");
      optName = optName.split("_")[0];
      if (!langProtoPackages.containsKey(optName)) {
        langProtoPackages.put(optName, optValue);
      }
    }

    // Parse declared service names
    m = SERVICE.matcher(fileBody);
    while (m.find()) {
      services.add(m.group("service"));
    }
  }

  void parseJsonFile(String fileName, String fileBody) {
    if (fileBody.contains("methodConfig")) {
      serviceConfigJsonPath = fileName;
    }
  }

  void parseBazelBuildFile(Path file) {
    try {
      Buildozer buildozer = Buildozer.getInstance();

      // We cannot and we do not want to preserve all the content of the file.
      // We will let the user edit just the following:
      // - names of the final targets (*_gapic_assembly_*) because they are user-facing;
      // - extra protoc plugin parameters for *_gapic_library rules.
      List<String> allRules = buildozer.execute(file, "print kind name", "*");
      for (String rule : allRules) {
        String[] split = rule.split(" ");
        if (split.length != 2) {
          // some rules e.g. package() don't have "name" attribute, just skip them
          continue;
        }
        String kind = split[0];
        String name = split[1];
        if (kind.contains("_gapic_assembly_")) {
          if (this.assemblyPkgRulesNames.containsKey(kind)) {
            // Duplicated rule of the same kind will break our logic for preserving rule name.
            System.err.println("There are more than one rule of kind " + kind + ".");
            System.err.println(
                "Bazel build file generator does not support regenerating BUILD.bazel in this case.");
            System.err.println(
                "Please run it with --overwrite option to overwrite the existing BUILD.bazel completely.");
            throw new RuntimeException("Duplicated rule " + kind);
          }
          this.assemblyPkgRulesNames.put(kind, name);
        } else if (kind.endsWith("_gapic_library")) {
          this.overriddenStringAttributes.put(name, new HashMap<>());
          this.overriddenListAttributes.put(name, new HashMap<>());
          for (String attr : ApiVersionedDir.PRESERVED_PROTO_LIBRARY_STRING_ATTRIBUTES) {
            String value = buildozer.getAttribute(file, name, attr);
            if (value != null) {
              this.overriddenStringAttributes.get(name).put(attr, value);
            }
          }
          for (String attr : ApiVersionedDir.PRESERVED_PROTO_LIBRARY_LIST_ATTRIBUTES) {
            String value = buildozer.getAttribute(file, name, attr);
            if (value != null && value.startsWith("[") && value.endsWith("]")) {
              value = value.substring(1, value.length() - 1);
              String[] values = value.split(" ");
              this.overriddenListAttributes.get(name).put(attr, Arrays.asList(values));
            }
          }
        }
      }
    } catch (IOException exception) {
      System.err.println(
          "Error parsing BUILD.bazel file in " + file.toString() + ": " + exception.toString());
    }
  }

  void injectFieldsFromTopLevel() {
    if (parent == null || serviceYamlPath != null) {
      return;
    }

    String topLevelServiceYaml = parent.getServiceYamlPaths().get(version);
    if (topLevelServiceYaml == null) {
      topLevelServiceYaml = parent.getServiceYamlPaths().get("");
    }
    if (topLevelServiceYaml != null && version != null) {
      serviceYamlPath = version + '/' + topLevelServiceYaml;
    }

    Boolean topLevelCloudScope = parent.getCloudScopes().get(version);
    if (topLevelCloudScope == null) {
      topLevelCloudScope = parent.getCloudScopes().getOrDefault("", false);
    }
    cloudScope = topLevelCloudScope;
  }
}

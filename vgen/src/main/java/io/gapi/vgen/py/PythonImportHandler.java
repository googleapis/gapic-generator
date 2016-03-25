package io.gapi.vgen.py;

import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

public class PythonImportHandler {

  /**
   * Bi-map from short names to PythonImport objects for imports. Should only be modified through
   * addImport() to maintain the invariant that elements of this map are in 1:1 correspondence with
   * those in fileImports.
   */
  private final BiMap<String, PythonImport> stringImports = HashBiMap.create();

  /**
   * Bi-map from proto files to short names for imports. Should only be modified through
   * addImport() to maintain the invariant that elements of this map are in 1:1 correspondence with
   * those in stringImports.
   */
  private final BiMap<ProtoFile, String> fileImports = HashBiMap.create();

  /**
   * Map of disambiguated message types to their fully qualified paths.
   */
  private final TreeMap<String, String> classNameImports = new TreeMap<String, String>();

  /**
   * Bi-map from disambiguated class names to their proto elements.
   */
  private final BiMap<String, ProtoElement> classNameElements = HashBiMap.create();

  public static PythonImportHandler createServicePythonImportHandler(Interface service) {
    return new PythonImportHandler(service, false);
  }

  public static PythonImportHandler createMessagesPythonImportHandler(Interface service) {
    return new PythonImportHandler(service, true);
  }

  private PythonImportHandler(Interface service, boolean messages) {
    if (!messages) {
      // Add non-service-specific imports.
      addImport(null, PythonImport.create("os", PythonImport.ImportType.STDLIB));
      addImport(null, PythonImport.create("pkg_resources", PythonImport.ImportType.STDLIB));
      addImport(null, PythonImport.create("platform", PythonImport.ImportType.STDLIB));
      addImport(null, PythonImport.create("google.gax", PythonImport.ImportType.THIRD_PARTY));
      addImport(null, PythonImport.create("google.gax", "api_callable",
          PythonImport.ImportType.THIRD_PARTY));
      addImport(null, PythonImport.create("google.gax", "config",
          PythonImport.ImportType.THIRD_PARTY));
      addImport(null, PythonImport.create("google.gax", "path_template",
          PythonImport.ImportType.THIRD_PARTY));
      addImport(null, PythonImport.create("messages", PythonImport.ImportType.THIRD_PARTY));
      addImport(null, PythonImport.create("yaml", PythonImport.ImportType.THIRD_PARTY));
    } else {
      addImport(null, PythonImport.create("google.protobuf", "message",
          PythonImport.ImportType.THIRD_PARTY));
      addImport(null, PythonImport.create("google.protobuf.internal",
          "python_message", PythonImport.ImportType.THIRD_PARTY));
    }
    // Add method request-type imports.
    for (Method method : service.getMethods()) {
      addImport(method.getFile(), PythonImport.create(method.getFile().getProto().getPackage(),
          PythonProtoElements.getPbFileName(method.getInputMessage()),
          PythonImport.ImportType.APP));
      for (Field field : method.getInputType().getMessageType().getFields()) {
        deepAddImport(field, messages);
      }
    }
  }

  /**
   * Calls addImport recursively if deep is true.
   */
  private void deepAddImport(Field field, boolean deep) {
    if (field.getType().getKind() != Type.TYPE_MESSAGE) {
      return;
    }
    MessageType messageType = field.getType().getMessageType();
    PythonImport imp = addImport(messageType.getFile(),
        PythonImport.create(messageType.getFile().getProto().getPackage(),
        PythonProtoElements.getPbFileName(messageType), PythonImport.ImportType.APP));
    // Appends to a map of class names that disambiguates using the short name
    // of the import. Since the short names themselves are disambiguated, the
    // resulting class name shouldn't conflict.
    String name = messageType.getSimpleName();
    String fullPath = fullyQualifiedPath(messageType) + "." + name;
    if (classNameImports.containsKey(name) &&
        !classNameImports.get(name).equals(fullPath)) {
      String prefix = imp.shortName().replaceAll("_pb2$", "");
      prefix = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, prefix);
      name = prefix + name;
    }
    classNameImports.put(name, fullPath);
    classNameElements.put(name, messageType);
    if (deep) {
      for (Field f : messageType.getNonCyclicFields()) {
        deepAddImport(f, deep);
      }
    }
  }

  /**
   * Returns a disambiguated class name for a message given its ProtoElement
   * representation.
   */
  public String getDisambiguatedClassName(ProtoElement elt) {
    return classNameElements.inverse().get(elt);
  }

  /**
   * Returns the proto element representing the disambiguated class name.
   */
  public ProtoElement getElement(String name) {
    return classNameElements.get(name);
  }

  /**
   * Returns the full path to a message or element.
   * Ex: for path.to.type.HelloWorld, it returns path.to.type
   */
  public String fullyQualifiedPath(ProtoElement elt) {
    String path = PythonProtoElements.prefixInFile(elt);
    path = Strings.isNullOrEmpty(path) ? "" : "." + path;
    return fileToModule(elt.getFile()) + path;
  }

  /*
   * Adds an import to the import maps.
   */
  private PythonImport addImport(ProtoFile file, PythonImport imp) {
    // No conflict
    if (stringImports.get(imp.shortName()) == null) {
      fileImports.put(file, imp.shortName());
      stringImports.put(imp.shortName(), imp);
      return imp;

    // Redundant import
    } else if (stringImports.get(imp.shortName()).importString().equals(imp.importString())) {
      return imp;

    // Conflict
    } else {
      String oldShortName = imp.shortName();
      PythonImport formerImp = stringImports.remove(oldShortName);
      ProtoFile formerFile = fileImports.inverse().remove(oldShortName);

      PythonImport disambiguatedNewImp = imp.disambiguate();
      PythonImport disambiguatedOldImp = formerImp.disambiguate();

      // If we mangled both names, un-mangle the older one; otherwise we'll be in an infinite
      // mangling cycle.
      if (disambiguatedNewImp.shortName().equals(oldShortName + "_") &&
          disambiguatedOldImp.shortName().equals(oldShortName + "_")) {
        disambiguatedOldImp = formerImp;
      }

      addImport(formerFile, disambiguatedOldImp);
      return addImport(file, disambiguatedNewImp);
    }
  }

  /**
   * Returns the class name -> fully qualified path map.
   */
  public TreeMap<String, String> getClassNameImports() {
    return classNameImports;
  }

  /**
   * Calculate the imports map and return a sorted set of python import output strings.
   */
  public List<String> calculateImports() {
    // Order by import type, then lexicographically
    List<String> stdlibResult = new ArrayList<>();
    List<String> thirdPartyResult = new ArrayList<>();
    List<String> appResult = new ArrayList<>();
    for (PythonImport protoImport : stringImports.values()) {
      switch(protoImport.type()) {
        case STDLIB:
          stdlibResult.add(protoImport.importString());
          break;
        case THIRD_PARTY:
          thirdPartyResult.add(protoImport.importString());
          break;
        case APP:
          appResult.add(protoImport.importString());
          break;
      }
    }
    Collections.sort(stdlibResult);
    Collections.sort(thirdPartyResult);
    Collections.sort(appResult);

    List<String> all = new ArrayList<>();
    if (stdlibResult.size() > 0) {
      all.addAll(stdlibResult);
      all.add("");
    }
    if (thirdPartyResult.size() > 0) {
      all.addAll(thirdPartyResult);
      all.add("");
    }
    all.addAll(appResult);
    return all;
  }

  public String fileToModule(ProtoFile file) {
    return fileImports.get(file);
  }
}

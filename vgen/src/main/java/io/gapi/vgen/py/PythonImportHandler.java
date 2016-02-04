package io.gapi.vgen.py;

import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

  public PythonImportHandler(Interface service) {
    // Add non-service-specific imports.
    addImport(null,
        PythonImport.create("google.gax", "api_callable", PythonImport.ImportType.THIRD_PARTY));
    addImport(null,
        PythonImport.create("google.gax", "config", PythonImport.ImportType.THIRD_PARTY));
    addImport(null,
        PythonImport.create("google.gax", "PageDescriptor", PythonImport.ImportType.THIRD_PARTY));
    addImport(null,
        PythonImport.create("google.gax.path_template", "PathTemplate",
            PythonImport.ImportType.THIRD_PARTY));

    // Add service-specific imports.
    addImport(service.getFile(),
        PythonImport.create(service.getFile().getProto().getPackage(),
            PythonProtoElements.getPbFileName(service), PythonImport.ImportType.THIRD_PARTY));

    // Add method request-type imports
    for (Method method : service.getMethods()) {
      addImport(method.getFile(), PythonImport.create(method.getFile().getProto().getPackage(),
          PythonProtoElements.getPbFileName(method.getInputMessage()), PythonImport.ImportType.APP));
      for (Field field : method.getInputType().getMessageType().getFields()) {
        if (field.getType().getKind() == Type.TYPE_MESSAGE) {
          MessageType messageType = field.getType().getMessageType();
          addImport(messageType.getFile(),
              PythonImport.create(messageType.getFile().getProto().getPackage(),
              PythonProtoElements.getPbFileName(messageType), PythonImport.ImportType.APP));
        }
      }
    }
  }
  /*
   * Adds an import to the import maps
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
      // Only mangle the earlier import if these are not local name (i.e. "import x as y") imports
      if (Strings.isNullOrEmpty(imp.localName())) {
        String oldShortName = imp.shortName();
        PythonImport former_imp = stringImports.remove(oldShortName);
        ProtoFile former_file = fileImports.inverse().remove(oldShortName);
        addImport(former_file, former_imp.disambiguate());
      }
      PythonImport latter_imp = imp.disambiguate();
      return addImport(file, latter_imp);
    }
  }

  /*
   * Calculate the imports map and return a sorted set of python import output strings.
   */
  public List<String> calculateImports() {

    // Order by import type, then lexicographically
    List<String> stdlib_result = new ArrayList<>();
    List<String> third_party_result = new ArrayList<>();
    List<String> app_result = new ArrayList<>();
    for (PythonImport protoImport : stringImports.values()) {
      switch(protoImport.type()) {
        case STDLIB:
          stdlib_result.add(protoImport.importString());
          break;
        case THIRD_PARTY:
          third_party_result.add(protoImport.importString());
          break;
        case APP:
          app_result.add(protoImport.importString());
          break;
      }
    }
    Collections.sort(stdlib_result);
    Collections.sort(third_party_result);
    Collections.sort(app_result);

    List<String> all = new ArrayList<>();
    all.addAll(stdlib_result);
    all.addAll(third_party_result);
    all.addAll(app_result);
    return all;
  }

  public String fileToModule(ProtoFile file) {
    return fileImports.get(file);
  }

}


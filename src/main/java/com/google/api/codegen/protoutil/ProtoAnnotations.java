package com.google.api.codegen.protoutil;

import com.google.api.AnnotationsProto;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.ExtensionRegistry;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class ProtoAnnotations {
  // Full name of the protofile containing proto annotations definitions.
  public static final String PROTO_FULL_NAME = "google/api/annotations.proto";
  public static ExtensionRegistry registry = ExtensionRegistry.newInstance();


  public ProtoAnnotations(Model model) {
    ProtoFile annotationProto = model.getFiles()
        .stream()
        .filter(f -> f.getProto().getName().equals(PROTO_FULL_NAME))
        .findFirst().get();
    AnnotationsProto.registerAllExtensions(registry);
    registry.toString();
//    for (DescriptorProtos.FieldDescriptorProto extension : annotationProto.getProto().getExtensionList()) {
//      registry.add(extension);
//    }
  }

//  public static void parseExtension() {
//    registry.add(MyProto.bar);
//    MyProto.Foo message = MyProto.Foo.parseFrom(input, registry);
//  }
}

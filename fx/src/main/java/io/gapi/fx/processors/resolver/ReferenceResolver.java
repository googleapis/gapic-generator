package io.gapi.fx.processors.resolver;

import io.gapi.fx.model.Diag;
import io.gapi.fx.model.EnumType;
import io.gapi.fx.model.EnumValue;
import io.gapi.fx.model.Field;
import io.gapi.fx.model.Location;
import io.gapi.fx.model.Method;
import io.gapi.fx.model.Model;
import io.gapi.fx.model.ProtoContainerElement;
import io.gapi.fx.model.SymbolTable;
import io.gapi.fx.model.TypeRef;
import io.gapi.fx.model.Visitor;
import io.gapi.fx.util.Visits;
import io.gapi.fx.util.VisitsBefore;

import com.google.common.collect.Queues;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.util.Deque;

/**
 * Visitor which resolves type references.
 */
class ReferenceResolver extends Visitor {

  private final Model model;
  private final SymbolTable symbolTable;

  // Represents a stack of namespaces, with the top element the most active one.
  // Namespace here either means the package (on file level) or the package
  // plus an enclosing number of messages. This is used for resolution of partial names.
  private final Deque<String> namespaces = Queues.newArrayDeque();

  ReferenceResolver(Model model, SymbolTable symbolTable) {
    this.model = model;
    this.symbolTable = symbolTable;
  }

  void run() {
    visit(model);
  }

  @Visits
  void visit(ProtoContainerElement container) {
    namespaces.push(container.getFullName());
    accept(container);
    namespaces.pop();
  }

  @VisitsBefore
  void visit(Field field) {
    // Resolve type of this field.
    TypeRef type = resolveType(field.getLocation(), field.getProto().getType(),
        field.getProto().getTypeName());
    if (type != null) {
      if (field.isRepeated()) {
        type = type.makeRepeated();
      } else if (!field.isOptional()) {
        type = type.makeRequired();
      }
      field.setType(type);
    }

    // Check for resolution of oneof.
    if (field.getProto().hasOneofIndex() && field.getOneof() == null) {
      // Indicates the oneof index could not be resolved.
      model.addDiag(Diag.error(field.getLocation(),
          "Unresolved oneof reference (indicates internal inconsistency of input; oneof index: %s)",
          field.getProto().getOneofIndex()));
    }
  }

  @VisitsBefore
  void visit(Method method) {
    // Resolve input and output type of this method.
    TypeRef inputType = resolveType(method.getLocation(),
        Type.TYPE_MESSAGE, method.getDescriptor().getInputTypeName());
    if (inputType != null) {
      method.setInputType(inputType);
    }
    TypeRef outputType = resolveType(method.getLocation(),
        Type.TYPE_MESSAGE, method.getDescriptor().getOutputTypeName());
    if (outputType != null) {
      method.setOutputType(outputType);
    }
  }

  @VisitsBefore
  void visit(EnumValue value) {
    // The type is build from the parent, which must be an enum type.
    value.setType(TypeRef.of((EnumType) value.getParent()));
  }

  /**
   * Resolves a type based on the given partial name. This does not assume that the name, as
   * obtained from the descriptor, is in absolute form.
   */
  private TypeRef resolveType(Location location, Type kind, String name) {
    TypeRef type;
    switch (kind) {
      case TYPE_MESSAGE:
      case TYPE_ENUM:
      case TYPE_GROUP:
        type = symbolTable.resolveType(namespaces.peek(), name);
        break;
      default:
        type = TypeRef.of(kind);
    }
    if (type == null) {
      model.addDiag(Diag.error(location, "Unresolved type '%s'", name));
    }
    return type;
  }
}

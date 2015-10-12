package io.gapi.fx.processors.resolver;

import io.gapi.fx.model.Diag;
import io.gapi.fx.model.EnumType;
import io.gapi.fx.model.EnumValue;
import io.gapi.fx.model.Field;
import io.gapi.fx.model.Interface;
import io.gapi.fx.model.Location;
import io.gapi.fx.model.MessageType;
import io.gapi.fx.model.Method;
import io.gapi.fx.model.Model;
import io.gapi.fx.model.SymbolTable;
import io.gapi.fx.model.TypeRef;
import io.gapi.fx.model.Visitor;
import io.gapi.fx.util.VisitsBefore;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Visitor which creates the symbol table for an model. Reports errors for duplicate
 * declarations.
 */
class SymbolTableBuilder extends Visitor {

  private final Model model;
  private final Map<String, Interface> interfaces = Maps.newLinkedHashMap();
  private final Map<String, TypeRef> types = Maps.newLinkedHashMap();

  SymbolTableBuilder(Model model) {
    this.model = model;
  }

  SymbolTable run() {
    visit(model);
    return new SymbolTable(interfaces, types);
  }

  @VisitsBefore void visit(Interface endpointInterface) {
    // Add the interface to the map of known interfaces.
    Interface old = interfaces.put(endpointInterface.getFullName(), endpointInterface);
    if (old != null) {
      model.addDiag(Diag.error(endpointInterface.getLocation(),
          "Duplicate declaration of interface '%s'. Previous location: %s",
          endpointInterface.getFullName(), old.getLocation().getDisplayString()));
    }

    // Build the method-by-name map for this interface.
    Map<String, Method> methodByName = Maps.newLinkedHashMap();
    for (Method method : endpointInterface.getMethods()) {
      Method oldMethod = methodByName.put(method.getSimpleName(), method);
      if (oldMethod != null) {
        model.addDiag(Diag.error(method.getLocation(),
            "Duplicate declaration of method '%s'. Previous location: %s",
            method.getSimpleName(), oldMethod.getLocation().getDisplayString()));
      }
    }
    endpointInterface.setMethodByNameMap(ImmutableMap.copyOf(methodByName));
  }

  @VisitsBefore void visit(MessageType message) {
    // Add the message to the set of known types.
    addType(message.getLocation(), message.getFullName(), TypeRef.of(message));

    // Build the field-by-name map for this message.
    Map<String, Field> fieldByName = Maps.newLinkedHashMap();
    for (Field field : message.getFields()) {
      Field old = fieldByName.put(field.getSimpleName(), field);
      if (old != null) {
        model.addDiag(Diag.error(field.getLocation(),
            "Duplicate declaration of field '%s'. Previous location: %s",
            field.getSimpleName(), old.getLocation().getDisplayString()));
      }
    }
    message.setFieldByNameMap(ImmutableMap.copyOf(fieldByName));
  }

  @VisitsBefore void visit(EnumType enumType) {
    // Add the enum type to the set of known types.
    addType(enumType.getLocation(), enumType.getFullName(),
        TypeRef.of(enumType));

    // Build the field-by-name map for this enum type.
    Map<String, EnumValue> valueByName = Maps.newLinkedHashMap();
    for (EnumValue value : enumType.getValues()) {
      EnumValue old = valueByName.put(value.getSimpleName(), value);
      if (old != null) {
        model.addDiag(Diag.error(value.getLocation(),
            "Duplicate declaration of enum value '%s'. Previous location: %s",
            value.getSimpleName(), old.getLocation().getDisplayString()));
      }
    }
    enumType.setValueByNameMap(ImmutableMap.copyOf(valueByName));
  }

  private void addType(Location location, String fullName, TypeRef type) {
    String typeName = SymbolTable.getTypeNameInSymbolTable(fullName);
    TypeRef old = types.put(typeName , type);
    if (old != null) {
      model.addDiag(Diag.error(location,
          "Duplicate declaration of type '%s'. Previous location: %s",
          fullName, old.getLocation().getDisplayString()));
    }
  }
}

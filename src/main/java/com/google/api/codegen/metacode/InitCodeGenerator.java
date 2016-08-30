/* Copyright 2016 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.metacode;

import com.google.api.codegen.util.Name;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.TypeRef;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * InitCodeGenerator generates an InitCode object for a given object and initialization field
 * structure (as constructed by FieldStructureParser).
 */
public class InitCodeGenerator {
  private final List<InitCodeLine> initLineSpecs = new ArrayList<>();

  /**
   * Generates the InitCode object based on the given context.
   *
   * If the generated fields are flattened, the last line (init line of the final object) will not
   * be added. Otherwise the final generated object will be the object specified in the context.
   */
  public InitCode generate(InitCodeGeneratorContext context) {
    Name suggestedName = context.initObjectName();
    TypeRef type = context.initObjectType();
    Map<String, Object> initStructure = context.initStructure();

    if (context.isFlattened()) {
      initStructure = getFlattenedInitStructure(context.flattenedFields(), initStructure);
    }
    InitCodeLine lastLine = generateCodeInit(suggestedName, type, initStructure, context);

    List<FieldSetting> outputFields;
    if (context.isFlattened()) {
      outputFields = ((StructureInitCodeLine) lastLine).getFieldSettings();
    } else {
      FieldSetting objectField =
          FieldSetting.create(
              type, suggestedName, lastLine.getIdentifier(), lastLine.getInitValueConfig());
      outputFields = Arrays.asList(objectField);
      initLineSpecs.add(lastLine);
    }

    return InitCode.create(initLineSpecs, outputFields);
  }

  private Map<String, Object> getFlattenedInitStructure(
      List<Field> flattenedFields, Map<String, Object> initStructure) {
    Map<String, Object> filteredInit = new HashMap<>();
    for (Field field : flattenedFields) {
      Object subStructure = initStructure.get(field.getSimpleName());
      if (subStructure != null) {
        filteredInit.put(field.getSimpleName(), subStructure);
      } else {
        filteredInit.put(field.getSimpleName(), InitValueConfig.create());
      }
    }
    return filteredInit;
  }

  private InitCodeLine generateCodeInitStructure(
      Name suggestedName,
      TypeRef typeRef,
      Map<String, Object> initFieldMap,
      InitCodeGeneratorContext context) {

    List<FieldSetting> fieldSettings = new ArrayList<>();
    for (Field field : typeRef.getMessageType().getFields()) {
      Object thisFieldInitStructure = initFieldMap.get(field.getSimpleName());
      if (thisFieldInitStructure == null) {
        continue;
      }

      InitCodeLine subFieldInit =
          generateCodeInit(
              Name.from(field.getSimpleName()), field.getType(), thisFieldInitStructure, context);
      initLineSpecs.add(subFieldInit);

      FieldSetting fieldSetting =
          FieldSetting.create(
              field.getType(),
              Name.from(field.getSimpleName()),
              subFieldInit.getIdentifier(),
              subFieldInit.getInitValueConfig());
      fieldSettings.add(fieldSetting);
    }

    // TODO check each field in initFieldMap and make sure it exists in
    // typeRef.getMessageType().getFields()

    // get a new symbol for this object after subfields, in order to preserve
    // numerical ordering in the case of conflicts
    Name identifier = context.symbolTable().getNewSymbol(suggestedName);

    return StructureInitCodeLine.create(typeRef, identifier, fieldSettings);
  }

  private InitCodeLine generateCodeInitList(
      Name suggestedName,
      TypeRef typeRef,
      List<Object> thisFieldInitList,
      InitCodeGeneratorContext context) {
    List<Name> elementIdentifiers = new ArrayList<>();
    for (Object elementInitStructure : thisFieldInitList) {
      Name suggestedElementName = suggestedName.join("element");
      // Using the Optional cardinality replaces the Repeated cardinality
      TypeRef elementType = typeRef.makeOptional();
      InitCodeLine subFieldInit =
          generateCodeInit(suggestedElementName, elementType, elementInitStructure, context);
      initLineSpecs.add(subFieldInit);

      elementIdentifiers.add(subFieldInit.getIdentifier());
    }

    // get a new symbol for this object after elements, in order to preserve
    // numerical ordering in the case of conflicts
    Name identifier = context.symbolTable().getNewSymbol(suggestedName);

    return ListInitCodeLine.create(typeRef, identifier, elementIdentifiers);
  }

  private InitCodeLine generateCodeInitMap(
      Name suggestedName,
      TypeRef typeRef,
      Map<String, Object> thisFieldInitMap,
      InitCodeGeneratorContext context) {
    TypeRef keyTypeRef = typeRef.getMapKeyField().getType();
    TypeRef elementType = typeRef.getMapValueField().getType();
    Map<String, Name> elementIdentifierMap = new HashMap<>();
    for (String keyString : thisFieldInitMap.keySet()) {
      String validatedKeyString = validateValue(keyTypeRef, keyString);
      if (validatedKeyString == null) {
        throw new IllegalArgumentException(
            "Inconsistent key type found for map, expected key of type "
                + keyTypeRef
                + ", got key "
                + keyString
                + "; suggestedName = "
                + suggestedName
                + ", initFieldStructure = "
                + thisFieldInitMap);
      }

      Object elementInitStructure = thisFieldInitMap.get(keyString);
      Name suggestedElementName = suggestedName.join("item");
      InitCodeLine subFieldInit =
          generateCodeInit(suggestedElementName, elementType, elementInitStructure, context);
      initLineSpecs.add(subFieldInit);

      elementIdentifierMap.put(validatedKeyString, subFieldInit.getIdentifier());
    }

    // get a new symbol for this object after elements, in order to preserve
    // numerical ordering in the case of conflicts
    Name identifier = context.symbolTable().getNewSymbol(suggestedName);
    return MapInitCodeLine.create(
        keyTypeRef, elementType, typeRef, identifier, elementIdentifierMap);
  }

  private InitCodeLine generateCodeInit(
      Name suggestedName,
      TypeRef typeRef,
      Object initFieldStructure,
      InitCodeGeneratorContext context) {
    // No matter what the type in the model is, we want to stop here, because we
    // have reached the end of initFieldStructure. At codegen time, we will
    // generate the zero value for the type.
    if (initFieldStructure instanceof InitValueConfig) {
      InitValueConfig initValueConfig = (InitValueConfig) initFieldStructure;
      Name identifier = context.symbolTable().getNewSymbol(suggestedName);
      if (typeRef.isPrimitive() && !typeRef.isRepeated() && context.shouldGenerateTestValue()) {
        initValueConfig =
            initValueConfig.withInitialValue(
                context.valueGenerator().getAndStoreValue(typeRef, identifier));
      } else if (initValueConfig.hasInitialValue()) {
        String validatedValue = validateValue(typeRef, initValueConfig.getInitialValue());
        if (validatedValue == null) {
          throw new IllegalArgumentException(
              "Inconsistent initial value found, expected value of type "
                  + typeRef
                  + ", got value "
                  + initValueConfig.getInitialValue()
                  + "; suggestedName = "
                  + suggestedName
                  + ", initFieldStructure = "
                  + initFieldStructure);
        }
        initValueConfig = initValueConfig.withInitialValue(validatedValue);
      }
      return SimpleInitCodeLine.create(typeRef, identifier, initValueConfig);
    }

    if (typeRef.isMessage() && !typeRef.isRepeated()) {
      if (!(initFieldStructure instanceof Map)) {
        throw new IllegalArgumentException(
            "Message typeRef needs a Map, found "
                + initFieldStructure.getClass().getName()
                + "; typeRef = "
                + typeRef
                + ", suggestedName = "
                + suggestedName
                + ", initFieldStructure = "
                + initFieldStructure);
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> initFieldMap = (Map<String, Object>) initFieldStructure;
      return generateCodeInitStructure(suggestedName, typeRef, initFieldMap, context);

    } else if (typeRef.isRepeated() && !typeRef.isMap()) {
      if (!(initFieldStructure instanceof List)) {
        throw new IllegalArgumentException(
            "Repeated typeRef needs a List, found "
                + initFieldStructure.getClass().getName()
                + "; typeRef = "
                + typeRef
                + ", suggestedName = "
                + suggestedName
                + ", initFieldStructure = "
                + initFieldStructure);
      }
      @SuppressWarnings("unchecked")
      List<Object> thisFieldInitList = (List<Object>) initFieldStructure;
      return generateCodeInitList(suggestedName, typeRef, thisFieldInitList, context);

    } else if (typeRef.isMap()) {
      if (!(initFieldStructure instanceof Map)) {
        throw new IllegalArgumentException(
            "Map typeRef needs a Map, found "
                + initFieldStructure.getClass().getName()
                + "; typeRef = "
                + typeRef
                + ", suggestedName = "
                + suggestedName
                + ", initFieldStructure = "
                + initFieldStructure);
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> thisFieldInitMap = (Map<String, Object>) initFieldStructure;

      return generateCodeInitMap(suggestedName, typeRef, thisFieldInitMap, context);

    } else {
      throw new IllegalArgumentException("Unexpected type: " + typeRef);
    }
  }

  /**
   * Validates that the provided value matches the provided type, and returns the validated value.
   * For string and byte types, the returned value has quote characters removed. Returns null if the
   * value does not match. Throws an IllegalArgumentException is the provided type is not supported.
   */
  private static String validateValue(TypeRef type, String value) {
    Type descType = type.getKind();
    switch (descType) {
      case TYPE_BOOL:
        String lowerCaseValue = value.toLowerCase();
        if (lowerCaseValue.equals("true") || lowerCaseValue.equals("false")) {
          return lowerCaseValue;
        }
        break;
      case TYPE_DOUBLE:
      case TYPE_FLOAT:
        if (Pattern.matches("[+-]?([0-9]*[.])?[0-9]+", value)) {
          return value;
        }
        break;
      case TYPE_INT64:
      case TYPE_UINT64:
      case TYPE_SINT64:
      case TYPE_FIXED64:
      case TYPE_SFIXED64:
      case TYPE_INT32:
      case TYPE_UINT32:
      case TYPE_SINT32:
      case TYPE_FIXED32:
      case TYPE_SFIXED32:
        if (Pattern.matches("[+-]?[0-9]+", value)) {
          return value;
        }
        break;
      case TYPE_STRING:
      case TYPE_BYTES:
        Matcher matcher = Pattern.compile("\"([^\\\"]+)\"").matcher(value);
        if (matcher.matches()) {
          return matcher.group(1);
        }
        break;
      default:
        // Throw an exception if a value is unsupported for the given type.
        throw new IllegalArgumentException(
            "Tried to assign value for unsupported type " + type + "; value " + value);
    }
    return null;
  }
}

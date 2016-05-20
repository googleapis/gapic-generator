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

import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;

/**
 * InitCodeGenerator generates an InitCode object for a given method and
 * initialization field structure (as constructed by FieldStructureParser).
 */
public class InitCodeGenerator {
  private Set<String> symbolTable = new HashSet<>();
  private List<InitCodeLine> initLineSpecs = new ArrayList<>();

  /**
   * Generates the InitCode for a method, where the input of the function
   * representing the method will take a request object.
   */
  public InitCode generateRequestObjectInitCode(Method method,
      Map<String, Object> initFieldStructure) {
    InitCodeLine lastLine = generateSampleCodeInit("request", method.getInputType(),
        initFieldStructure);
    initLineSpecs.add(lastLine);
    FieldSetting requestField = FieldSetting.create(method.getInputType(), "request",
        lastLine.getIdentifier(), lastLine.getInitValueConfig());
    List<FieldSetting> outputFields = Arrays.asList(requestField);
    return InitCode.create(initLineSpecs, outputFields);
  }

  /**
   * Generates the InitCode for a method, where the input of the function
   * representing the method will take the given set of fields.
   */
  public InitCode generateRequestFieldInitCode(Method method,
      Map<String, Object> initFieldStructure, Iterable<Field> fields) {

    Map<String, Object> filteredInit = new HashMap<>();
    for (Field field : fields) {
      Object subStructure = initFieldStructure.get(field.getSimpleName());
      if (subStructure != null) {
        filteredInit.put(field.getSimpleName(), subStructure);
      } else {
        filteredInit.put(field.getSimpleName(), InitValueConfig.create());
      }
    }

    InitCodeLine lastLine = generateSampleCodeInit("request", method.getInputType(), filteredInit);
    if (!(lastLine instanceof StructureInitCodeLine)) {
      throw new IllegalArgumentException(
          "Expected method request to be a message, found " + lastLine.getClass().getName());
    }
    StructureInitCodeLine requestInitCodeLine = (StructureInitCodeLine) lastLine;
    return InitCode.create(initLineSpecs, requestInitCodeLine.getFieldSettings());
  }

  private String getNewSymbol(String desiredName) {
    String actualName = desiredName;
    int i = 2;
    while (symbolTable.contains(actualName)) {
      actualName = desiredName + i;
    }
    symbolTable.add(actualName);
    return actualName;
  }

  private InitCodeLine generateSampleCodeInit(String suggestedName, TypeRef typeRef,
      Object initFieldStructure) {
    if (initFieldStructure instanceof InitValueConfig) {
      InitValueConfig initValueConfig = (InitValueConfig) initFieldStructure;
      String identifier = getNewSymbol(suggestedName);
      return SimpleInitCodeLine.create(typeRef, identifier, initValueConfig);
    }

    if (typeRef.isMessage() && !typeRef.isRepeated()) {
      if (!(initFieldStructure instanceof Map)) {
        throw new IllegalArgumentException(
            "Message typeRef needs a Map, found " + initFieldStructure.getClass().getName()
                + "; typeRef = " + typeRef + ", suggestedName = " + suggestedName
                + ", initFieldStructure = " + initFieldStructure);
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> initFieldMap = (Map<String, Object>) initFieldStructure;

      List<FieldSetting> fieldSettings = new ArrayList<>();
      for (Field field : typeRef.getMessageType().getFields()) {
        Object thisFieldInitStructure = initFieldMap.get(field.getSimpleName());
        if (thisFieldInitStructure == null) {
          continue;
        }

        InitCodeLine subFieldInit = generateSampleCodeInit(field.getSimpleName(), field.getType(),
            thisFieldInitStructure);
        initLineSpecs.add(subFieldInit);

        FieldSetting fieldSetting = FieldSetting.create(field.getType(), field.getSimpleName(),
            subFieldInit.getIdentifier(), subFieldInit.getInitValueConfig());
        fieldSettings.add(fieldSetting);
      }

      // TODO check each field in initFieldMap and make sure it exists in
      // typeRef.getMessageType().getFields()

      // get a new symbol for this object after subfields, in order to preserve
      // numerical ordering in the case of conflicts
      String identifier = getNewSymbol(suggestedName);
      return StructureInitCodeLine.create(typeRef, identifier, fieldSettings);

    } else if (typeRef.isRepeated()) {
      if (!(initFieldStructure instanceof List)) {
        throw new IllegalArgumentException(
            "Repeated typeRef needs a List, found " + initFieldStructure.getClass().getName()
                + "; typeRef = " + typeRef + ", suggestedName = " + suggestedName
                + ", initFieldStructure = " + initFieldStructure);
      }
      @SuppressWarnings("unchecked")
      List<Object> thisFieldInitList = (List<Object>) initFieldStructure;

      List<String> elementIdentifiers = new ArrayList<>();
      for (Object elementInitStructure : thisFieldInitList) {
        String suggestedElementName = suggestedName + "_element";
        // Using the Optional cardinality replaces the Repeated cardinality
        TypeRef elementType = typeRef.makeOptional();
        InitCodeLine subFieldInit = generateSampleCodeInit(suggestedElementName, elementType,
            elementInitStructure);
        initLineSpecs.add(subFieldInit);

        elementIdentifiers.add(subFieldInit.getIdentifier());
      }

      // get a new symbol for this object after elements, in order to preserve
      // numerical ordering in the case of conflicts
      String identifier = getNewSymbol(suggestedName);
      return ListInitCodeLine.create(typeRef, identifier, elementIdentifiers);

    } else if (typeRef.isMap()) {
      throw new NotImplementedException("map not supported yet");

    } else {
      throw new IllegalArgumentException("Unexpected type: " + typeRef);
    }
  }
}

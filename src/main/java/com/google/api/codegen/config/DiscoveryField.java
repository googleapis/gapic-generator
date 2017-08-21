/* Copyright 2017 Google Inc
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
package com.google.api.codegen.config;

import static com.google.api.codegen.config.ApiSource.DISCOVERY;

import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.discovery.Schema.Format;
import com.google.api.codegen.discovery.Schema.Type;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.SchemaTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.api.tools.framework.model.TypeRef;
import com.google.api.tools.framework.model.TypeRef.Cardinality;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** Created by andrealin on 7/31/17. */
public class DiscoveryField implements FieldModel {
  private final Schema schema;
  private final DiscoGapicNamer discoGapicNamer;

  /* Create a FieldModel object from a non-null Schema object. */
  public DiscoveryField(Schema schema, DiscoGapicNamer discoGapicNamer) {
    Preconditions.checkNotNull(schema);
    this.schema = schema;
    this.discoGapicNamer = discoGapicNamer;
  }

  @Override
  /* @return the type of the underlying model resource. */
  public ApiSource getApiSource() {
    return DISCOVERY;
  }

  @Override
  public Schema getDiscoveryField() {
    return schema;
  }

  @Override
  public String getSimpleName() {
    return schema.getIdentifier();
  }

  @Override
  public String getFullName() {
    SurfaceNamer surfaceNamer = discoGapicNamer.getLanguageNamer();
    TypeNameConverter typeNameConverter = surfaceNamer.getTypeNameConverter();
    return typeNameConverter
        .getTypeNameInImplicitPackage(
            surfaceNamer.publicClassName(Name.anyCamel(schema.getIdentifier())))
        .getFullName();
  }

  @Override
  public TypeName getTypeName(ImportTypeTable typeTable) {
    return typeTable
        .getTypeTable()
        .getTypeName(((SchemaTypeTable) typeTable).getFullNameFor(schema));
  }

  @Override
  public Name asName() {
    return Name.anyCamel(getSimpleName());
  }

  @Override
  public String getTypeFullName() {
    return schema.getIdentifier();
  }

  @Override
  public boolean isMap() {
    return false;
  }

  @Override
  public FieldModel getMapKeyField() {
    throw new IllegalArgumentException("Discovery model types have no map keys.");
  }

  @Override
  public FieldModel getMapValueField() {
    throw new IllegalArgumentException("Discovery model types have no map values.");
  }

  @Override
  public boolean isMessage() {
    return false;
  }

  @Override
  public boolean isRequired() {
    return schema.required();
  }

  @Override
  public boolean isRepeated() {
    return schema.type() == Type.ARRAY;
  }

  @Override
  public String getParentFullName() {
    SurfaceNamer surfaceNamer = discoGapicNamer.getLanguageNamer();
    TypeNameConverter typeNameConverter = surfaceNamer.getTypeNameConverter();
    if (schema.parent() instanceof Method) {
      return typeNameConverter
          .getTypeNameInImplicitPackage(
              surfaceNamer.publicClassName(
                  DiscoGapicNamer.getRequestName((Method) schema.parent())))
          .getFullName();
    } else if (schema.parent() instanceof Schema) {
      return typeNameConverter
          .getTypeNameInImplicitPackage(
              surfaceNamer.publicClassName(
                  Name.anyCamel(((Schema) schema.parent()).getIdentifier())))
          .getFullName();
    } else if (schema.parent() instanceof Document) {
      return ((Document) schema.parent()).name();
    }
    return "";
  }

  @Override
  public String getParentSimpleName() {
    return schema.parent().id();
  }

  @Override
  public TypeName getParentTypeName(ImportTypeTable typeTable) {
    if (schema.parent() instanceof Schema) {
      DiscoveryField parent = new DiscoveryField((Schema) schema.parent(), discoGapicNamer);
      return typeTable.getTypeTable().getTypeName(typeTable.getFullNameFor(parent));
    }
    return typeTable.getTypeTable().getTypeName(typeTable.getFullNameFor(this));
  }

  @Override
  public Cardinality getCardinality() {
    throw new IllegalArgumentException("Discovery model types have no defined Cardinality.");
  }

  @Override
  public boolean isEnum() {
    return schema.isEnum();
  }

  @Override
  public boolean isPrimitive() {
    return schema.reference().isEmpty() && schema.items() == null;
  }

  @Override
  public String toString() {
    return String.format("Discovery FieldModel (%s): {%s}", getApiSource(), schema.toString());
  }

  @Override
  public TypeRef getProtoTypeRef() {
    throw new IllegalArgumentException("Discovery model types have no TypeRefs.");
  }

  @Override
  public boolean equals(Object o) {
    return o != null
        && o instanceof DiscoveryField
        && ((DiscoveryField) o).schema.equals(this.schema);
  }

  @Override
  /* @Get the description of the element scoped to the visibility as currently set in the model. */
  public String getScopedDocumentation() {
    return schema.description();
  }

  @Override
  public List<String> getOneofFieldsNames(SurfaceNamer surfaceNamer) {
    return ImmutableList.of();
  }

  @Override
  public boolean isString() {
    return schema.type().equals(Type.STRING);
  }

  @Override
  public boolean isBytes() {
    return schema.type().equals(Type.ANY)
        || (schema.type().equals(Type.STRING) && schema.format().equals(Format.BYTE));
  }

  @Override
  public String getKind() {
    return schema.type().toString();
  }

  public DiscoGapicNamer getDiscoGapicNamer() {
    return discoGapicNamer;
  }
}

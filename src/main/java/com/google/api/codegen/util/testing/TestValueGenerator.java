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
package com.google.api.codegen.util.testing;

import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.util.Name;
import java.util.HashMap;

/**
 * A utility class used by the test generators which populates values for primitive fields.
 *
 * <p>The populated values will be unique (except for boolean values) and deterministic based on the
 * populated fields and the sequence.
 */
public class TestValueGenerator {
  private final HashMap<Name, String> valueTable = new HashMap<>();
  private final ValueProducer producer;

  public TestValueGenerator(ValueProducer producer) {
    this.producer = producer;
  }

  public String getAndStoreValue(TypeModel type, Name identifier) {
    if (!valueTable.containsKey(identifier)) {
      String value = producer.produce(type, identifier);
      while (type.getPrimitiveTypeName() != "bool" && valueTable.containsValue(value)) {
        // If the value already exists regenerate using a deterministically different identifier.
        identifier = identifier.join("1");
        value = producer.produce(type, identifier);
      }
      valueTable.put(identifier, value);
    }
    return valueTable.get(identifier);
  }
}

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
package com.google.api.codegen;

import com.google.api.Service;
import com.google.common.truth.Truth;
import com.google.protobuf.Api;
import com.google.protobuf.Field;
import com.google.protobuf.Method;
import com.google.protobuf.Type;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import org.junit.Test;

public class DiscoveryImporterTest {
  private static Reader getReader(String file) throws IOException {
    file = "src/test/java/com/google/api/codegen/discoverytestdata/" + file;
    return new InputStreamReader(new FileInputStream(new File(file)));
  }

  private static DiscoveryImporter importer(String file) throws IOException {
    return DiscoveryImporter.parse(getReader(file));
  }

  @Test
  public void testBasicInfo() throws IOException {
    Service service = importer("basicinfo.json").getService();
    Truth.assertThat(service.getName()).isEqualTo("https://pubsub.googleapis.com/");
    Truth.assertThat(service.getTitle()).isEqualTo("Google Cloud Pub/Sub API");
    Truth.assertThat(service.getDocumentation().getDocumentationRootUrl())
        .isEqualTo("https://cloud.google.com/pubsub/docs");
    Truth.assertThat(service.getApisCount()).isEqualTo(1);
    Truth.assertThat(service.getApis(0).getName()).isEqualTo("pubsub");
    Truth.assertThat(service.getApis(0).getVersion()).isEqualTo("v1");
  }

  @Test
  public void testMethod() throws IOException {
    DiscoveryImporter imp = importer("method.json");
    Api api = imp.getService().getApis(0);
    Truth.assertThat(api.getMethodsCount()).isEqualTo(1);

    Method method = api.getMethods(0);
    Truth.assertThat(method.getName()).isEqualTo("pubsub.projects.topics.setIamPolicy");
    Truth.assertThat(method.getResponseTypeUrl()).isEqualTo("Policy");
    Truth.assertThat(imp.getConfig().getMethodParams().get(method.getName()))
        .containsExactly("resource", "request$")
        .inOrder();
    Truth.assertThat(imp.getConfig().getResources().get(method.getName()))
        .containsExactly("projects", "topics")
        .inOrder();

    Type requestType = null;
    for (Type t : imp.getTypes().values()) {
      if (t.getName().equals(method.getRequestTypeUrl())) {
        requestType = t;
        break;
      }
    }
    Truth.assertThat(requestType).isNotNull();
    Truth.assertThat(requestType.getFieldsList()).hasSize(2);

    Truth.assertThat(requestType.getFields(0).getName()).isEqualTo("resource");
    Truth.assertThat(requestType.getFields(0).getKind()).isEqualTo(Field.Kind.TYPE_STRING);

    Truth.assertThat(requestType.getFields(1).getName()).isEqualTo("request$");
    Truth.assertThat(requestType.getFields(1).getKind()).isEqualTo(Field.Kind.TYPE_MESSAGE);
    Truth.assertThat(requestType.getFields(1).getTypeUrl()).isEqualTo("SetIamPolicyRequest");

    Truth.assertThat(imp.getConfig().getAuthScopes().get(method.getName()))
        .containsExactly(
            "https://www.googleapis.com/auth/cloud-platform",
            "https://www.googleapis.com/auth/pubsub");
  }

  @Test
  public void testType() throws IOException {
    DiscoveryImporter imp = importer("type.json");

    // simple object
    Type type = imp.getTypes().get("SetIamPolicyRequest");
    Truth.assertThat(type.getName()).isEqualTo("SetIamPolicyRequest");
    Truth.assertThat(type.getFieldsList()).hasSize(1);
    Truth.assertThat(type.getFields(0).getName()).isEqualTo("policy");
    Truth.assertThat(type.getFields(0).getKind()).isEqualTo(Field.Kind.TYPE_MESSAGE);
    Truth.assertThat(type.getFields(0).getTypeUrl()).isEqualTo("Policy");

    // object with objects
    type = imp.getTypes().get("Creative");
    Truth.assertThat(type.getName()).isEqualTo("Creative");
    Truth.assertThat(type.getFieldsList()).hasSize(1);
    Truth.assertThat(type.getFields(0).getName()).isEqualTo("filteringReasons");
    type = imp.getTypes().get(type.getFields(0).getTypeUrl());
    Truth.assertThat(type.getFields(0).getName()).isEqualTo("date");
  }

  @Test
  public void testArray() throws IOException {
    DiscoveryImporter imp = importer("array.json");

    // named element type
    Type type = imp.getTypes().get("Policy");
    Truth.assertThat(type).isNotNull();
    Truth.assertThat(type.getName()).isEqualTo("Policy");
    Truth.assertThat(type.getFieldsList()).hasSize(1);
    Truth.assertThat(type.getFields(0).getKind()).isEqualTo(Field.Kind.TYPE_MESSAGE);
    Truth.assertThat(type.getFields(0).getCardinality())
        .isEqualTo(Field.Cardinality.CARDINALITY_REPEATED);
    Truth.assertThat(type.getFields(0).getTypeUrl()).isEqualTo("Binding");

    // primitive element type
    type = imp.getTypes().get("ClientAccessCapabilities");
    Truth.assertThat(type).isNotNull();
    Truth.assertThat(type.getName()).isEqualTo("ClientAccessCapabilities");
    Truth.assertThat(type.getFieldsList()).hasSize(1);
    Truth.assertThat(type.getFields(0).getKind()).isEqualTo(Field.Kind.TYPE_INT32);
    Truth.assertThat(type.getFields(0).getCardinality())
        .isEqualTo(Field.Cardinality.CARDINALITY_REPEATED);

    // element type declared in line
    type = imp.getTypes().get("CreativeDealIds");
    Truth.assertThat(type).isNotNull();
    Truth.assertThat(type.getName()).isEqualTo("CreativeDealIds");
    Truth.assertThat(type.getFieldsList()).hasSize(1);
    Truth.assertThat(type.getFields(0).getCardinality())
        .isEqualTo(Field.Cardinality.CARDINALITY_REPEATED);
    Truth.assertThat(type.getFields(0).getKind()).isEqualTo(Field.Kind.TYPE_MESSAGE);
    type = imp.getTypes().get(type.getFields(0).getTypeUrl());
    Truth.assertThat(type).isNotNull();
    Truth.assertThat(type.getFieldsList()).hasSize(3);

    // array of arrays
    type = imp.getTypes().get("Report");
    Truth.assertThat(type).isNotNull();
    Truth.assertThat(type.getName()).isEqualTo("Report");
    Truth.assertThat(type.getFieldsList()).hasSize(1);
    Truth.assertThat(type.getFields(0).getCardinality())
        .isEqualTo(Field.Cardinality.CARDINALITY_REPEATED);
    Truth.assertThat(type.getFields(0).getKind()).isEqualTo(Field.Kind.TYPE_MESSAGE);
    type = imp.getTypes().get(type.getFields(0).getTypeUrl());
    Truth.assertThat(type).isNotNull();
    Truth.assertThat(type.getFields(0).getCardinality())
        .isEqualTo(Field.Cardinality.CARDINALITY_REPEATED);
    Truth.assertThat(type.getFields(0).getKind()).isEqualTo(Field.Kind.TYPE_STRING);
  }
}

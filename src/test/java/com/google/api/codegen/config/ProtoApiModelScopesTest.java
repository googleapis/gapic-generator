/* Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.config;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.Authentication;
import com.google.api.AuthenticationRule;
import com.google.api.OAuthRequirements;
import com.google.api.Service;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import java.util.Arrays;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class ProtoApiModelScopesTest {
  private static final Model protoModel = Mockito.mock(Model.class);
  private static final ProtoParser protoParser = Mockito.mock(ProtoParser.class);
  private static final ProtoApiModel apiModel = new ProtoApiModel(protoModel, protoParser);
  private static final Interface libraryService = Mockito.mock(Interface.class);
  private static final Interface foodService = Mockito.mock(Interface.class);

  private static final AuthenticationRule scopes1 =
      AuthenticationRule.newBuilder()
          .setOauth(
              OAuthRequirements.newBuilder()
                  .setCanonicalScopes(
                      "https://www.googleapis.com/auth/calendar,\n"
                          + "https://www.googleapis.com/auth/calendar.read"))
          .build();
  private static final AuthenticationRule scopes2 =
      AuthenticationRule.newBuilder()
          .setOauth(
              OAuthRequirements.newBuilder()
                  .setCanonicalScopes(
                      "https://www.googleapis.com/auth/default, "
                          + "https://www.googleapis.com/auth/pumpkin"))
          .build();
  private static final Service serviceYaml =
      Service.newBuilder()
          .setAuthentication(Authentication.newBuilder().addRules(scopes1).addRules(scopes2))
          .build();

  @BeforeClass
  public static void startUp() {
    Mockito.when(protoParser.getAuthScopes(libraryService))
        .thenReturn(
            Arrays.asList(
                "https://www.googleapis.com/auth/library",
                "https://www.googleapis.com/auth/default"));
    Mockito.when(protoParser.getAuthScopes(foodService))
        .thenReturn(
            Arrays.asList(
                "https://www.googleapis.com/auth/food", "https://www.googleapis.com/auth/default"));
    Mockito.when(protoModel.getServiceConfig()).thenReturn(serviceYaml);
  }

  @Test
  public void testGetAuthScopes() {
    List<ProtoInterfaceModel> interfaces =
        Arrays.asList(
            new ProtoInterfaceModel(libraryService, protoParser),
            new ProtoInterfaceModel(foodService, protoParser));
    List<String> scopes = apiModel.getAuthScopes(protoParser, interfaces);
    assertThat(scopes.size()).isEqualTo(6);
    assertThat(scopes.contains("https://www.googleapis.com/auth/calendar")).isTrue();
    assertThat(scopes.contains("https://www.googleapis.com/auth/calendar.read")).isTrue();
    assertThat(scopes.contains("https://www.googleapis.com/auth/default")).isTrue();
    assertThat(scopes.contains("https://www.googleapis.com/auth/food")).isTrue();
    assertThat(scopes.contains("https://www.googleapis.com/auth/library")).isTrue();
    assertThat(scopes.contains("https://www.googleapis.com/auth/pumpkin")).isTrue();
  }
}

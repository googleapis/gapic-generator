package com.google.api.codegen.config;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.AuthRequirement;
import com.google.api.Authentication;
import com.google.api.AuthenticationRule;
import com.google.api.OAuthRequirements;
import com.google.api.Service;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class ServiceConfigTest {
  private static final Model protoModel = Mockito.mock(Model.class);
  private static final ProtoApiModel apiModel = new ProtoApiModel(protoModel);
  private static final ProtoParser protoParser = Mockito.mock(ProtoParser.class);
  private static final Interface libraryService = Mockito.mock(Interface.class);
  private static final Interface foodService = Mockito.mock(Interface.class);

  private static final Service serviceYaml = Service.newBuilder()
      .setAuthentication(
          Authentication.newBuilder()
          .addRules(AuthenticationRule.newBuilder()
              .setOauth(OAuthRequirements.newBuilder()
                  .setCanonicalScopes("https://www.googleapis.com/auth/calendar,\n"
                      + "https://www.googleapis.com/auth/calendar.read")))
          .addRules(AuthenticationRule.newBuilder().setOauth(OAuthRequirements.newBuilder()
              .setCanonicalScopes("https://www.googleapis.com/auth/default, "
                  + "https://www.googleapis.com/auth/pumpkin"))))
      .build();

  @BeforeClass
  public static void startUp() {
    Mockito.when(protoParser.getAuthScopes(libraryService)).thenReturn(
        Arrays.asList(
            "https://www.googleapis.com/auth/library",
            "https://www.googleapis.com/auth/default"));
    Mockito.when(protoParser.getAuthScopes(foodService)).thenReturn(
        Arrays.asList(
            "https://www.googleapis.com/auth/food",
            "https://www.googleapis.com/auth/default"));
    Mockito.when(protoModel.getServiceConfig()).thenReturn(serviceYaml);
  }

  @Test
  public void testServiceConfigFromProtoFile() {
    List<ProtoInterfaceModel> interfaces =  Arrays.asList(
        new ProtoInterfaceModel(libraryService),
        new ProtoInterfaceModel(foodService));
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

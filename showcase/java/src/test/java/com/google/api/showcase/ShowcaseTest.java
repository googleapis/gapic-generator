/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.api.showcase;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.rpc.AbortedException;
import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.api.gax.rpc.BidiStreamObserver;
import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.HeaderProvider;
import com.google.api.gax.rpc.ServerStream;
import com.google.api.gax.rpc.StreamController;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.protobuf.Duration;
import com.google.rpc.Code;
import com.google.rpc.Status;
import com.google.showcase.v1beta1.BlockRequest;
import com.google.showcase.v1beta1.BlockResponse;
import com.google.showcase.v1beta1.EchoClient;
import com.google.showcase.v1beta1.EchoRequest;
import com.google.showcase.v1beta1.EchoResponse;
import com.google.showcase.v1beta1.EchoSettings;
import com.google.showcase.v1beta1.ExpandRequest;
import io.grpc.StatusRuntimeException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests via Showcase: https://github.com/googleapis/gapic-showcase */
@RunWith(JUnit4.class)
public class ShowcaseTest {

  private EchoClient client;
  private ShowcaseTransportChannelProvider channelProvider;

  @Before
  public void setup() throws Exception {
    String host = System.getenv("HOST");
    if (host == null) host = "localhost";
    String port = System.getenv("PORT");
    if (port == null) port = "7469";
    channelProvider =
        new ShowcaseTransportChannelProvider(
            host, Integer.parseInt(port), new ShowcaseHeaderProvider());

    // init client for all tests
    client =
        EchoClient.create(
            EchoSettings.newBuilder()
                .setCredentialsProvider(() -> null)
                .setTransportChannelProvider(
                    new ShowcaseTransportChannelProvider(
                        host, Integer.parseInt(port), new ShowcaseHeaderProvider()))
                .build());
  }

  @After
  public void teardown() throws Exception {
    client.shutdownNow();
    client.awaitTermination(5, TimeUnit.SECONDS);
  }

  @Test
  public void echosTheRequest() {
    EchoResponse result = client.echo(EchoRequest.newBuilder().setContent("Hi there!").build());

    assertThat(result.getContent()).isEqualTo("Hi there!");
  }

  @Test(expected = StatusRuntimeException.class)
  public void throwsAnError() {
    try {
      client.echo(
          EchoRequest.newBuilder()
              .setContent("junk")
              .setError(
                  Status.newBuilder()
                      .setCode(Code.DATA_LOSS_VALUE)
                      .setMessage("DATA_LOSS: oh no!")
                      .build())
              .build());
    } catch (Exception e) {
      assertThat(e.getCause()).isInstanceOf(StatusRuntimeException.class);
      StatusRuntimeException error = (StatusRuntimeException) e.getCause();
      assertThat(error.getStatus().getDescription()).isEqualTo("DATA_LOSS: oh no!");
      assertThat(error.getStatus().getCode().value()).isEqualTo(Code.DATA_LOSS_VALUE);
      throw error;
    }
  }

  @Test
  public void canExpandAStreamOfResponses() {
    List<String> expansions = new ArrayList<>();

    ServerStream<EchoResponse> stream =
        client
            .expandCallable()
            .call(ExpandRequest.newBuilder().setContent("well hello there how are you").build());

    stream.iterator().forEachRemaining(response -> expansions.add(response.getContent()));

    assertThat(expansions).containsExactly("well", "hello", "there", "how", "are", "you").inOrder();
  }

  @Test(expected = AbortedException.class)
  public void canExpandAStreamOfResponsesAndThenError() {
    List<String> expansions = new ArrayList<>();

    ServerStream<EchoResponse> stream =
        client
            .expandCallable()
            .call(
                ExpandRequest.newBuilder()
                    .setContent("one two zee")
                    .setError(
                        Status.newBuilder().setCode(Code.ABORTED_VALUE).setMessage("yikes").build())
                    .build());

    try {
      stream.iterator().forEachRemaining(response -> expansions.add(response.getContent()));
    } catch (Exception ex) {
      assertThat(ex.getCause().getMessage()).isEqualTo("ABORTED: yikes");
      assertThat(expansions).containsExactly("one", "two", "zee").inOrder();
      throw ex;
    }
  }

  @Test
  public void canCollectAStreamOfRequests() throws InterruptedException {
    List<String> collections = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(1);

    ApiStreamObserver<EchoRequest> requestStream =
        client
            .collectCallable()
            .clientStreamingCall(
                new ApiStreamObserver<EchoResponse>() {
                  @Override
                  public void onNext(EchoResponse value) {
                    collections.add(value.getContent());
                  }

                  @Override
                  public void onError(Throwable t) {
                    fail("error not expected");
                  }

                  @Override
                  public void onCompleted() {
                    latch.countDown();
                  }
                });

    for (String request : new String[] {"a", "b", "c", "done"}) {
      requestStream.onNext(EchoRequest.newBuilder().setContent(request).build());
    }
    requestStream.onCompleted();

    latch.await(7, TimeUnit.SECONDS);

    assertThat(collections).containsExactly("a b c done");
  }

  @Test
  public void canHaveARandomChat() throws InterruptedException {
    List<String> responses = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(1);

    List<String> inputs =
        IntStream.range(0, 5)
            .mapToObj(
                idx ->
                    new Random()
                        .ints(20)
                        .mapToObj(Integer::toString)
                        .collect(Collectors.joining("->")))
            .collect(Collectors.toList());

    client
        .chatCallable()
        .call(
            new BidiStreamObserver<EchoRequest, EchoResponse>() {
              @Override
              public void onReady(ClientStream<EchoRequest> stream) {
                inputs.forEach(
                    message -> stream.send(EchoRequest.newBuilder().setContent(message).build()));
                stream.closeSend();
              }

              @Override
              public void onStart(StreamController controller) {
                // skip...
              }

              @Override
              public void onResponse(EchoResponse response) {
                responses.add(response.getContent());
              }

              @Override
              public void onError(Throwable t) {
                fail("error not expected");
              }

              @Override
              public void onComplete() {
                latch.countDown();
              }
            });

    latch.await(7, TimeUnit.SECONDS);

    assertThat(responses).containsExactlyElementsIn(inputs).inOrder();
  }

  @Test(expected = StatusRuntimeException.class)
  public void blockTimeout() {
    try {
      client.block(
          BlockRequest.newBuilder()
              // Set a longer timeout than the 5 seconds specified in the grpc_service_config.
              .setResponseDelay(Duration.newBuilder().setSeconds(10L).build())
              .build());
    } catch (Exception e) {
      assertThat(e.getCause()).isInstanceOf(StatusRuntimeException.class);
      StatusRuntimeException error = (StatusRuntimeException) e.getCause();
      assertThat(error.getStatus().getCode().value()).isEqualTo(Code.DEADLINE_EXCEEDED_VALUE);
      throw error;
    }
  }

  @Test
  public void block() {
    BlockResponse result =
        client.block(
            BlockRequest.newBuilder()
                .setResponseDelay(Duration.newBuilder().setSeconds(2L).build())
                .setSuccess(BlockResponse.newBuilder().setContent("Hello, World!").build())
                .build());
    assertThat(result.getContent()).isEqualTo("Hello, World!");
  }

  static GoogleCredentials loadCredentials(String credentialFile) {
    try {
      InputStream keyStream = new ByteArrayInputStream(credentialFile.getBytes());
      return GoogleCredentials.fromStream(keyStream);
    } catch (IOException e) {
      fail("Couldn't create fake JSON credentials.");
    }
    return null;
  }

  @Test
  public void quotaProjectIdTest() throws Exception {
    final String QUOTA_PROJECT_ID_KEY = "x-google-user-project";
    final String QUOTA_PROJECT_ID = "quota_project_id";
    final String QUOTA_PROJECT_ID_FROM_HEADER_VALUE = "quota_project_id_from_headers";
    final String QUOTA_PROJECT_ID_FROM_CREDENTIALS_VALUE = "quota_project_id_from_credentials";
    final String JSON_KEY_QUOTA_PROJECT_ID =
        "{\n"
            + "  \"private_key_id\": \"somekeyid\",\n"
            + "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggS"
            + "kAgEAAoIBAQC+K2hSuFpAdrJI\\nnCgcDz2M7t7bjdlsadsasad+fvRSW6TjNQZ3p5LLQY1kSZRqBqylRkzteMOyHg"
            + "aR\\n0Pmxh3ILCND5men43j3h4eDbrhQBuxfEMalkG92sL+PNQSETY2tnvXryOvmBRwa/\\nQP/9dJfIkIDJ9Fw9N4"
            + "Bhhhp6mCcRpdQjV38H7JsyJ7lih/oNjECgYAt\\nknddadwkwewcVxHFhcZJO+XWf6ofLUXpRwiTZakGMn8EE1uVa2"
            + "LgczOjwWHGi99MFjxSer5m9\\n1tCa3/KEGKiS/YL71JvjwX3mb+cewlkcmweBKZHM2JPTk0ZednFSpVZMtycjkbLa"
            + "\\ndYOS8V85AgMBewECggEBAKksaldajfDZDV6nGqbFjMiizAKJolr/M3OQw16K6o3/\\n0S31xIe3sSlgW0+UbYlF"
            + "4U8KifhManD1apVSC3csafaspP4RZUHFhtBywLO9pR5c\\nr6S5aLp+gPWFyIp1pfXbWGvc5VY/v9x7ya1VEa6rXvL"
            + "sKupSeWAW4tMj3eo/64ge\\nsdaceaLYw52KeBYiT6+vpsnYrEkAHO1fF/LavbLLOFJmFTMxmsNaG0tuiJHgjshB\\"
            + "n82DpMCbXG9YcCgI/DbzuIjsdj2JC1cascSP//3PmefWysucBQe7Jryb6NQtASmnv\\nCdDw/0jmZTEjpe4S1lxfHp"
            + "lAhHFtdgYTvyYtaLZiVVkCgYEA8eVpof2rceecw/I6\\n5ng1q3Hl2usdWV/4mZMvR0fOemacLLfocX6IYxT1zA1FF"
            + "JlbXSRsJMf/Qq39mOR2\\nSpW+hr4jCoHeRVYLgsbggtrevGmILAlNoqCMpGZ6vDmJpq6ECV9olliDvpPgWOP+\\nm"
            + "YPDreFBGxWvQrADNbRt2dmGsrsCgYEAyUHqB2wvJHFqdmeBsaacewzV8x9WgmeX\\ngUIi9REwXlGDW0Mz50dxpxcK"
            + "CAYn65+7TCnY5O/jmL0VRxU1J2mSWyWTo1C+17L0\\n3fUqjxL1pkefwecxwecvC+gFFYdJ4CQ/MHHXU81Lwl1iWdF"
            + "Cd2UoGddYaOF+KNeM\\nHC7cmqra+JsCgYEAlUNywzq8nUg7282E+uICfCB0LfwejuymR93CtsFgb7cRd6ak\\nECR"
            + "8FGfCpH8ruWJINllbQfcHVCX47ndLZwqv3oVFKh6pAS/vVI4dpOepP8++7y1u\\ncoOvtreXCX6XqfrWDtKIvv0vjl"
            + "HBhhhp6mCcRpdQjV38H7JsyJ7lih/oNjECgYAt\\nkndj5uNl5SiuVxHFhcZJO+XWf6ofLUregtevZakGMn8EE1uVa"
            + "2AY7eafmoU/nZPT\\n00YB0TBATdCbn/nBSuKDESkhSg9s2GEKQZG5hBmL5uCMfo09z3SfxZIhJdlerreP\\nJ7gSi"
            + "dI12N+EZxYd4xIJh/HFDgp7RRO87f+WJkofMQKBgGTnClK1VMaCRbJZPriw\\nEfeFCoOX75MxKwXs6xgrw4W//AYG"
            + "GUjDt83lD6AZP6tws7gJ2IwY/qP7+lyhjEqN\\nHtfPZRGFkGZsdaksdlaksd323423d+15/UvrlRSFPNj1tWQmNKk"
            + "XyRDW4IG1Oa2p\\nrALStNBx5Y9t0/LQnFI4w3aG\\n-----END PRIVATE KEY-----\\n\",\n"
            + "  \"project_id\": \"someprojectid\",\n"
            + "  \"client_email\": \"someclientid@developer.gserviceaccount.com\",\n"
            + "  \"client_id\": \"someclientid.apps.googleusercontent.com\",\n"
            + "  \"type\": \"service_account\",\n"
            + "  \"quota_project_id\": \""
            + QUOTA_PROJECT_ID_FROM_CREDENTIALS_VALUE
            + "\"\n"
            + "}";
    final GoogleCredentials credentialsWithQuotaProject =
        loadCredentials(JSON_KEY_QUOTA_PROJECT_ID);
    final CredentialsProvider credentialsProviderWithQuota =
        new CredentialsProvider() {
          @Override
          public Credentials getCredentials() throws IOException {
            return credentialsWithQuotaProject;
          }
        };
    final HeaderProvider headerProviderWithQuota =
        new HeaderProvider() {
          @Override
          public Map<String, String> getHeaders() {
            return Collections.singletonMap(
                QUOTA_PROJECT_ID_KEY, QUOTA_PROJECT_ID_FROM_HEADER_VALUE);
          }
        };

    EchoSettings settingsSetQuota =
        EchoSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(NoCredentialsProvider.create())
            .setQuotaProjectId(QUOTA_PROJECT_ID)
            .build();
    EchoSettings settingsHeader =
        EchoSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(NoCredentialsProvider.create())
            .setHeaderProvider(headerProviderWithQuota)
            .build();
    EchoSettings settingsHeaderAndQuota =
        EchoSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(NoCredentialsProvider.create())
            .setHeaderProvider(headerProviderWithQuota)
            .setQuotaProjectId(QUOTA_PROJECT_ID)
            .build();
    EchoSettings settingsCredentials =
        EchoSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(credentialsProviderWithQuota)
            .build();
    EchoSettings settingsCredentialsAndQuota =
        EchoSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(credentialsProviderWithQuota)
            .setQuotaProjectId(QUOTA_PROJECT_ID)
            .build();
    EchoSettings settingsQuotaAll =
        EchoSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(credentialsProviderWithQuota)
            .setQuotaProjectId(QUOTA_PROJECT_ID)
            .setHeaderProvider(headerProviderWithQuota)
            .build();

    validQuotaProjectId(settingsSetQuota, QUOTA_PROJECT_ID);
    validQuotaProjectId(settingsHeader, QUOTA_PROJECT_ID_FROM_HEADER_VALUE);
    validQuotaProjectId(settingsHeaderAndQuota, QUOTA_PROJECT_ID);
    validQuotaProjectId(settingsCredentials, QUOTA_PROJECT_ID_FROM_CREDENTIALS_VALUE);
    validQuotaProjectId(settingsCredentialsAndQuota, QUOTA_PROJECT_ID);
    validQuotaProjectId(client.getSettings(), null);
    validQuotaProjectId(settingsQuotaAll, QUOTA_PROJECT_ID);
  }

  private void validQuotaProjectId(EchoSettings settings, String expectId) throws Exception {
    EchoClient clientSetQuota = EchoClient.create(settings);
    Assert.assertEquals(clientSetQuota.getSettings().getQuotaProjectId(), expectId);
  }
}

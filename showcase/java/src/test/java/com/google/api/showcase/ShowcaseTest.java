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

import com.google.api.gax.rpc.AbortedException;
import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.api.gax.rpc.BidiStreamObserver;
import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ServerStream;
import com.google.api.gax.rpc.StreamController;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests via Showcase: https://github.com/googleapis/gapic-showcase */
@RunWith(JUnit4.class)
public class ShowcaseTest {

  private EchoClient client;

  @Before
  public void setup() throws Exception {
    String host = System.getenv("HOST");
    if (host == null) host = "localhost";
    String port = System.getenv("PORT");
    if (port == null) port = "7469";

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
}

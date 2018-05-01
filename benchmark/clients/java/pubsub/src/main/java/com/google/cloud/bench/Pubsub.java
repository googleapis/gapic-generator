package com.google.cloud.bench;

import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.grpc.FixedChannelProvider;
import com.google.api.gax.grpc.FixedExecutorProvider;
import com.google.auto.value.AutoValue;
import com.google.cloud.pubsub.spi.v1.TopicAdminClient;
import com.google.cloud.pubsub.spi.v1.TopicAdminSettings;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.pubsub.v1.GetTopicRequest;
import com.google.pubsub.v1.PublisherGrpc;
import com.google.pubsub.v1.PublisherGrpc.PublisherFutureStub;
import com.google.pubsub.v1.Topic;
import com.google.pubsub.v1.TopicName;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import java.io.File;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class Pubsub {
  @AutoValue
  abstract static class Settings {
    abstract long warmDurNano();

    abstract long targetDurNano();

    abstract int numWorkers();

    abstract String endpoint();

    abstract String cert();

    static Builder builder() {
      return new AutoValue_Pubsub_Settings.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder warmDurNano(long val);

      abstract Builder targetDurNano(long val);

      abstract Builder numWorkers(int val);

      abstract Builder endpoint(String val);

      abstract Builder cert(String val);

      abstract Settings build();
    }
  }

  private static final TopicName TOPIC_NAME_RESOURCE =
      TopicName.create("benchmark-project", "benchmark-topic");
  private static final String TOPIC_NAME_STRING = TOPIC_NAME_RESOURCE.toString();
  private static final long BILLION = 1000L * 1000L * 1000L;
  private static final long MILLION = 1000L * 1000L;
  private static final Metadata.Key<String> X_GOOG_HEADER_KEY =
      Metadata.Key.of("x-goog-api-client", Metadata.ASCII_STRING_MARSHALLER);
  private static final String X_GOOG_HEADER_VALUE =
      "gl-java/1.8.0_112-google-v7 gapic/ gax/1.1.1-SNAPSHOT grpc/1.10.0";

  public static void main(String[] args) throws Exception {
    Options options =
        new Options()
            .addOption(
                Option.builder("c")
                    .longOpt("client")
                    .required()
                    .hasArg()
                    .desc("client to use: gapic/grpc")
                    .build())
            .addOption(Option.builder("cert").required().hasArg().desc("certificate file").build())
            .addOption("ep", "endpoint", true, "endpoint to connect to")
            .addOption("n", "num_workers", true, "number of concurrent calls")
            .addOption("wd", "warmup_duration", true, "warmup duration in seconds")
            .addOption("d", "duration", true, "test duration in seconds");
    CommandLine helpCl =
        new DefaultParser()
            .parse(new Options().addOption("h", "help", false, "print help message"), args, true);
    if (helpCl.hasOption('h')) {
      new HelpFormatter().printHelp("Pubsub -c <client> -cert <cert_file> -n <num_workers>", options);
      return;
    }

    CommandLine cl = new DefaultParser().parse(options, args);

    Settings settings =
        Settings.builder()
            .warmDurNano(Long.parseLong(cl.getOptionValue("wd", "60")) * BILLION)
            .targetDurNano(Long.parseLong(cl.getOptionValue("d", "60")) * BILLION)
            .numWorkers(Integer.parseInt(cl.getOptionValue("n", "20")))
            .endpoint(cl.getOptionValue("ep", "localhost:8080"))
            .cert(cl.getOptionValue("cert"))
            .build();

    String client = cl.getOptionValue("c", "");
    switch (client) {
      case "grpc":
        grpc(settings);
        break;
      case "gapic":
        gapic(settings);
        break;
      default:
        throw new IllegalArgumentException("unknown client: " + client);
    }
  }

  private static void gapic(final Settings settings) throws Exception {
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    // In real clients, InstantiatingChannelProvider is responsible for adding interceptors.
    // We can't use it here because InstantiatingChannelProvider doesn't have sslContext method.
    // Instead, we imitate HeaderInterceptor.
    ClientInterceptor headerInterceptor =
        new ClientInterceptor() {
          @Override
          public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
              MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
            ClientCall<ReqT, RespT> call = next.newCall(method, callOptions);
            return new SimpleForwardingClientCall<ReqT, RespT>(call) {
              @Override
              public void start(ClientCall.Listener<RespT> responseListener, Metadata headers) {
                headers.put(X_GOOG_HEADER_KEY, X_GOOG_HEADER_VALUE);
                super.start(responseListener, headers);
              }
            };
          }
        };
    ManagedChannel channel =
        NettyChannelBuilder.forTarget(settings.endpoint())
            .executor(executor)
            .sslContext(GrpcSslContexts.forClient().trustManager(new File(settings.cert())).build())
            .intercept(headerInterceptor)
            .build();
    final Semaphore semaphore = new Semaphore(settings.numWorkers());
    final TopicAdminClient client =
        TopicAdminClient.create(
            TopicAdminSettings.defaultBuilder()
                .setChannelProvider(FixedChannelProvider.create(channel))
                .setExecutorProvider(FixedExecutorProvider.create(executor))
                .build());

    final AtomicLong resetTime = new AtomicLong();
    final AtomicLong numCalls = new AtomicLong();
    final AtomicLong numErrs = new AtomicLong();

    long endTime = System.nanoTime() + settings.warmDurNano() + settings.targetDurNano();

    Thread resetter =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                try {
                  Thread.sleep(settings.warmDurNano() / MILLION);
                } catch (InterruptedException e) {
                  throw new RuntimeException(e);
                }

                numCalls.set(0);
                numErrs.set(0);
                resetTime.set(System.nanoTime());
              }
            });
    resetter.start();

    while (System.nanoTime() < endTime) {
      semaphore.acquire(1);
      ApiFutures.addCallback(
          client
              .getTopicCallable()
              .futureCall(
                  GetTopicRequest.newBuilder().setTopicWithTopicName(TOPIC_NAME_RESOURCE).build()),
          new ApiFutureCallback<Topic>() {
            @Override
            public void onSuccess(Topic topic) {
              if (!topic.getName().equals(TOPIC_NAME_STRING)) {
                numErrs.incrementAndGet();
              }
              both();
            }

            @Override
            public void onFailure(Throwable t) {
              numErrs.incrementAndGet();
              both();
            }

            void both() {
              numCalls.incrementAndGet();
              semaphore.release(1);
            }
          });
    }

    long nCalls = numCalls.get();
    long nErrs = numErrs.get();
    long runDurNano = System.nanoTime() - resetTime.get();

    System.out.println("errors: " + nErrs);
    System.out.println("calls: " + nCalls);
    System.out.println("time per call (ns): " + (runDurNano / nCalls));
    System.out.println("QPS: " + (nCalls * BILLION / runDurNano));

    client.close();
    channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);
  }

  private static void grpc(final Settings settings) throws Exception {
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    ManagedChannel channel =
        NettyChannelBuilder.forTarget(settings.endpoint())
            .executor(executor)
            .sslContext(GrpcSslContexts.forClient().trustManager(new File(settings.cert())).build())
            .build();
    final Semaphore semaphore = new Semaphore(settings.numWorkers());

    Metadata header = new Metadata();
    header.put(X_GOOG_HEADER_KEY, X_GOOG_HEADER_VALUE);
    final PublisherFutureStub stub =
        MetadataUtils.attachHeaders(PublisherGrpc.newFutureStub(channel), header);

    final AtomicLong resetTime = new AtomicLong();
    final AtomicLong numCalls = new AtomicLong();
    final AtomicLong numErrs = new AtomicLong();

    long endTime = System.nanoTime() + settings.warmDurNano() + settings.targetDurNano();

    Thread resetter =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                try {
                  Thread.sleep(settings.warmDurNano() / MILLION);
                } catch (InterruptedException e) {
                  throw new RuntimeException(e);
                }

                numCalls.set(0);
                numErrs.set(0);
                resetTime.set(System.nanoTime());
              }
            });
    resetter.start();

    while (System.nanoTime() < endTime) {
      semaphore.acquire(1);
      Futures.addCallback(
          stub.getTopic(GetTopicRequest.newBuilder().setTopic(TOPIC_NAME_STRING).build()),
          new FutureCallback<Topic>() {
            @Override
            public void onSuccess(Topic topic) {
              if (!topic.getName().equals(TOPIC_NAME_STRING)) {
                numErrs.incrementAndGet();
              }
              both();
            }

            @Override
            public void onFailure(Throwable t) {
              numErrs.incrementAndGet();
              both();
            }

            void both() {
              numCalls.incrementAndGet();
              semaphore.release(1);
            }
          });
    }

    long nCalls = numCalls.get();
    long nErrs = numErrs.get();
    long runDurNano = System.nanoTime() - resetTime.get();

    System.out.println("errors: " + nErrs);
    System.out.println("calls: " + nCalls);
    System.out.println("time per call (ns): " + (runDurNano / nCalls));
    System.out.println("QPS: " + (nCalls * BILLION / runDurNano));

    channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);
  }
}

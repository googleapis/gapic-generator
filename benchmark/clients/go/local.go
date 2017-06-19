//+build ignore

package main

import (
	"flag"
	"fmt"
	"log"
	"os"
	"runtime"
	"runtime/pprof"
	"sync"
	"sync/atomic"
	"time"

	"google.golang.org/api/option"
	"google.golang.org/grpc/credentials"
	"google.golang.org/grpc/metadata"

	"golang.org/x/net/context"

	gapic "cloud.google.com/go/pubsub/apiv1"
	pb "google.golang.org/genproto/googleapis/pubsub/v1"
	"google.golang.org/grpc"
)

func main() {
	client := flag.String("client", "", "client: gapic/grpc")
	address := flag.String("address", "localhost:8080", "server to connect to")
	cert := flag.String("cert", "", "cert file")
	nworker := flag.Int("num_workers", 20, "number of workers")
	warmDur := flag.Duration("warmup", time.Minute, "time to warm up")
	runDur := flag.Duration("duration", time.Minute, "time to spam server for, does not include warmup time")
	cpuprofile := flag.String("cpuprofile", "", "write cpu profile here")
	flag.Parse()

	if p := *cpuprofile; p != "" {
		f, err := os.Create(p)
		if err != nil {
			log.Fatal(err)
		}
		if err := pprof.StartCPUProfile(f); err != nil {
			log.Fatal(err)
		}
		defer pprof.StopCPUProfile()
	}

	runtime.GOMAXPROCS(1)

	if *cert == "" {
		log.Fatal("need TLS cert and key, for testing you can create your own: https://workaround.org/ispmail/jessie/create-certificate")
	}
	creds, err := credentials.NewClientTLSFromFile(*cert, "")
	if err != nil {
		log.Fatal(err)
	}

	conn, err := grpc.Dial(*address, grpc.WithTransportCredentials(creds))
	if err != nil {
		log.Fatal(err)
	}

	var bfn benchFunc
	var bctx context.Context

	switch *client {
	case "gapic":
		c, err := gapic.NewPublisherClient(context.Background(), option.WithGRPCConn(conn))
		if err != nil {
			log.Fatal(err)
		}
		bfn = func(ctx context.Context, req *pb.GetTopicRequest) (*pb.Topic, error) {
			return c.GetTopic(ctx, req)
		}
		bctx = context.Background()
	case "grpc":
		c := pb.NewPublisherClient(conn)
		bfn = func(ctx context.Context, req *pb.GetTopicRequest) (*pb.Topic, error) {
			return c.GetTopic(ctx, req)
		}
		bctx = metadata.NewOutgoingContext(context.Background(), metadata.Pairs("x-goog-api-client", "gl-go/1.8.1 gapic/20170404 gax/0.1.0 grpc/1.3.0-dev"))
	default:
		log.Fatalf("unknown client: %q", *client)
	}

	bench(bctx, bfn, *nworker, *warmDur, *runDur)
}

type benchFunc func(context.Context, *pb.GetTopicRequest) (*pb.Topic, error)

func bench(ctx context.Context, fn benchFunc, nworker int, warmDur, targetDur time.Duration) {
	const topicName = "projects/benchmark-project/topics/benchmark-topic"

	end := time.Now().Add(warmDur + targetDur)

	var numCalls, numErrs int64

	var wg sync.WaitGroup
	wg.Add(nworker + 1)

	for i := 0; i < nworker; i++ {
		go func() {
			defer wg.Done()
			for time.Now().Before(end) {
				res, err := fn(ctx, &pb.GetTopicRequest{Topic: topicName})
				if err != nil {
					atomic.AddInt64(&numErrs, 1)
				} else if res.Name != topicName {
					atomic.AddInt64(&numErrs, 1)
				}
				atomic.AddInt64(&numCalls, 1)
			}
		}()
	}

	var resetTime time.Time
	time.AfterFunc(warmDur, func() {
		atomic.StoreInt64(&numCalls, 0)
		atomic.StoreInt64(&numErrs, 0)
		resetTime = time.Now()
		wg.Done()
	})

	wg.Wait()

	// Might not be exactly runDur, in case AfterFunc wakes up late.
	runDur := time.Since(resetTime)
	nCalls := atomic.LoadInt64(&numCalls)
	nErrs := atomic.LoadInt64(&numErrs)

	fmt.Printf("calls: %d, errs: %d, duration %s, time per call: %s, QPS: %f\n",
		nCalls,
		nErrs,
		runDur,
		runDur/time.Duration(nCalls),
		float64(nCalls)/runDur.Seconds())
}

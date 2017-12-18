//+build ignore

package main

import (
	"flag"
	"log"
	"net"

	"golang.org/x/net/context"

	pb "google.golang.org/genproto/googleapis/pubsub/v1"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"
)

func main() {
	addr := flag.String("a", "localhost:8080", "address")
	key := flag.String("key", "", "key file")
	cert := flag.String("cert", "", "cert file")
	flag.Parse()

	lis, err := net.Listen("tcp", *addr)
	if err != nil {
		log.Fatal(err)
	}

	if *cert == "" || *key == "" {
		log.Fatal("need TLS cert and key, for testing you can create your own: https://workaround.org/ispmail/jessie/create-certificate")
	}
	creds, err := credentials.NewServerTLSFromFile(*cert, *key)
	if err != nil {
		log.Fatal(err)
	}

	gs := grpc.NewServer(grpc.Creds(creds))
	pb.RegisterPublisherServer(gs, &serv{})
	log.Fatal(gs.Serve(lis))
}

type serv struct {
	pb.PublisherServer
}

func (*serv) GetTopic(_ context.Context, r *pb.GetTopicRequest) (*pb.Topic, error) {
	return &pb.Topic{Name: r.Topic}, nil
}

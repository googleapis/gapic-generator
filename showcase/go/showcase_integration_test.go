// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package showcase_integration

import (
	"context"
	"flag"
	"io"
	"log"
	"os"
	"strings"
	"testing"
	"time"

	showcase "cloud.google.com/go/showcase/apiv1alpha2"
	durationpb "github.com/golang/protobuf/ptypes/duration"
	genprotopb "github.com/googleapis/gapic-showcase/server/genproto"
	"google.golang.org/api/iterator"
	"google.golang.org/api/option"
	spb "google.golang.org/genproto/googleapis/rpc/status"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

var client *showcase.EchoClient

func TestMain(m *testing.M) {
	flag.Parse()

	conn, err := grpc.Dial("localhost:7469", grpc.WithInsecure())
	if err != nil {
		log.Fatal(err)
	}
	clientOpt := option.WithGRPCConn(conn)
	client, err = showcase.NewEchoClient(context.Background(), clientOpt)
	if err != nil {
		log.Fatal(err)
	}

	os.Exit(m.Run())
}

func TestEcho(t *testing.T) {
	t.Parallel()
	content := "hello world!"
	req := &genprotopb.EchoRequest{
		Response: &genprotopb.EchoRequest_Content{
			Content: content,
		},
	}
	resp, err := client.Echo(context.Background(), req)

	if err != nil {
		t.Fatal(err)
	}
	if resp.GetContent() != req.GetContent() {
		t.Errorf("Echo() = %q, want %q", resp.GetContent(), content)
	}
}

func TestEcho_error(t *testing.T) {
	t.Parallel()
	val := codes.Canceled
	req := &genprotopb.EchoRequest{
		Response: &genprotopb.EchoRequest_Error{
			Error: &spb.Status{Code: int32(val)},
		},
	}
	resp, err := client.Echo(context.Background(), req)

	if resp != nil {
		t.Errorf("Echo() = %v, wanted error %d", resp, val)
	}
	status, _ := status.FromError(err)
	if status.Code() != val {
		t.Errorf("Echo() errors with %d, want %d", status.Code(), val)
	}
}

func TestExpand(t *testing.T) {
	t.Parallel()
	content := "The rain in Spain stays mainly on the plain!"
	req := &genprotopb.ExpandRequest{Content: content}
	s, err := client.Expand(context.Background(), req)
	if err != nil {
		t.Fatal(err)
	}
	resps := []string{}
	for {
		resp, err := s.Recv()
		if err == io.EOF {
			break
		}
		if err != nil {
			t.Fatal(err)
		}
		resps = append(resps, resp.GetContent())
	}
	got := strings.Join(resps, " ")
	if content != got {
		t.Errorf("Expand() = %q, want %q", got, content)
	}
}

func TestCollect(t *testing.T) {
	t.Parallel()
	content := "The rain in Spain stays mainly on the plain!"
	s, err := client.Collect(context.Background())
	if err != nil {
		t.Fatal(err)
	}

	for _, str := range strings.Split(content, " ") {
		s.Send(&genprotopb.EchoRequest{
			Response: &genprotopb.EchoRequest_Content{Content: str}})
	}

	resp, err := s.CloseAndRecv()
	if err != nil {
		t.Fatal(err)
	}
	if content != resp.GetContent() {
		t.Errorf("Collect() = %q, want %q", resp.GetContent(), content)
	}
}

func TestChat(t *testing.T) {
	t.Parallel()
	content := "The rain in Spain stays mainly on the plain!"
	s, err := client.Chat(context.Background())
	if err != nil {
		t.Fatal(err)
	}
	for _, str := range strings.Split(content, " ") {
		s.Send(&genprotopb.EchoRequest{
			Response: &genprotopb.EchoRequest_Content{Content: str}})
	}
	s.CloseSend()
	resps := []string{}
	for {
		resp, err := s.Recv()
		if err == io.EOF {
			break
		}
		if err != nil {
			t.Fatal(err)
		}
		resps = append(resps, resp.GetContent())
	}
	got := strings.Join(resps, " ")
	if content != got {
		t.Errorf("Chat() = %q, want %q", got, content)
	}
}

func TestWait(t *testing.T) {
	t.Parallel()
	content := "hello world!"
	req := &genprotopb.WaitRequest{
		ResponseDelay: &durationpb.Duration{Nanos: 100},
		Response: &genprotopb.WaitRequest_Success{
			Success: &genprotopb.WaitResponse{Content: content},
		},
	}
	resp, err := client.Wait(context.Background(), req)
	if err != nil {
		t.Fatal(err)
	}
	if resp.GetContent() != content {
		t.Errorf("Wait() = %q, want %q", resp.GetContent(), content)
	}
}

func TestWait_timeout(t *testing.T) {
	t.Parallel()
	content := "hello world!"
	req := &genprotopb.WaitRequest{
		ResponseDelay: &durationpb.Duration{Seconds: 1},
		Response: &genprotopb.WaitRequest_Success{
			Success: &genprotopb.WaitResponse{Content: content},
		},
	}
	ctx, _ := context.WithTimeout(context.Background(), 20*time.Millisecond)
	resp, err := client.Wait(ctx, req)

	if err == nil {
		t.Errorf("Wait() = %v, want error", resp)
	}
}


func TestPagination(t *testing.T) {
	t.Parallel()
	req := &genprotopb.PaginationRequest{PageSize: 5, MaxResponse: 20}
	iter := client.Pagination(context.Background(), req)

	expected := int32(0)
	for {
		i, err := iter.Next()
		if err == iterator.Done {
			break
		}
		if err != nil {
			t.Fatal(err)
		}
		if i != expected {
			t.Errorf("Chat() = %d, want %d", i, expected)
		}
		expected++
	}
	if expected != 20 {
		t.Errorf("Pagination expected to see 20 vals")
	}
}

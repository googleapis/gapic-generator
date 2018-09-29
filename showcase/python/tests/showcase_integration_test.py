# Copyright 2018 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Showcase based integration tests."""

import grpc
import pytest

from google import showcase_v1alpha2
from google.api_core import exceptions
from google.rpc import status_pb2
from google.showcase_v1alpha2.proto import echo_pb2
from google.showcase_v1alpha2.gapic.transports import echo_grpc_transport


class TestEchoClient(object):
    channel = grpc.insecure_channel('localhost:7469')
    transport = echo_grpc_transport.EchoGrpcTransport(channel=channel)
    client = showcase_v1alpha2.EchoClient(transport=transport)

    def test_echo(self):
        content = 'hello world'
        response = self.client.echo(content=content)
        assert content == response.content

    def test_echo_exception(self):
        with pytest.raises(exceptions.InternalServerError):
            self.client.echo(
                error=status_pb2.Status(code=13, message='error!!!'))

    def test_expand(self):
        content = 'The rain in Spain stays mainly on the Plain!'
        response = []
        for r in self.client.expand(content=content):
            response.append(r.content)

        assert content == ' '.join(response)

    def test_collect(self):
        content = 'The rain in Spain stays mainly on the Plain!'
        requests = content.split(' ')
        requests = map(lambda s: echo_pb2.EchoRequest(content=s), requests)
        response = self.client.collect(iter(requests))

        assert content == response.content

    def test_chat(self):
        content = 'The rain in Spain stays mainly on the Plain!'
        requests = content.split(' ')
        requests = map(lambda s: echo_pb2.EchoRequest(content=s), requests)
        responses = self.client.chat(iter(requests))
        responses = map(lambda r: r.content, responses)

        assert content == ' '.join(responses)

    def test_wait(self):
        content = 'hello world'
        response = self.client.wait(
            response_delay={'nanos': 500 }, success={'content': content})
        assert content == response.content

    def test_pagination(self):
        expected = 0
        for element in self.client.pagination(20, page_size=5):
            assert element == expected
            expected = expected + 1

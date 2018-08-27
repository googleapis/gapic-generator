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

require "minitest/autorun"
require "minitest/spec"

require "grpc"

require "google/showcase/v1alpha1/echo_client"
require "google/showcase/v1alpha1/echo_services_pb"

describe Google::Showcase::V1alpha1::EchoClient do
  before(:all) do
    @client = Google::Showcase::V1alpha1::EchoClient.new(
      credentials: GRPC::Core::Channel.new(
        "localhost:7469", nil, :this_channel_is_insecure))
  end

  describe 'echo' do
    it 'invokes echo without error' do
      # Create expected grpc response
      content = "Echo Content"
      expected_response = { content: content }
      expected_response = Google::Gax::to_proto(
        expected_response, Google::Showcase::V1alpha1::EchoResponse)

      # Call method
      response = @client.echo(content: content)

      # Verify the response
      assert_equal(expected_response, response)
    end

    it 'invokes echo with error' do
      # Create expected grpc response
      error = {
        code: 13, # 500 internal service error
        message: "Errors errors errors!"
      }

      # Verify the response
      assert_raises do
        @client.echo(error: error)
      end
    end
  end

  describe 'expand' do
    it 'invokes expand' do
      content = "The rain in Spain stays mainly on the plain!"

      response = []

      @client.expand(content: content).each do |resp|
        response << resp.content
      end

      assert_equal(response.join(" "), content)
    end
  end

  describe 'collect' do
    it 'invokes collect' do
      expected = "The rain in spain stays mainly on the plain!"

      requests = expected.split(" ").map { |s| {content: s} }

      response = @client.collect(requests)

      assert_equal(expected, response.content)
    end
  end

  describe 'chat' do
    it 'invokes chat' do
      expected = "The rain in spain stays mainly on the plain!"

      requests = expected.split(" ").map { |s| {content: s} }
      response = []

      @client.chat(requests).each do |resp|
        response << resp.content
      end

      assert_equal(response.join(" "), expected)
    end
  end

  describe 'wait' do
    it 'invokes wait' do
      # Create expected grpc response
      response_delay = {seconds: 2}
      success = { content: "wait Content" }
      expected_response = Google::Gax::to_proto(
        success, Google::Showcase::V1alpha1::WaitResponse)

      # Call method
      response = @client.wait(response_delay, success: success)

      # Verify the response
      assert_equal(expected_response, response)
    end
  end

  describe 'pagination' do
    it 'invokes pagination for each element' do
      page_size = 5
      max_response = 20
      expected = 0
      @client.pagination(max_response, page_size: 5).each do |element|
        assert_equal(expected, element)
        expected = expected + 1
      end
    end

    it 'invokes pagination for each page'
      page_size = 5
      max_response = 20
      expected = 0
      pages = 0
      @client.pagination(max_response, page_size: 5).each_page do |page|
        pages = pages + 1
        page.each do |element|
          assert_equal(expected, element)
          expected = expected + 1
        end
      end
      assert_equal(4, pages)
    end
  end

end

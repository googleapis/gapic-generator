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
      # Call method
      response = @client.echo(error: error)

      # Verify the response
      assert_equal(expected_response, response)
    end
  end

  describe 'wait' do
    it 'invokes wait without error' do
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
end

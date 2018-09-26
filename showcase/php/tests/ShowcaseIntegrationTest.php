<?php
/*
 * Copyright 2018 Google LLC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

use Google\ApiCore\ApiException;
use Google\ApiCore\ValidationException;
use Google\Auth\CredentialsLoader;
use Google\Rpc\Code;
use Google\Rpc\Status;
use PHPUnit\Framework\TestCase;

class ShowcaseIntegrationTest extends TestCase
{
    private static $grpcClient;

    /**
     * @param Google\Showcase\V1alpha2\EchoClient $client
     * @throws ApiException
     * @dataProvider clientProvider
     */
    public function testUnary($client)
    {
        $response = $client->echo_([
            'content' => '"Wales snail hail fails!" wails Gail'
        ]);
        $this->assertEquals(
            '"Wales snail hail fails!" wails Gail',
            $response->getContent()
        );
    }

    /**
     * @param Google\Showcase\V1alpha2\EchoClient $client
     * @expectedException \Google\ApiCore\ApiException
     * @dataProvider clientProvider
     */
    public function testFailUnary($client)
    {
        try {
            $client->echo_([
                'error' => (new Status())
                    ->setCode(Code::INVALID_ARGUMENT)
                    ->setMessage("Unary error message"),
            ]);
        } catch (ApiException $ex) {
            $this->assertEquals("Unary error message", $ex->getBasicMessage());
            $this->assertEquals(Code::INVALID_ARGUMENT, $ex->getCode());
            throw $ex;
        }
    }

    /**
     * @return \Google\Showcase\V1alpha2\EchoClient[]
     * @throws \Google\ApiCore\ValidationException
     */
    public function clientProvider()
    {
        try {
            if (empty(self::$grpcClient)) {
                self::$grpcClient = new \Google\Showcase\V1alpha2\EchoClient([
                    'serviceAddress' => 'localhost:7469',
                    'transport' => 'grpc',
                    'transportConfig' => [
                        'grpc' => [
                            'stubOpts' => [
                                'credentials' => null,
                            ]
                        ]
                    ],
                    'credentials' => CredentialsLoader::makeInsecureCredentials(),
                ]);
            }

            // TODO(michaelbausor): add clients that use alternate transports
            // (rest, grpc-fallback) once they are supported by Showcase
            return [
                [self::$grpcClient],
            ];
        } catch (ValidationException $ex) {
            var_dump($ex);
            throw $ex;
        }
    }
}

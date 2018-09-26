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

'use strict';

const assert = require('assert');
const grpc = require('grpc');
const showcase = require('showcase');

describe('EchoClient', () => {
  var client;
  before(() => {
    client = new showcase.v1alpha2.EchoClient({
      grpc: grpc,
      sslCreds: grpc.credentials.createInsecure(),
      port: '7469',
    });
  });

  describe('echo', () => {
    it('invokes echo without error', done => {
      let request = {content: 'hello world'}
      client.echo(request)
        .then(responses => {
          assert.equal(request.content, responses[0].content)
        })
        .then(done)
        .catch(done);
    });
    it('invokes echo with error', done => {
      let request = {
        error : {
          code: 13,
          message: "Errors errors errors!"
        }
      };

      client.echo(request)
        .then(() => {
          done(new Error('should not get here.'))
        })
        .catch(() => {
          done();
        });
    });
  });

  describe('expand', () => {
    it('invokes expand', done => {
      let request = {content: 'The rain in Spain stays mainly on the plain!'}
      let responses = []
      client.expand(request)
        .on('data', response => {
          responses.push(response.content)
        })
        .on('error', done)
        .on('end', () => {
          assert.equal(request.content, responses.join(' '));
          done();
        });
    });
  });

  describe('collect', () => {
    it('invokes collect', done => {
      let expected = 'The rain in Spain stays mainly on the plain!'
      let responses = []
      let s = client.collect((err, response) => {
        if (err) {
          done(err);
        }
        assert.equal(expected, response.content);
        done();
      });

      expected
        .split(' ')
        .map(s => { return {content: s}; })
        .forEach(req => { s.write(req); });
      s.end()
    });
  });

  describe('chat', () => {
    it('invokes chat', done => {
      let expected = 'The rain in Spain stays mainly on the plain!'
      let responses = []
      let s = client.chat()
        .on('data', response => {
          responses.push(response.content)
        })
        .on('error', done)
        .on('end', () => {
          assert.equal(expected, responses.join(' '));
          done();
        });

      expected
        .split(' ')
        .map(s => { return {content: s}; })
        .forEach(req => { s.write(req) });
      s.end()
    });
  });
});

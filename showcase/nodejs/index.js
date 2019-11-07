const assert = require('assert');
const showcase = require('showcase');

// Fake auth client for fallback
const authStub = {
  getRequestHeaders() {
    return {Authorization: 'Bearer SOME_TOKEN'};
  },
};

// Helper function to run tests according to the given environment
function testShowcase(opts) {
  opts = opts || {};
  const clientOptions = {};
  let hasStreaming = true;
  if (opts.browser) {
    clientOptions.protocol = 'http';
    clientOptions.servicePath = 'localhost';
    clientOptions.port = 1337;
    clientOptions.auth = authStub;
    hasStreaming = false;
  } else if (opts.fallback) {
    clientOptions.protocol = 'http';
    clientOptions.servicePath = 'localhost';
    clientOptions.port = 1337;
    clientOptions.auth = authStub;
    clientOptions.fallback = true;
    hasStreaming = false;
  } else if (opts.grpcJs) {
    const grpc = require('@grpc/grpc-js');
    clientOptions.sslCreds = grpc.credentials.createInsecure();
    clientOptions.grpc = grpc;
  } else if (opts.grpc) {
    const grpc = require('grpc');
    clientOptions.sslCreds = grpc.credentials.createInsecure();
    clientOptions.grpc = grpc;
  } else {
    throw new Error('Wrong options passed!');
  }
  const client = new showcase.v1beta1.EchoClient(clientOptions);
  runTest(client, {hasStreaming});
}

function runTest(client, opts) {
  opts = opts || {};
  testEcho(client);
  if (opts.hasStreaming) {
    testExpand(client);
    testCollect(client);
    testChat(client);
  }
  testPagedExpand(client);
  testPagedExpandStream(client);
  testWait(client);
}

// Set of functions to tests all showcase methods
function testEcho(client) {
  it('echo', async () => {
    const request = {
      content: 'test',
    };
    const [response] = await client.echo(request);
    assert.deepStrictEqual(request.content, response.content);
  });
}

function testExpand(client) {
  it('expand', async () => {
    const words = ['nobody', 'ever', 'reads', 'test', 'input'];
    const request = {
      content: words.join(' '),
    };
    const result = await new Promise((resolve, reject) => {
      const stream = client.expand(request);
      const result = [];
      stream.on('data', response => {
        result.push(response.content);
      });
      stream.on('end', () => {
        resolve(result);
      });
      stream.on('error', reject);
    });
    assert.deepStrictEqual(words, result);
  });
}

function testPagedExpand(client) {
  it('pagedExpand', async () => {
    const words = ['nobody', 'ever', 'reads', 'test', 'input'];
    const request = {
      content: words.join(' '),
      pageSize: 2,
    };
    const [response] = await client.pagedExpand(request);
    const result = response.map(r => r.content);
    assert.deepStrictEqual(words, result);
  });
}

function testPagedExpandStream(client) {
  it('pagedExpand with streaming', async () => {
    const words = ['I', 'did', 'not', 'even', 'know', 'it', 'works'];
    const request = {
      content: words.join(' '),
      pageSize: 2,
    };
    const result = await new Promise((resolve, reject) => {
      const stream = client.pagedExpandStream(request);
      const result = [];
      stream.on('data', response => {
        result.push(response.content);
      });
      stream.on('end', () => {
        resolve(result);
      });
      stream.on('error', reject);
    });
    assert.deepStrictEqual(words, result);
  });
}

function testCollect(client) {
  it('collect', async () => {
    const words = ['nobody', 'ever', 'reads', 'test', 'input'];
    const result = await new Promise((resolve, reject) => {
      const stream = client.collect((err, result) => {
        if (err) {
          reject(err);
          return;
        }
        resolve(result);
      });
      for (const word of words) {
        const request = {content: word};
        stream.write(request);
      }
      stream.end();
    });
    assert.deepStrictEqual(result.content, words.join(' '));
  });
}

function testChat(client) {
  it('chat', async () => {
    const words = [
      'nobody',
      'ever',
      'reads',
      'test',
      'input',
      'especially',
      'this',
      'one',
    ];
    const result = await new Promise((resolve, reject) => {
      const result = [];
      const stream = client.chat();
      stream.on('data', response => {
        result.push(response.content);
      });
      stream.on('end', () => {
        resolve(result);
      });
      stream.on('error', reject);
      for (const word of words) {
        stream.write({content: word});
      }
      stream.end();
    });
    assert.deepStrictEqual(result, words);
  });
}

function testWait(client) {
  it('wait', async function() {
    this.timeout(10000);
    const request = {
      ttl: {
        seconds: 5,
        nanos: 0,
      },
      success: {
        content: 'done',
      },
    };
    const [operation] = await client.wait(request);
    const [response] = await operation.promise();
    assert.deepStrictEqual(response.content, request.success.content);
  });
}

// Finally, using mocha!
describe('Showcase tests', () => {
  if (typeof window !== 'undefined') {
    describe('browser library works', () => {
      testShowcase({browser: true});
    });
  } else {
    describe('grpc-fallback works', () => {
      testShowcase({fallback: true});
    });
    describe('@grpc/grpc-js works', () => {
      testShowcase({grpcJs: true});
    });
    describe('grpc works', () => {
      testShowcase({grpc: true});
    });
  }
});

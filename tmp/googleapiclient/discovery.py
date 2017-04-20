# A mock Python client library for the Mock API.

def build(apiName, apiVersion, developerKey=None):
  assert apiName is 'mock'
  assert apiVersion is 'v1'
  assert developerKey is not None
  return Service()

class Service(object):

  def foo(self):
    return Foo()


class FooRequest(object):

  def execute(self):
    return 'Hello world!'


class Foo(object):

  def get(self, foo_id):
    assert foo_id == '0'
    return FooRequest()

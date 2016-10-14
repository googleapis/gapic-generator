"""
BEFORE RUNNING:
---------------
1. If not already done, enable the Mock API
   and check the quota for your project at
   https://console.developers.google.com/apis/api/mock
2. Install the Python client library for Google APIs by running
   `pip install --upgrade google-api-python-client`
"""
from pprint import pprint

from googleapiclient import discovery


# TODO: Change placeholder below to desired API key:
service = discovery.build('mock', 'v1', developerKey='{MY-API-KEY}')


# TODO: Change placeholder below to desired parameter value for the `get` method:

# The id of the foo you want.
foo_id = str(0L)


request = service.foo().get(foo_id=foo_id)
response = request.execute()

# TODO: Change code below to process the `response` dict:
pprint(response)

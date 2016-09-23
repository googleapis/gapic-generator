#!/usr/bin/env python

from json import loads
from glob import glob
from urllib import urlopen, urlretrieve

disco_url = dict(
    ((api['name'], api['version']), api['discoveryRestUrl'])
    for api in loads(urlopen("https://www.googleapis.com/discovery/v1/apis").read())['items'])

for disco_name in glob("*.json"):
    urlretrieve(disco_url[tuple(disco_name.rstrip(".json").split(".", 1))], disco_name)

#!/usr/bin/env python

# Copyright 2018 Google Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Gapic generator smoke tests.

Generate a client library in the given language and run its unit tests.
"""

import logging
import pytest

from smoketests.gapic_smoketest import run_smoke_test

def test_java_pubsub():
    print("tested java speeech")
    # caplog.set_level(logging.INFO)
    # run_smoke_test("speech",
    #                "java",
    #                "/tmp/workspace/googleapis",
    #                "../../artman_config.yaml")
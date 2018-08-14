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

It generates GAPIC client libraries for a Google APIs in googleapis
repo. The test will fail only if ALL generations fails.
"""

import argparse
import logging
import os
import subprocess
import sys

logger = logging.getLogger('smoketest')
logger.setLevel(logging.INFO)

languages = [
    "java",
    "python",
    # TODO: add other languages here.
]

test_apis = {
    "pubsub" : ("v1", "google/pubsub/artman_pubsub.yaml"),
    "logging" : ("v2", "google/logging/artman_logging.yaml"),
    "speech" : ("v1", "google/cloud/speech/artman_speech_v1.yaml"),
}


def generate_clients(root_dir, log, user_config):
    log_file = _setup_logger(log)
    failure = []
    success = []
    warning = []
    for language in languages:
        for api_name in test_apis:
            (api_version, artman_yaml_path) = test_apis[api_name]
            target = language + "_gapic"
            # Generate client library for an API and language.
            if _generate_artifact(artman_yaml_path,
                                  target,
                                  root_dir,
                                  log_file,
                                  user_config):
                msg = 'Failed to generate %s of %s.' % (
                    target, artman_yaml_path)
                failure.append(msg)
            else:
                msg = 'Succeded to generate %s of %s.' % (
                    target, artman_yaml_path)
                success.append(msg)
            logger.info(msg)
    logger.info('================ Library Generation Summary ================')
    if not warning or not failure:
        logger.info('Successes:')
        if warning:
            for msg in success:
                logger.info(msg)
        logger.info('Warnings:')
        if warning:
            for msg in warning:
                logger.info(msg)
        logger.info('Failures:')
        if failure:
            for msg in failure:
                logger.error(msg)
            sys.exit('Smoke test failed.')
    else:
        logger.info("All passed.")

    return success, failure


def _generate_artifact(artman_config, artifact_name, root_dir, log_file, user_config_file):
    with open(log_file, 'a') as log:
        grpc_pipeline_args = [
            'artman',
            '--local',
            '--verbose',
        ]
        if user_config_file:
            grpc_pipeline_args = grpc_pipeline_args + [
                '--user-config', user_config_file,]
        grpc_pipeline_args = grpc_pipeline_args + [
            '--config', os.path.join(root_dir, artman_config),
            '--root-dir', root_dir,
            'generate', artifact_name
        ]
        logger.info("Generate artifact %s of %s: %s" %
                    (artifact_name, artman_config, " ".join(grpc_pipeline_args)))
        return subprocess.call(grpc_pipeline_args, stdout=log, stderr=log)


def _test_artifact(test_call, api_name, api_version, log_file):
    with open(log_file, 'a') as log:
        return test_call(api_name, api_version, log)


def parse_args(*args):
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '--root-dir',
        # The default value is configured for CircleCI.
        default='/tmp/workspace/googleapis/',
        help='Specify where googleapis local repo lives.')
    parser.add_argument(
        '--log',
        # The default value is configured for CircleCI.
        default='/tmp/workspace/reports/smoketest.log',
        help='Specify where smoketest log should be stored.')
    parser.add_argument(
        '--user-config',
        # Default to the artman-specified default location.
        default="~/.artman/config.yaml",
        help='Specify where the artman user config lives.')
    return parser.parse_args(args=args)


def _setup_logger(log_file):
    """Setup logger with a logging FileHandler."""
    log_file_handler = logging.FileHandler(log_file, mode='a+')
    logger.addHandler(log_file_handler)
    logger.addHandler(logging.StreamHandler())
    return log_file


if __name__ == '__main__':
    flags = parse_args(*sys.argv[1:])

    root_dir = os.path.abspath(flags.root_dir)
    log = os.path.abspath(flags.log)
    user_config = os.path.abspath(os.path.expanduser(flags.user_config)) if flags.user_config else None

    (successes, failures) = generate_clients(root_dir, log, user_config)

    # Exit with success if there were any successful generations.
    exit_code = 0 if successes else 1
    sys.exit(exit_code)

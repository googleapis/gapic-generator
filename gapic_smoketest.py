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
repo. The test will fail if any generation fails.
"""

import argparse
import logging
import os
import subprocess
import sys
# TODO: requirements.txt for pip install deps


logger = logging.getLogger('smoketest')
logger.setLevel(logging.INFO)

test_languages = {
    "java" : lambda api_name, api_version, log : runJavaTests(api_name, api_version, log),
    #TODO: all other languages
}

test_apis = [
    ("pubsub", "v1", "google/pubsub/artman_pubsub.yaml"),
    ("logging", "v2", "google/logging/artman_logging.yaml"),
    ("speech", "v1", "google/cloud/speech/artman_speech_v1.yaml"),
]

def runJavaTests(api_name, api_version, log):
    gapic_dir = "gapic-google-cloud-%s-%s" % (api_name, api_version)
    # Run gradle test in the main java directory and also in the gapic directory.
    cmds = ("%s/gradlew build test" % os.getcwd()).split()
    exit_code = subprocess.call(cmds, stdout=log, stderr=log)
    if exit_code:
        return exit_code

    cmds = ("%s/gradlew -p %s build test" % (os.getcwd(), gapic_dir)).split()
    return subprocess.call(cmds, stdout=log, stderr=log)

def run_smoke_test(root_dir, log):
    log_file = _setup_logger(log)
    failure = []
    success = []
    warning = []
    for (api_name, api_version, artman_yaml_path) in test_apis:
        for language in test_languages:
            target = language + "_gapic"
            logger.info('Start artifact generation for %s of %s'
                        % (target, artman_yaml_path))
            if _generate_artifact(artman_yaml_path,
                                  target,
                                  root_dir,
                                  log_file):
                msg = 'Failed to generate %s of %s.' % (
                    target, artman_yaml_path)
                failure.append(msg)
                logger.info(msg)
            else:
                msg = 'Succeded to generate %s of %s.' % (
                    target, artman_yaml_path)
                success.append(msg)
                logger.info(msg)

                cwd = os.getcwd()
                os.chdir("artman-genfiles/%s" % language)

                if _test_artifact(test_languages[language], api_name, api_version, log_file):
                    msg = 'Failed to pass tests for %s library.' % (
                        language)
                    failure.append(msg)
                else:
                    msg = 'Succeeded to pass tests for %s library.' % (
                        language)
                    success.append(msg)
                os.chdir(cwd)
                logger.info(msg)
    logger.info('================ Smoketest summary ================')
    logger.info('Successes:')
    for msg in success:
        logger.info(msg)
    logger.info('Warnings:')
    if warning:
        for msg in warning:
            logger.info(msg)
    else:
        logger.info("none.")
    logger.info('Failures:')
    if failure:
        for msg in failure:
            logger.error(msg)
        sys.exit('Smoke test failed.')
    else:
        logger.info("none.")


def _generate_artifact(artman_config, artifact_name, root_dir, log_file):
    with open(log_file, 'a') as log:
        grpc_pipeline_args = [
            'artman',
            '--local',
            '--verbose',
            '--user-config', "artman_config.yaml",
            '--config', os.path.join(root_dir, artman_config),
            '--root-dir', root_dir,
            'generate', artifact_name
        ]
        return subprocess.call(grpc_pipeline_args, stdout=log, stderr=log)

def _test_artifact(test_call, api_name, api_version, log_file):
    with open(log_file, 'a') as log:
        return test_call(api_name, api_version, log)

def parse_args(*args):
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '--root-dir',
        default='/googleapis',
        type=str,
        help='Specify where googleapis local repo lives.')
    parser.add_argument(
        '--log',
        default='/tmp/smoketest.log',
        type=str,
        help='Specify where smoketest log should be stored.')
    return parser.parse_args(args=args)


def _setup_logger(log_file):
    """Setup logger with a logging FileHandler."""
    log_file_handler = logging.FileHandler(log_file, mode='a+')
    logger.addHandler(log_file_handler)
    logger.addHandler(logging.StreamHandler())
    return log_file


if __name__ == '__main__':
    flags = parse_args(*sys.argv[1:])

    run_smoke_test(os.path.abspath(flags.root_dir), os.path.abspath(flags.log))

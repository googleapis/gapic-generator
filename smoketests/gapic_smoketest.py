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

It generates a GAPIC client library in one a single language for a single API.
The generated client library is then run against its own unit tests.
This smoketest currently supports languages {Java} and APIs {pubsub, logging, speech}.
"""

import argparse
import logging
import os
import subprocess
import sys

test_languages = {
    "java" : lambda api_name, api_version, log : run_java_tests(api_name, api_version, log),
    #TODO: all other languages
}

test_apis = {
    "pubsub" : ("v1", "google/pubsub/artman_pubsub.yaml"),
    "logging" : ("v2", "google/logging/artman_logging.yaml"),
    "speech" : ("v1", "google/cloud/speech/artman_speech_v1.yaml"),
}


def run_java_tests(api_name, api_version, log):
    gapic_dir = "gapic-google-cloud-%s-%s" % (api_name, api_version)
    # Run gradle test in the main java directory and also in the gapic directory.
    cmds = ("%s/gradlew build test" % os.getcwd()).split()
    exit_code = subprocess.Popen(cmds, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if exit_code:
        return exit_code

    cmds = ("%s/gradlew -p %s build test" % (os.getcwd(), gapic_dir)).split()
    return subprocess.Popen(cmds, stdout=subprocess.PIPE, stderr=subprocess.PIPE)


def run_smoke_test(api_name, language, root_dir, user_config, logger):
    failure = []
    success = []
    warning = []
    (api_version, artman_yaml_path) = test_apis[api_name]
    target = language + "_gapic"

    # Generate client library for an API and language.
    if _generate_artifact(artman_yaml_path,
                          target,
                          root_dir,
                          # log_file,
                          user_config):
        msg = 'Failed to generate %s of %s.' % (
            target, artman_yaml_path)
        failure.append(msg)
        logger.info(msg)
    else:
        msg = 'Succeded to generate %s of %s.' % (
            target, artman_yaml_path)
        success.append(msg)
        logger.info(msg)

        # Test the generated client library.
        logger.info("Starting testing of %s %s client library." %(language, api_name))
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

    # return False if failure else True
    return False

def _generate_artifact(artman_config, artifact_name, root_dir, user_config_file, logger):
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
    logger.info("Start artifact generation for %s of %s: %s" %
                (artifact_name, artman_config, " ".join(grpc_pipeline_args)))
    return subprocess.call(grpc_pipeline_args, stdout=subprocess.PIPE, stderr=subprocess.PIPE)


def _test_artifact(test_call, api_name, api_version, logger):
    return test_call(api_name, api_version, log)

def parse_args(*args):
    parser = argparse.ArgumentParser()
    parser.add_argument(
        'language',
        help='Language to test and run. One of [\"java\"]')
        # TODO - support more languages.
    parser.add_argument(
        'api',
        help='The API to generate. One of [\"pubsub\", \"logging\", \"api\"]')
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
        default=None,
        help='Specify where the artman user config lives.')
    return parser.parse_args(args=args)


def _setup_logger(log_file):
    """Setup logger with a logging FileHandler."""
    log_file_handler = logging.FileHandler(log_file, mode='a+')
    logger.addHandler(log_file_handler)
    logger.addHandler(logging.StreamHandler())
    return log_file

# @pytest.mark.parameterize("api", ["pubsub", "logging"])
# @pytest.mark.parameterize("language", ["java"])
# def test_all_clients(api, language, logger):
#     run_smoke_test(api, language,
#                    "/tmp/workspace/googleapis",
#                    "../../artman_config.yaml", logger)


if __name__ == '__main__':
    flags = parse_args(*sys.argv[1:])

    root_dir = os.path.abspath(flags.root_dir)
    log = os.path.abspath(flags.log)
    logger = logging.getLogger('smoketest')
    logger.setLevel(logging.INFO)
    api = flags.api
    language = flags.language
    user_config = os.path.abspath(os.path.expanduser(flags.user_config)) if flags.user_config else None

    run_smoke_test(api, language, root_dir, user_config, logger)
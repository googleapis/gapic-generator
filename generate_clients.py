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

It generates GAPIC client libraries for a set of Google APIs in googleapis
repo. The test will fail only if ALL generations fails.

For each language, if the ALL API client generations for that language passes,
then this script outputs a file called [language].log in the given log_dir.
"""

import argparse
import logging
import os
import subprocess
import sys

default_languages = [
    "java",
    "python",
    "ruby",
    "go",
    "php",
    "csharp"
    # TODO: add other languages here.
]

logger = logging.getLogger('smoketest')
logger.setLevel(logging.INFO)


def generate_clients(root_dir, languages, artman_config, log_dir, user_config):
    log = os.path.join(log_dir, "smoketest.log")
    log_file = _setup_logger(log)
    failure = []
    success = []
    warning = []
    for language in languages:
        lang_success = True
        target = language + "_gapic"
        # Generate client library for an API and language.
        if _generate_artifact(artman_config,
                              target,
                              root_dir,
                              log_file,
                              user_config):
            msg = 'Failed to generate %s of %s.' % (
                target, artman_config)
            failure.append(msg)
            lang_success = False
        else:
            msg = 'Succeded to generate %s of %s.' % (
                target, artman_config)
            success.append(msg)
        logger.info(msg)
    if not lang_success:
        # Output the [language.log] file.
        write_lang_error_log(language, log_dir)

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
    else:
        logger.info("All passed.")

    return success


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


def write_lang_error_log(language, log_dir):
    file_name = "%s.log" % language
    filepath = os.path.join(log_dir, file_name)
    with open(filepath,"w") as f:
        f.write("Error.")


def parse_args(*args):
    parser = argparse.ArgumentParser()
    parser.add_argument('artman_config', help='The artman config relative to the root directory.')
    parser.add_argument(
        '--root-dir',
        # The default value is configured for CircleCI.
        default='/tmp/workspace/googleapis/',
        help='Specify where googleapis local repo lives.')
    parser.add_argument(
        '--log-dir',
        # The default value is configured for CircleCI.
        default='/tmp/workspace/reports/',
        required=True,
        help='Specify where smoketest log and language success logs should be stored. ' +
             'The contents of this directory will be removed prior to execution.')
    parser.add_argument(
        '--user-config',
        # Default to the artman-specified default location.
        default="~/.artman/config.yaml",
        help='Specify where the artman user config lives.')
    parser.add_argument(
        '--languages',
        nargs='+',
        default=default_languages,
        help='Languages to generate clients for.')
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
    log_dir = os.path.abspath(flags.log_dir)
    user_config = os.path.abspath(os.path.expanduser(flags.user_config)) if flags.user_config else None

    # Clear log directory.
    for f in os.listdir(log_dir):
        os.remove(os.path.join(log_dir, f))

    successes = generate_clients(root_dir, flags.languages, flags.artman_config, log_dir, user_config)

    # Exit with success if there were any successful generations.
    if successes:
        sys.exit(0)
    else:
        sys.exit('Client generation failed.')

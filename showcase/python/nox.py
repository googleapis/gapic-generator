# Copyright 2018 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from __future__ import absolute_import
import os

import nox


@nox.session
def default(session):
    return showcase(session, 'default')


@nox.session
@nox.parametrize('py', ['2.7', '3.5', '3.6', '3.7'])
def showcase(session, py):
    """Run the showcase integration test suite."""

    # Run unit tests against all supported versions of Python.
    if py != 'default':
        session.interpreter = 'python{}'.format(py)

    # Set the virtualenv directory name.
    session.virtualenv_dirname = 'showcase-' + py

    # Install all test dependencies, then install this package in-place.
    session.install('pytest')
    session.install('--pre', 'googleapis-common-protos')
    session.install(
        '-e',
        os.path.join('..', '..', 'artman-genfiles', 'python',
                     'showcase-v1alpha2'))

    # Run py.test against the unit tests.
    session.run('py.test', '--quiet', 'tests')


@nox.session
@nox.parametrize('py', ['2.7', '3.5', '3.6', '3.7'])
def unit(session, py):
    """Run the unit test suite."""

    # Run unit tests against all supported versions of Python.
    if py != 'default':
        session.interpreter = 'python{}'.format(py)

    # Set the virtualenv directory name.
    session.virtualenv_dirname = 'unit-' + py

    # Install all test dependencies, then install this package in-place.
    session.install('pytest', 'mock')
    session.install('--pre', 'googleapis-common-protos')
    session.install(
        '-e',
        os.path.join('..', '..', 'artman-genfiles', 'python',
                     'showcase-v1alpha2'))

    # Run py.test against the unit tests.
    session.run(
        'py.test', '--quiet',
        os.path.join('..', '..', 'artman-genfiles', 'python',
                     'showcase-v1alpha2', 'tests', 'unit'))

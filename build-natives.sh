#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# Copyright 2015-2024 Google Inc.
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

set -euox pipefail

mkdir -p core/target/
docker build -t google-java-format-native --rm .
id=$(docker create google-java-format-native)
docker cp "$id":/build/core/target/google-java-format core/target/google-java-format_linux-x86_64
docker rm -v "$id"

# The following isn't just to "look nice" in log, but *required* to TEST it! (Script & build will fail if NOK.)
status=-1
chmod +x core/target/google-java-format_linux-x86_64
if time core/target/google-java-format_linux-x86_64; then
 status=0
else
 status=$?
fi
if [[ $status -ne 2 ]]; then
  echo "google-java-format_linux-x86_64 without arguments should have printed usage help and exited with 2, but did not :("
  exit 1
fi

# TODO This only builds a Linux ABI Native Binary so far - later also build a Win/Mac one... (but how?)

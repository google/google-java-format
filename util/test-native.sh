#!/usr/bin/env bash
# Copyright 2024 The Google Java Format Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -euox pipefail

time java -jar core/target/google-java-format-*-all-deps.jar || true

status=-1
chmod +x core/target/google-java-format
if time core/target/google-java-format; then
 status=0
else
 status=$?
fi
if [[ $status -ne 2 ]]; then
  echo "google-java-format_linux (native) without arguments should have printed usage help and exited with 2, but did not :("
  exit 1
fi

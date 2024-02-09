#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# Copyright 2015-2024 The Google Java Format Authors
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

FROM ghcr.io/graalvm/native-image-community:21

# TODO Use RUN --mount=type=bind,source= instead
WORKDIR /build/
ADD . /build/

RUN --mount=type=cache,target=/root/.m2/ ./mvnw -Pnative -DskipTests package -pl core -am

RUN time java -jar core/target/google-java-format-*-all-deps.jar || true

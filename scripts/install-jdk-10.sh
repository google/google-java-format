#!/usr/bin/env bash
set -e

# Adapted from https://github.com/sormuras/bach/blob/75a6719e5a42b3cde9b84f82e89cfb1c53abbe96/install-jdk.sh

JDK_FEATURE=10

TMP=$(curl -L jdk.java.net/${JDK_FEATURE})
TMP="${TMP#*Most recent build: jdk-${JDK_FEATURE}-ea+}" # remove everything before the number
TMP="${TMP%%<*}"                                        # remove everything after the number
JDK_BUILD="$(echo -e "${TMP}" | tr -d '[:space:]')"     # remove all whitespace

JDK_ARCHIVE=jdk-${JDK_FEATURE}-ea+${JDK_BUILD}_linux-x64_bin.tar.gz

cd ~
wget http://download.java.net/java/jdk${JDK_FEATURE}/archive/${JDK_BUILD}/BCL/${JDK_ARCHIVE}
tar -xzf ${JDK_ARCHIVE}
export JAVA_HOME=~/jdk-${JDK_FEATURE}
export PATH=${JAVA_HOME}/bin:$PATH
cd -

java --version

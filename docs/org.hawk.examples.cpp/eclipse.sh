#!/bin/bash

SRC_DIR="$(dirname "$(readlink -f "$0")")"
BUILD_DIR=../org.hawk.examples.cpp.eclipse

rm -rf "$BUILD_DIR"
mkdir "$BUILD_DIR"
cd "$BUILD_DIR"
cmake -G"Eclipse CDT4 - Unix Makefiles" -D CMAKE_BUILD_TYPE=Debug "$SRC_DIR"

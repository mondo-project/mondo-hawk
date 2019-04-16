#!/bin/bash

rm -rf src-gen src-gen-js src-gen-cpp
mkdir src-gen src-gen-js src-gen-cpp

thrift -out src-gen --gen java src/api.thrift
thrift -out src-gen-js --gen js src/api.thrift
thrift -out src-gen-cpp --gen cpp src/api.thrift

pushd src-gen-cpp
ln -s ../CMakeLists.txt
popd

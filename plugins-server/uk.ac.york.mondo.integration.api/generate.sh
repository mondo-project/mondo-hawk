#!/bin/bash

rm -rf src-gen src-gen-js
mkdir src-gen src-gen-js
thrift -out src-gen --gen java src/api.thrift
thrift -out src-gen-js --gen js src/api.thrift

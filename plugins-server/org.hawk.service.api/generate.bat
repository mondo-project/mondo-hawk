SETLOCAL

REM Make sure the PATH environment variable includes the Thrift compiler from the right source

SET PATH=%PATH%;C:\Eclipse\eclipse-SDK-4.4.1-win32-x86_64-MONDO\thrift-0.9.2

CD %~dp0

RMDIR /Q /S src-gen

MKDIR src-gen

thrift -out src-gen --gen java src/api.thrift

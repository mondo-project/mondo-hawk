cmake_minimum_required (VERSION 3.10)

project(HawkStub)

file(GLOB SOURCES RELATIVE ${CMAKE_CURRENT_SOURCE_DIR} *.cpp *.h)
add_library(hawkclient STATIC ${SOURCES})

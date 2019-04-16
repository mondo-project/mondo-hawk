cmake_minimum_required (VERSION 3.10)

project(HawkCPPExample)

find_package(Boost 1.53.0 REQUIRED)
include_directories(SYSTEM "${Boost_INCLUDE_DIRS}")

add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../../plugins-server/org.hawk.service.api/src-gen-cpp hawkclient-build)

file(GLOB EXAMPLE_SOURCES RELATIVE ${CMAKE_SOURCE_DIR} *.cpp)
add_executable(example ${EXAMPLE_SOURCES})
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../../plugins-server/org.hawk.service.api/src-gen-cpp)

target_link_libraries(example thrift hawkclient)

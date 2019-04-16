# C++ client example

This is a simple example application that uses the Apache Thrift C++ libraries and the C++ stubs generated from the Thrift IDL file produced by Ecore2Thrift [1]. It uses the Thrift Compact protocol over HTTP. The application lists all the available instances in a server, and it is used as follows (brackets indicate optional arguments):

    ./example [host] [port]

## Dependencies

To compile this application, you will need the Apache Thrift 0.9.3 C++ runtime library. Since Thrift is not yet at 1.0, headers may change quite a bit, so use exactly that version if you have issues.

If you need to compile it from source in a Debian-based system, you could follow [the official instructions](https://thrift.apache.org/docs/install/debian). If you run into an error message about "libcrypto required", you may need to install `libssl1.0-dev` ([source](https://stackoverflow.com/questions/9123457/configure-thrift-libcrypto-required)). You may want to disable Ruby, as one of the test cases for Ruby fails, by using:

    ./configure --without-ruby

After configuring, you will have to follow the usual `make` and `sudo make install` steps. You will want to run `sudo ldconfig` to make sure the library is in the shared library search path.

The project depends on the Hawk C++ stubs in the `plugins-server/org.hawk.service.api/src-gen-cpp` folder. The `CMakeLists.txt` file refers to this library using a relative path with the `add_subdirectory` CMake command.

## How to build

You can use `./build.sh` to build the application inside the `build` folder, or use `./eclipse.sh` to generate an Eclipse CDT4 project in the `../org.hawk.examples.cpp.eclipse` folder. CMake recommends that you use a sibling folder rather than a subfolder for generated Eclipse projects.

[1]: https://github.com/bluezio/ecore2thrift

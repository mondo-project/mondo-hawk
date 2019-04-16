#include <iostream>

#include <thrift/protocol/TCompactProtocol.h>
#include <thrift/transport/TSocket.h>
#include <thrift/transport/THttpClient.h>
#include <thrift/transport/TTransportUtils.h>

#include "Hawk.h"

using namespace std;
using namespace boost;
using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;

namespace {
	const string DEFAULT_HOST = "localhost";
	const int DEFAULT_PORT = 8080;
}

int main(int argc, char* argv[]) {
	const string host = argc > 1 ? argv[1] : DEFAULT_HOST;
	const int port = argc > 2 ? atoi(argv[2]) : DEFAULT_PORT;

	boost::shared_ptr<TTransport> socket(new TSocket(host, port));
	boost::shared_ptr<TTransport> transport(new THttpClient(socket, host, "/thrift/hawk/compact"));
	boost::shared_ptr<TProtocol> protocol(new TCompactProtocol(transport));
	HawkClient client(protocol);

	try {
		transport->open();

		vector<HawkInstance> instances;
		client.listInstances(instances);

		if (instances.empty()) {
			cout << "No instances in the specified server";
		} else {
			cout << "Found " << instances.size() << " in the server" << endl;
			for (auto it = instances.begin(); it != instances.end(); ++it) {
				cout << "* " << it->name << endl;
			}
		}

		transport->close();
	} catch (TException& tex) {
		cerr << "ERROR: " << tex.what() << endl;
		return EXIT_FAILURE;
	}

	return EXIT_SUCCESS;
}

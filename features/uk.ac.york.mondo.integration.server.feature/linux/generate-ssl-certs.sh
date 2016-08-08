#!/bin/bash

# NOTE: replace key/trust store passwords with something better on production,
# and replace CN with your server's hostname!
#
# On the client .ini, you'll need to set:
#
# -Djavax.net.ssl.trustStore=path/to/mondo-client-truststore.jks
# -Djavax.net.ssl.trustStorePassword=secureexample
#
# On the server .ini, you'll need to enable SSL and tell Jetty and Artemis about your KeyStore:
#
# -Dorg.eclipse.equinox.http.jetty.https.enabled=true
# -Dhawk.artemis.sslEnabled=true
# -Dorg.eclipse.equinox.http.jetty.ssl.keystore=path/to/mondo-server-keystore.jks
# -Djavax.net.ssl.keyStore=path/to/mondo-server-keystore.jks
#
# You'll be prompted for the key store password three times: two by Jetty and
# once by the Artemis server. If you don't want these prompts, you could use these
# properties, but using them is *UNSAFE*, as another user in the same machine could
# retrieve these passwords from your process manager:
#
# -Djavax.net.ssl.keyStorePassword=secureexample
# -Dorg.eclipse.equinox.http.jetty.ssl.keypassword=secureexample
# -Dorg.eclipse.equinox.http.jetty.ssl.password=secureexample

keytool -genkey -keystore mondo-server-keystore.jks -storepass secureexample -keypass secureexample -dname "CN=localhost, OU=Artemis, O=ActiveMQ, L=AMQ, S=AMQ, C=AMQ" -keyalg RSA
keytool -export -keystore mondo-server-keystore.jks -file mondo-jks.cer -storepass secureexample
keytool -import -keystore mondo-client-truststore.jks -file mondo-jks.cer -storepass secureexample -keypass secureexample -noprompt


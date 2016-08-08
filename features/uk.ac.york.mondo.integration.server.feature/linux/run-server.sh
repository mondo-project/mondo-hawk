#!/bin/bash

INI=mondo-server.ini
KEYRING=mondo.keyring
PASSWORD=mondo.pass

# Length of the random password file, in bytes (before base64 encoding)
PASSLENGTH=100

if ! test -f "$KEYRING"; then
  touch "$KEYRING"
  touch "$PASSWORD"
  chmod 600 "$KEYRING" "$PASSWORD"
  head -c "$PASSLENGTH" /dev/random | base64 > mondo.pass
  chmod 400 "$PASSWORD"
  sed -i -e "/[-]vmargs/i -eclipse.keyring\n$KEYRING\n-eclipse.password\n$PASSWORD" "$INI"
fi

./mondo-server

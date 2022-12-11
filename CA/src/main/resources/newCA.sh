#!/bin/sh
# Generates a New Private Key
openssl genrsa -out CA.key 4096
# Generates a New Self Signing Certificate
openssl req -new -x509 -key CA.key -sha256 -subj "/C=PT/O=IST/OU=IST/CN=localhost" -days 365 -out CA.crt

openssl pkcs12 -export -inkey CA.key \
        -in CA.crt -name CACert -out CA.p12 -passout "pass:CA"
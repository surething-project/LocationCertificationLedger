#!/bin/sh

if [ "$#" -ne 1 ]; then
    echo "Common Name Missing!"
    exit 1
fi

cd ../$1/src/main/resources
# Creates a KeyStore with the PrivateKey and Public Key
openssl pkcs12 -export -inkey $1.key \
        -in $1.crt -name $1Cert -out $1.p12 -passout "pass:$1"
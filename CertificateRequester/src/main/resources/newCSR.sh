#!/bin/sh

if [ "$#" -ne 1 ]; then
    echo "Common Name Missing!"
    exit 1
fi
src_path=../$1/src/main/resources/

cd ${src_path}

# Generates a New Private Key
openssl genrsa -out $1.key 4096

# Generates a New CSR
openssl req -new -key $1.key -subj "/C=PT/O=IST/OU=IST/CN=localhost" -out $1.csr

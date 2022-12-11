#!/bin/sh
if [ "$#" -ne 2 ]; then
    echo "Common Name Missing!"
    exit 1
fi

cd src/main/resources/
# CA Signs a CSR
openssl x509 -req -days 500 -in $1 -CA CA.crt -CAkey CA.key -CAcreateserial -out $2 -sha256
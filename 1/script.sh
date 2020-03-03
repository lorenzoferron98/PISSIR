#!/bin/bash

set -e

if [ $# -ne 1 ]; then
  echo "$(basename "$0") file" >&2
  exit 1
fi

openssl req -newkey rsa:2048 -nodes -keyout 'mykey.pem' -x509 -days 365 -out 'certificate.pem' -verbose
openssl rsa -in 'mykey.pem' -out 'pubkey.pem' -pubout -modulus
openssl dgst -sha256 -out "$1.sign" -sign 'mykey.pem' "$1"
openssl base64 -in "$1.sign" -out "$1.sign.base64" -e -v -p
# check
openssl base64 -out 'rec.sign' -in "$1.sign.base64" -d -v -p
openssl x509 -pubkey -noout -in 'certificate.pem' -out 'rec.pubkey.pem'
openssl dgst -sha256 -verify 'rec.pubkey.pem' -signature 'rec.sign' "$1"
# clean all
rm -rf 'rec.sign' 'rec.pubkey.pem'
# zip
zip -r 'consegna1.zip' "$1.sign.base64" 'certificate.pem' 'README.txt' "$1" -v
# send mail
exit 0

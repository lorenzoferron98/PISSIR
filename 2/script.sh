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
openssl aes-128-cbc -in "$1" -out "$1.cif.base64" -e -p -v -k 'Q.`[)mx+=U' -base64
echo 'Q.`[)mx+=U' >key.txt
# openssl rsautl -in 'key.txt' -out 'key.txt.cif' -inkey 'pubkey.pem' -pubin -encrypt
openssl rsautl -in 'key.txt' -out 'key.txt.cif' -inkey 'giordanapub.pem' -pubin -encrypt
openssl base64 -in 'key.txt.cif' -out 'key.txt.cif.base64' -e -v -p
# check
openssl base64 -out 'rec_key.txt.cif' -in 'key.txt.cif.base64' -d -v -p
# openssl rsautl -in 'rec_key.txt.cif' -out 'rec_key.txt' -inkey 'mykey.pem' -decrypt
# openssl aes-128-cbc -out 'rec.txt' -in "$1.cif.base64" -d -p -v -k 'Q.`[)mx+=U' -base64
openssl base64 -out 'rec.sign' -in "$1.sign.base64" -d -v -p
openssl x509 -pubkey -noout -in 'certificate.pem' -out 'rec.pubkey.pem'
# openssl dgst -sha256 -verify 'rec.pubkey.pem' -signature 'rec.sign' rec.txt
## clean all
rm -rf 'rec.sign' 'rec.pubkey.pem' 'rec.txt' 'rec_key.txt.cif' 'rec_key.txt'
## zip
zip -r 'consegna2.zip' "$1.sign.base64" 'certificate.pem' 'key.txt.cif.base64' "$1.cif.base64" 'README.txt' -v
## send mail
exit 0

#!/usr/bin/env bash
set -euo pipefail

mkdir -p keystore

keytool -genkeypair \
  -v \
  -keystore keystore/meuponto-release.jks \
  -alias meuponto \
  -keyalg RSA \
  -keysize 4096 \
  -validity 36500 \
  -dname "CN=MEU PONTO, OU=Desenvolvimento, O=Bichocutela, L=Natal, ST=Rio Grande do Norte, C=BR"

echo
printf '%s\n' 'Keystore criado em: keystore/meuponto-release.jks'
printf '%s\n' 'Guarde o arquivo e as senhas em local seguro. Nunca envie o .jks ao GitHub.'

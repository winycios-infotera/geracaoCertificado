#!/bin/bash

# Parâmetros esperados
CLIENTE=$1
CN=$2
CS=$3
SENHA=$4

if [ -z "$CLIENTE" ] || [ -z "$CN" ] || [ -z "$CS" ] || [ -z "$SENHA" ]; then
  echo "⚠️  Uso: $0 <cliente> <client_id> <Client_secret> <sua_senha>"
  exit 1
fi

if [ ! -f "$CLIENTE.pfx" ]; then
  echo "❌ Arquivo \"$CLIENTE.pfx\" não encontrado."
  exit 1
fi

# Detecta a pasta "Documentos" no idioma atual do sistema
DOC_DIR=$(xdg-user-dir DOCUMENTS)

# Verifica se o diretório existe
if [ ! -d "$DOC_DIR" ]; then
  echo "❌ Pasta de Documentos não encontrada. Verifique se o xdg-user-dir está instalado corretamente."
  exit 1
fi

# CAP 1 -- GERAÇÃO DE CERTIFICADOS

# Etapa 1: PFX para KEY
openssl pkcs12 -in "$CLIENTE.pfx" -nocerts -out "$DOC_DIR/$CLIENTE.key" -nodes -passin pass:$SENHA
echo "[1/3] ✅ KEY gerado: $CLIENTE.key"

# Etapa 2: PFX para CRT
openssl pkcs12 -in "$CLIENTE.pfx" -clcerts -nokeys -out "$DOC_DIR/$CLIENTE.crt" -passin pass:$SENHA
echo "[2/3] ✅ CRT gerado: $CLIENTE.crt"

# Etapa 3: CRT + KEY para CSR
openssl req -new -key "$DOC_DIR/$CLIENTE.key" -out "$DOC_DIR/$CLIENTE.csr" \
  -subj "/C=BR/ST=SP/L=Sao Paulo/O=${CLIENTE^^}/OU=IT/CN=$CN"
echo "[3/3] ✅ CSR gerado: $CLIENTE.csr"

# CAP 2 - GERAÇÃO TOKEN

chmod 644 "$DOC_DIR/$CLIENTE.crt"
chmod 644 "$DOC_DIR/$CLIENTE.key"

echo "🌐 Realizando requisição para gerar access_token..."

RESPONSE=$(curl --location 'https://sts.itau.com.br/api/oauth/token' \
  --cert "$DOC_DIR/$CLIENTE.crt" \
  --key "$DOC_DIR/$CLIENTE.key" \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'grant_type=client_credentials' \
  --data-urlencode "client_id=$CN" \
  --data-urlencode "client_secret=$CS")

# Verifica se veio um access_token
ACCESS_TOKEN=$(echo "$RESPONSE" | grep -oP '"access_token"\s*:\s*"\K[^"]+')

if [ -n "$ACCESS_TOKEN" ]; then
  echo "✅ Access Token gerado com sucesso:"
  echo "$ACCESS_TOKEN"
else
  echo "❌ Falha ao obter o Access Token. Resposta da API:"
  echo "$RESPONSE"
fi

# CAP 3 - LIMPEZA DOS ARQUIVOS
echo "🧹 Limpando arquivos temporários..."
rm -f "$DOC_DIR/$CLIENTE.key" "$DOC_DIR/$CLIENTE.crt" "$DOC_DIR/$CLIENTE.csr"

echo "✅ Tudo pronto!"


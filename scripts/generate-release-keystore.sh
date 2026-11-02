#!/usr/bin/env bash
set -e

KEYSTORE_DIR="keystore"
KEYSTORE_FILE="$KEYSTORE_DIR/airhockey-release.jks"
KEY_ALIAS="airhockey"
LOCAL_PROPS="local.properties"

mkdir -p "$KEYSTORE_DIR"

if [ -f "$KEYSTORE_FILE" ]; then
    echo "Keystore já existe em $KEYSTORE_FILE."
    exit 0
fi

# Password: use argument if provided, or generate a secure alphanumeric password
PASSWORD="${1:-$(openssl rand -base64 12 | tr -dc 'a-zA-Z0-9' | head -c 16)2026!}"

echo "================================================="
echo "  Gerando Release Keystore para Air Hockey"
echo "  Arquivo: $KEYSTORE_FILE"
echo "  Alias:   $KEY_ALIAS"
echo "================================================="

keytool -genkeypair -v \
  -keystore "$KEYSTORE_FILE" \
  -alias "$KEY_ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass "$PASSWORD" \
  -keypass "$PASSWORD" \
  -dname "CN=Vinicius Mellos, OU=VMellos Dev, O=VMellos Dev, L=Porto Alegre, ST=RS, C=BR"

# Update local.properties safely
# Remove existing keystore properties if any
sed -i '/^KEYSTORE_/d' "$LOCAL_PROPS" 2>/dev/null || true
sed -i '/^KEY_/d' "$LOCAL_PROPS" 2>/dev/null || true

cat <<EOF >> "$LOCAL_PROPS"

# Release Signing Configuration
KEYSTORE_PATH=$KEYSTORE_FILE
KEYSTORE_PASSWORD=$PASSWORD
KEY_ALIAS=$KEY_ALIAS
KEY_PASSWORD=$PASSWORD
EOF

echo ""
echo "================================================="
echo "  Keystore gerada com sucesso!"
echo "  Credenciais salvas em $LOCAL_PROPS"
echo "  (Certifique-se de que keystore/ e local.properties estão no .gitignore)"
echo "================================================="

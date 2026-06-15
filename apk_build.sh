#!/bin/sh

eval STORE_FILE=$(grep "custom.signing.store.file" local.properties | cut -d'=' -f2)

printf "Enter Keystore Password: "
read -s pw
echo ""

./gradlew assembleRelease \
          -Pandroid.injected.signing.store.file="$STORE_FILE" \
          -Pandroid.injected.signing.store.password="$pw" \
          -Pandroid.injected.signing.key.alias="releasekey" \
          -Pandroid.injected.signing.key.password="$pw"

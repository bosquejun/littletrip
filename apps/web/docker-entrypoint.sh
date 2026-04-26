#!/bin/sh
set -e

if [ -f /run/secrets/api_key ]; then
  export API_KEY=$(cat /run/secrets/api_key)
fi

if [ -f /run/secrets/admin_username ]; then
  export ADMIN_USERNAME=$(cat /run/secrets/admin_username)
fi

if [ -f /run/secrets/admin_password ]; then
  export ADMIN_PASSWORD=$(cat /run/secrets/admin_password)
fi

exec node apps/web/server.js

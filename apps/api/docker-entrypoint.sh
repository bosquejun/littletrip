#!/bin/sh
set -e

if [ -f /run/secrets/db_password ]; then
  export SPRING_DATASOURCE_PASSWORD=$(cat /run/secrets/db_password)
fi

if [ -f /run/secrets/api_key ]; then
  export API_KEY=$(cat /run/secrets/api_key)
fi

exec java -jar app.jar

#!/bin/bash

set -e
set -u

if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
	echo "Multiple database creation requested: $POSTGRES_MULTIPLE_DATABASES"
	for db in $(echo $POSTGRES_MULTIPLE_DATABASES | tr ',' ' '); do
            psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
	        CREATE DATABASE $db;
EOSQL
	done
	echo "Multiple databases created"
fi

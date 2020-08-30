#!/bin/bash

pushd /liquibase > /dev/null

find ./changelog -type f | xargs -d "\n" -I "{}" -n 1 ./liquibase --url="mongodb://mongo:27017/liquibase_cli" --logLevel info --changeLogFile '{}' update



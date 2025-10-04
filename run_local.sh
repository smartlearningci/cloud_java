#!/usr/bin/env bash
set -e
cd tasks-service
# usa Maven local; pode trocar por mvnw se preferir adicionar o wrapper
mvn spring-boot:run

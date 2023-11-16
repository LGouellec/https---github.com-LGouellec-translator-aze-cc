#!/bin/bash

docker-compose --env-file ./env -f docker-compose.yml down -v

docker rm -f $(docker ps -aq) 2>/dev/null
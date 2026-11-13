#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
export ORRERY_VERSION=${ORRERY_VERSION:-quickstart}
cd $DIR && docker compose pull && docker compose -p orrery up

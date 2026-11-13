#!/bin/bash

# read -p "Desired orrery version, for example 'v0.10.1' (defaults to newest): " VERSION
VERSION=${ORRERY_VERSION:-quickstart}
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
IMAGE=acryldata/orrery-upgrade:$ORRERY_VERSION
cd $DIR && docker pull ${IMAGE} && docker run --env-file ./env/docker.env --network="orrery_network" ${IMAGE} "$@"

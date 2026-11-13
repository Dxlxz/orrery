#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd $DIR && ../orrery-upgrade.sh -u NoCodeDataMigration -a batchSize=1000 -a batchDelayMs=100

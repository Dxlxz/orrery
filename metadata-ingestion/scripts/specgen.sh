#!/bin/bash
set -euo pipefail

# Note: this assumes that orrery has already been built with `./gradlew build`.
ORRERY_ROOT=..
SPECS_OUT_DIR=$ORRERY_ROOT/docs/generated/specs

rm -r $SPECS_OUT_DIR || true
python scripts/specgen.py --out-dir ${SPECS_OUT_DIR} $@

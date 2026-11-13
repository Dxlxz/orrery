#!/bin/bash
set -euo pipefail

OUTDIR=./src/orrery/metadata

# Note: this assumes that orrery has already been built with `./gradlew build`.
ORRERY_ROOT=..

SCHEMAS_PDL="$ORRERY_ROOT/metadata-models/src/main/pegasus/com/linkedin"
SCHEMAS_AVSC="$ORRERY_ROOT/metadata-events/mxe-schemas/src/renamed/avro/com/linkedin"
ENTITY_REGISTRY="$ORRERY_ROOT/metadata-models/src/main/resources/entity-registry.yml"

rm -r $OUTDIR 2>/dev/null || true
python scripts/avro_codegen.py $ENTITY_REGISTRY $SCHEMAS_PDL $SCHEMAS_AVSC $OUTDIR

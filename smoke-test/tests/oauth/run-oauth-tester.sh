#!/bin/bash
# Runs inside the `tester` container (mounted at /smoke). Invoked by run-oauth-ci.sh.
#
# The SDK's CLIENT-SIDE CODE under test comes from the triggering ref, so this
# canary gates client auth changes — env-based ORRERY_AUTH_TYPE resolution,
# the sink env-merge, precedence semantics — not just the released surface.
# GMS still runs the pinned release images (its OAuth validation is shipped).
#
# Mechanism: a plain `pip install /repo/metadata-ingestion` cannot work here —
# the mount is read-only (setuptools writes egg-info into the source tree) and,
# more fundamentally, the generated orrery/metadata schema classes are
# gitignored and absent from a fresh CI checkout. Instead, install the released
# SDK (which ships the generated classes and all dependencies), then OVERLAY
# the repo's pure-Python source on top — never touching orrery/metadata.
# Limits: a ref that adds dependencies, entry points, or changes model codegen
# is not covered by the overlay; that would need a full codegen build.
#
# Pass/fail is pytest's exit code alone (set -e + pipefail below). No skip may
# slip through as green — pytest exits 0 on an all-skipped run — so the two skip
# triggers are guarded upstream instead: the SDK auth layer by the import check
# below, the GMS/Keycloak env by the precondition just above the pytest call.
set -euo pipefail

# CLI_VERSION is the canonical floor from gradle/versioning/cliVersion.gradle
# (passed in by run-oauth-ci.sh). Install the NEWEST release at/above it (>=, not
# ==): the overlay below replaces the pure-Python source but NOT orrery/metadata,
# so the installed base supplies the generated schema_classes — which must be new
# enough for the overlaid source's imports. Pinning the base floor (==1.6.0) fails
# with ImportError on classes added after it (e.g. DomainAssociationClass).
pip install --quiet "orrery>=${CLI_VERSION}" pytest requests
SITE_ORRERY=$(python -c "import orrery, os; print(os.path.dirname(orrery.__file__))")
tar -C /repo/metadata-ingestion/src/orrery --exclude="./metadata" --exclude="*__pycache__*" -cf - . | tar -C "$SITE_ORRERY" -xf -
python -c "from orrery.ingestion.auth.env import build_auth_config_from_env"  # overlay sanity check
# Assert the tester's OAuth env (set on the compose service) so a missing var
# fails loudly here instead of skipping the whole module — an all-skipped run
# exits 0 and would read as green.
: "${ORRERY_GMS_URL:?}"
: "${KEYCLOAK_TOKEN_ENDPOINT:?}"
pytest /smoke/test_oauth_cli_gms.py -v

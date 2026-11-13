#!/usr/bin/bash

if [ -n "$ACTIONS_EXTRA_PACKAGES" ]; then
  uv pip install $ACTIONS_EXTRA_PACKAGES
fi

if [[ -n "$ACTIONS_CONFIG" && -n "$ACTIONS_EXTRA_PACKAGES" ]]; then
  mkdir -p /tmp/orrery/logs
  curl -q "$ACTIONS_CONFIG" -o config.yaml
  source /usr/local/lib/orrery/wait_for_deps.sh
  orrery_wait_begin
  orrery_wait_http "${ORRERY_GMS_PROTOCOL:-http}://${ORRERY_GMS_HOST}:${ORRERY_GMS_PORT}/health" ""
  exec orrery actions --config config.yaml
else
  exec orrery $@
fi

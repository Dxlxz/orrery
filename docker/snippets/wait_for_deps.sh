#!/usr/bin/env bash
# Dependency waits for Orrery container entrypoints (replaces dockerize).
# shellcheck disable=SC1091
# Usage: source this file, call orrery_wait_begin once, then orrery_wait_* as needed.

: "${ORRERY_WAIT_TIMEOUT_SECONDS:=240}"

orrery_wait_begin() {
  export ORRERY_WAIT_DEADLINE=$(( $(date +%s) + ORRERY_WAIT_TIMEOUT_SECONDS ))
}

_orrery_wait_expired() {
  (( $(date +%s) >= ORRERY_WAIT_DEADLINE ))
}

orrery_wait_tcp() {
  local raw="$1"
  local spec="${raw#tcp://}"
  local host="${spec%%:*}"
  local port="${spec##*:}"
  echo "[orrery-wait] TCP ${host}:${port}"
  while ! _orrery_wait_expired; do
    if timeout 1 bash -c "echo >/dev/tcp/${host}/${port}" 2>/dev/null; then
      echo "[orrery-wait] TCP ${host}:${port} ready"
      return 0
    fi
    sleep 1
  done
  echo "[orrery-wait] Timeout waiting for TCP ${host}:${port}" >&2
  exit 1
}

# Optional second argument: custom Authorization or other -H value (single header).
orrery_wait_http() {
  local url="$1"
  local hdr="${2-}"
  echo "[orrery-wait] HTTP GET ${url}"
  while ! _orrery_wait_expired; do
    if [[ -n "$hdr" ]]; then
      if curl -sf -m 5 -H "$hdr" "$url" >/dev/null; then
        echo "[orrery-wait] HTTP ready"
        return 0
      fi
    else
      if curl -sf -m 5 "$url" >/dev/null; then
        echo "[orrery-wait] HTTP ready"
        return 0
      fi
    fi
    sleep 1
  done
  echo "[orrery-wait] Timeout waiting for HTTP ${url}" >&2
  exit 1
}

# Neo4j / Schema Registry: http(s) URL uses GET; bare host:port uses TCP.
orrery_wait_endpoint() {
  local ep="$1"
  if [[ "$ep" == http://* || "$ep" == https://* ]]; then
    orrery_wait_http "$ep" ""
    return
  fi
  local t="${ep#tcp://}"
  orrery_wait_tcp "tcp://${t}"
}

# Run the startup HTTP wait for Elasticsearch/OpenSearch when the app expects it to be used
# (${ELASTICSEARCH_ENABLED:-true}; false/0 means no Elasticsearch integration). Optionally set
# SKIP_ELASTICSEARCH_CHECK=true to skip the wait even when Elasticsearch is enabled.
orrery_should_wait_elasticsearch() {
  case "${SKIP_ELASTICSEARCH_CHECK:-}" in true|TRUE|True|1) return 1 ;; esac
  case "${ELASTICSEARCH_ENABLED:-true}" in false|FALSE|False|0|no|NO|off|OFF) return 1 ;; esac
  return 0
}

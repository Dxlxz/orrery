# Set default base path if not provided
ORRERY_GMS_BASE_PATH=${ORRERY_GMS_BASE_PATH:-}

# Handle base path properly to avoid double slashes
if [ "${ORRERY_GMS_BASE_PATH}" = "/" ] || [ -z "${ORRERY_GMS_BASE_PATH}" ]; then
    export ORRERY_KAFKA_SCHEMA_REGISTRY_URL=http://localhost:8080/schema-registry/api
else
    export ORRERY_KAFKA_SCHEMA_REGISTRY_URL=http://localhost:8080${ORRERY_GMS_BASE_PATH}/schema-registry/api
fi
# Handle base path properly to avoid double slashes
if [ "${ORRERY_GMS_BASE_PATH}" = "/" ] || [ -z "${ORRERY_GMS_BASE_PATH}" ]; then
    export ORRERY_GMS_URL=http://localhost:8080
else
    export ORRERY_GMS_URL=http://localhost:8080${ORRERY_GMS_BASE_PATH}
fi
# Defining environment
ARG APP_ENV=prod

ARG BASE_IMAGE=cgr.dev/chainguard/wolfi-base:latest
FROM ${BASE_IMAGE} AS base

ARG APK_REPOSITORY_URL=https://apk.cgr.dev/chainguard
ARG GITHUB_REPO_URL=https://github.com
ARG MAVEN_CENTRAL_REPO_URL=https://repo1.maven.org/maven2

ENV JMX_VERSION=1.0.1

RUN printf '%s\n' "${APK_REPOSITORY_URL}" > /etc/apk/repositories \
    && apk update --no-cache \
    && apk upgrade --no-cache

COPY docker/snippets/setup_java_runtime.sh /tmp/setup_java_runtime.sh
RUN --mount=type=cache,target=/var/cache/apk,sharing=locked \
    chmod +x /tmp/setup_java_runtime.sh \
    && mkdir -p /orrery/orrery-upgrade/lib \
    && INSTALL_JATTACH=0 OTEL_JMX_DIR=/orrery/orrery-upgrade/lib /tmp/setup_java_runtime.sh \
    && rm -f /tmp/setup_java_runtime.sh \
    && chmod -R a+r /orrery/orrery-upgrade/lib

COPY docker/snippets/wait_for_deps.sh /usr/local/lib/orrery/wait_for_deps.sh

RUN addgroup -S orrery && adduser -S orrery -G orrery && chmod g-s /home/orrery

FROM base AS prod-install
# UPGRADE_JAR selects the boot archive: orrery-upgrade.jar (default, slim) or
# orrery-upgrade-onnx.jar (orrery-upgrade-onnx variant, bundling ONNX/DJL). Destination filename
# stays orrery-upgrade.jar so start.sh is unchanged.
ARG UPGRADE_JAR=orrery-upgrade.jar
COPY orrery-upgrade/build/libs/${UPGRADE_JAR} /orrery/orrery-upgrade/bin/orrery-upgrade.jar
COPY --chmod=755 docker/orrery-upgrade/start.sh /orrery/orrery-upgrade/scripts/start.sh
COPY docker/monitoring/client-prometheus-config.yaml /orrery/orrery-upgrade/scripts/prometheus-config.yaml
COPY metadata-models/src/main/resources/entity-registry.yml /orrery/orrery-gms/resources/entity-registry.yml

FROM base AS dev-install
# Dummy stage for development. Assumes code is built on your machine and mounted to this image.
# See this excellent thread https://github.com/docker/cli/issues/1134

FROM ${APP_ENV}-install AS final
USER orrery

# application-upgrade.yaml holds health checks, the entity client, and cache defaults.
# An explicit SPRING_PROFILES_ACTIVE still overrides this image default.
ENV SPRING_PROFILES_ACTIVE=upgrade
ENV OTEL_EXPORTER_OTLP_MAX_PAYLOAD_SIZE=4194304 \
    OTEL_EXPORTER_OTLP_HTTP_HTTP2_MAX_FRAME_SIZE=16777215 \
    OTEL_EXPORTER_OTLP_HTTP_COMPRESSION=gzip \
    OTEL_EXPORTER_OTLP_TRACES_COMPRESSION=gzip

ENTRYPOINT ["/orrery/orrery-upgrade/scripts/start.sh"]

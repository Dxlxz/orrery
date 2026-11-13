# syntax=docker/dockerfile:1
# full / slim / locked variants (see APP_ENV). Base OS: Wolfi (Chainguard); override via BASE_IMAGE.
# The full variant has additional deps preinstalled, like a JRE, Oracle client, and MSSQL ODBC driver.
ARG APP_ENV=full
ARG PYTHON_VERSION=3.11
ARG BASE_IMAGE=cgr.dev/chainguard/wolfi-base:latest

# INLINE-BEGIN @/docker/snippets/ingestion_base (Wolfi)
# Stages:
# - python-base: python, uv, venv, user orrery (UID 1000)
# - ingestion-base-slim: LDAP/SASL/Kerberos, librdkafka, ODBC, etc.
# - ingestion-base-full: JRE, build toolchain, Oracle Instant Client, MSSQL ODBC driver

FROM ${BASE_IMAGE} AS python-base

ENV HOME=/home/orrery
USER root

RUN addgroup -g 1000 orrery && \
    adduser -D -u 1000 -G orrery -h /home/orrery -s /bin/sh orrery && \
    mkdir -p $HOME && \
    chown -R orrery:orrery $HOME && \
    chmod g-s $HOME

ARG PYTHON_VERSION
RUN test -n "${PYTHON_VERSION}"  # PYTHON_VERSION must be set

# Cache-bust the python apk layer. Wolfi is a rolling distro, so a cached build layer
# can keep shipping a stale, vulnerable package set even after upstream fixes it (a
# stale cache shipped setuptools 70.3.0 / CVE-2025-47273 at /usr/share/python-wheels/;
# current Wolfi ships a fixed setuptools). Bump WOLFI_PKG_REFRESH to force a fresh
# `apk upgrade` + `apk add` whenever base packages need refreshing. `apk add` alone
# never upgrades packages already installed (from wolfi-base or earlier steps), so
# the upgrade keeps the whole OS package set current.
ARG WOLFI_PKG_REFRESH=2026-10-27
RUN --mount=type=cache,target=/var/cache/apk,sharing=locked \
    echo "wolfi pkg refresh: ${WOLFI_PKG_REFRESH}" && \
    apk upgrade --no-cache --update-cache && \
    apk add --no-cache --update-cache \
    ca-certificates \
    python-${PYTHON_VERSION} \
    python-${PYTHON_VERSION}-dev \
    uv \
    git \
    wget \
    curl \
    zip \
    unzip \
    bash \
    openssh-client

# PAM's unix_chkpwd is setuid/setgid in many images for interactive logins. This container does
# not rely on PAM password checks; clearing bits avoids ABC/DoD-style SUID-SGID file findings
# (e.g. suid_or_guid_set on /usr/bin/unix_chkpwd) without affecting the Orrery app runtime.
RUN for f in /usr/bin/unix_chkpwd /usr/sbin/unix_chkpwd; do \
    if [ -f "$f" ]; then chmod u-s,g-s "$f"; fi; \
  done

RUN ln -sf /usr/bin/python${PYTHON_VERSION} /usr/bin/python3 && \
    ln -sf /usr/bin/python3 /usr/bin/python

# OpenSSL 4 drops ssl.PROTOCOL_TLSv1. sitecustomize restores it for this
# interpreter and every venv created from it, including bundled ingestion
# venvs built later and `uv venv --python` subprocesses. A .pth copied into
# venvs that already exist does not cover those.
COPY ./docker/snippets/openssl4_ssl_compat.py /tmp/openssl4_ssl_compat.py
RUN python3 -c 'import pathlib, shutil, sysconfig; dest = pathlib.Path(sysconfig.get_path("stdlib")) / "sitecustomize.py"; shutil.copyfile("/tmp/openssl4_ssl_compat.py", dest)' \
 && rm /tmp/openssl4_ssl_compat.py

USER orrery
WORKDIR $HOME
RUN uv venv --python "${PYTHON_VERSION}"
# Pre-create as orrery so BuildKit cache mounts at $HOME/.cache/uv do not leave
# the parent root-owned (siblings like puccinialin then hit PermissionError).
RUN mkdir -p $HOME/.cache
ENV VIRTUAL_ENV=$HOME/.venv
ENV PATH="${VIRTUAL_ENV}/bin:$PATH"

# Use the system CA bundle. uv: https://github.com/astral-sh/uv/issues/1474
ENV REQUESTS_CA_BUNDLE=/etc/ssl/certs/ca-certificates.crt
ENV SSL_CERT_FILE="/etc/ssl/certs/ca-certificates.crt"
# gRPC ignores SSL_CERT_FILE and trusts its own bundled roots.pem by default.
ENV GRPC_DEFAULT_SSL_ROOTS_FILE_PATH="/etc/ssl/certs/ca-certificates.crt"

ARG UV_PROFILE=default
ARG DEFAULT_INDEX_URL=""
ARG EXTRA_INDEX_URLS=""
RUN --mount=type=bind,source=./docker/snippets/uv,target=/tmp/uv-config \
    mkdir -p $HOME/.config/uv && \
    if [ "${UV_PROFILE}" = "custom" ] || [ -n "${EXTRA_INDEX_URLS}" ]; then \
        PROFILES_DIR=/tmp/uv-config/profiles BASE_PROFILE="${UV_PROFILE}" UV_PROFILE="${UV_PROFILE}" DEFAULT_INDEX_URL="${DEFAULT_INDEX_URL}" EXTRA_INDEX_URLS="${EXTRA_INDEX_URLS}" \
          python3 /tmp/uv-config/generate-config.py > $HOME/.config/uv/uv.toml; \
    else \
        cp /tmp/uv-config/profiles/${UV_PROFILE}.toml $HOME/.config/uv/uv.toml; \
    fi

FROM python-base AS ingestion-base-slim

USER 0
# cyrus-sasl-mit-* avoids Heimdal SASL (cyrus-sasl-libs resolves via provides:).
# glibc-iconv: gconv modules required for msodbcsql18 iconv (without it, ODBC init fails with IM004).
# libssl3: msodbcsql18 dlopens libssl.so.3. The RPM is installed with --nodeps, so
# OpenSSL 3 must be declared here; it otherwise arrives only via libldap and rpm-sequoia.
RUN --mount=type=cache,target=/var/cache/apk,sharing=locked \
    apk add --no-cache --update-cache \
    openldap-2.6-dev \
    cyrus-sasl-mit-dev \
    cyrus-sasl-mit-libs \
    glibc-iconv \
    libssl3 \
    krb5 \
    krb5-dev \
    krb5-libs \
    libaio-dev \
    libaio \
    librdkafka-dev \
    openldap-2.6-clients \
    unixodbc \
    unixodbc-dev
USER orrery

FROM ingestion-base-slim AS ingestion-base-full

USER 0
COPY docker/snippets/setup_java_runtime.sh /tmp/setup_java_runtime_major.sh
# JRE on PATH for layertools/builds; JAVA_MAJOR is read from setup_java_runtime.sh (same source as service images).
# build-base pulls gcc (includes g++ driver), make, binutils, glibc-dev on Wolfi; there is no separate g++ apk.
RUN --mount=type=cache,target=/var/cache/apk,sharing=locked \
    JAVA_MAJOR=$(grep -E '^JAVA_MAJOR=[0-9]+$' /tmp/setup_java_runtime_major.sh | head -1 | cut -d= -f2) \
    && test -n "${JAVA_MAJOR}" \
    && apk add --no-cache --update-cache \
    "openjdk-${JAVA_MAJOR}-jre" \
    build-base \
    && ln -sf "/usr/lib/jvm/java-${JAVA_MAJOR}-openjdk/bin/java" /usr/bin/java \
    && rm -f /tmp/setup_java_runtime_major.sh

RUN --mount=type=bind,source=./docker/snippets/oracle_instantclient.sh,target=/oracle_instantclient.sh \
    /oracle_instantclient.sh

RUN --mount=type=bind,source=./docker/snippets/mssql_odbc.sh,target=/mssql_odbc.sh \
    /mssql_odbc.sh

USER orrery
# INLINE-END

# =============================================================================
# PRE-BUILD BUNDLED INGESTION VENVS - FULL VARIANT
# =============================================================================

FROM ingestion-base-slim AS bundled-venvs-full
USER 0

# Set up bundled venv configuration for FULL variant (with PySpark)
ARG BUNDLED_VENV_PLUGINS="s3,demo-data,file,orrery-gc,orrery-documents"
ARG BUNDLED_VENV_SLIM_MODE="false"
ARG BUNDLED_CLI_VERSION
ENV ORRERY_BUNDLED_VENV_PATH=/opt/orrery/venvs
ENV BUNDLED_VENV_PLUGINS=${BUNDLED_VENV_PLUGINS}
ENV BUNDLED_VENV_PLUGINS_COMMON=${BUNDLED_VENV_PLUGINS}
ENV BUNDLED_VENV_SLIM_MODE=${BUNDLED_VENV_SLIM_MODE}
ENV BUNDLED_CLI_VERSION=${BUNDLED_CLI_VERSION}
RUN test -n "$BUNDLED_CLI_VERSION"

# Create venv directory
RUN mkdir -p $ORRERY_BUNDLED_VENV_PATH && \
    chown -R orrery:orrery $ORRERY_BUNDLED_VENV_PATH

# Copy metadata-ingestion source (needed to build wheels)
COPY --chown=orrery:orrery ./metadata-ingestion /metadata-ingestion

# Copy the self-contained venv build scripts
COPY --chown=orrery:orrery ./docker/snippets/ingestion/build_bundled_venvs_unified.py /tmp/
COPY --chown=orrery:orrery ./docker/snippets/ingestion/build_bundled_venvs_unified.sh /tmp/
COPY --chown=orrery:orrery ./docker/snippets/ingestion/bundled_venv_config.py /tmp/
COPY --chown=orrery:orrery ./docker/snippets/ingestion/constraints.txt ${ORRERY_BUNDLED_VENV_PATH}/

# Make scripts executable
RUN chmod +x /tmp/build_bundled_venvs_unified.sh && \
    chmod +x /tmp/build_bundled_venvs_unified.py

USER orrery

# Build bundled venvs using our self-contained script (standard s3 with PySpark)
WORKDIR /tmp
RUN --mount=type=secret,id=netrc,target=/home/orrery/.netrc,required=false,uid=1000,gid=1000 \
    ./build_bundled_venvs_unified.sh

USER orrery

# =============================================================================
# PRE-BUILD BUNDLED INGESTION VENVS - SLIM VARIANT
# =============================================================================

FROM ingestion-base-slim AS bundled-venvs-slim
USER 0

# Set up bundled venv configuration for SLIM variant (without PySpark)
# Venv named s3-bundled but uses s3-slim package internally
ARG BUNDLED_VENV_PLUGINS="s3,demo-data,file,orrery-gc,orrery-documents"
ARG BUNDLED_VENV_SLIM_MODE="true"
ARG BUNDLED_CLI_VERSION
ENV ORRERY_BUNDLED_VENV_PATH=/opt/orrery/venvs
ENV BUNDLED_VENV_PLUGINS=${BUNDLED_VENV_PLUGINS}
ENV BUNDLED_VENV_PLUGINS_COMMON=${BUNDLED_VENV_PLUGINS}
ENV BUNDLED_VENV_SLIM_MODE=${BUNDLED_VENV_SLIM_MODE}
ENV BUNDLED_CLI_VERSION=${BUNDLED_CLI_VERSION}
RUN test -n "$BUNDLED_CLI_VERSION"

# Create venv directory
RUN mkdir -p $ORRERY_BUNDLED_VENV_PATH && \
    chown -R orrery:orrery $ORRERY_BUNDLED_VENV_PATH

# Copy metadata-ingestion source (needed to build wheels)
COPY --chown=orrery:orrery ./metadata-ingestion /metadata-ingestion

# Copy the self-contained venv build scripts
COPY --chown=orrery:orrery ./docker/snippets/ingestion/build_bundled_venvs_unified.py /tmp/
COPY --chown=orrery:orrery ./docker/snippets/ingestion/build_bundled_venvs_unified.sh /tmp/
COPY --chown=orrery:orrery ./docker/snippets/ingestion/bundled_venv_config.py /tmp/
COPY --chown=orrery:orrery ./docker/snippets/ingestion/constraints.txt ${ORRERY_BUNDLED_VENV_PATH}/

# Make scripts executable
RUN chmod +x /tmp/build_bundled_venvs_unified.sh && \
    chmod +x /tmp/build_bundled_venvs_unified.py

USER orrery

# Build bundled venvs using our self-contained script (s3-slim without PySpark)
WORKDIR /tmp
RUN --mount=type=secret,id=netrc,target=/home/orrery/.netrc,required=false,uid=1000,gid=1000 \
    ./build_bundled_venvs_unified.sh

USER orrery

# =============================================================================
# PRE-BUILD BUNDLED INGESTION VENVS - LOCKED VARIANT
# =============================================================================

FROM ingestion-base-slim AS bundled-venvs-locked
USER 0

# Set up bundled venv configuration for LOCKED variant (without PySpark, network blocked)
# Same as slim but will have network access disabled in final stage
ARG BUNDLED_VENV_PLUGINS="s3,demo-data,file,orrery-gc,orrery-documents"
ARG BUNDLED_VENV_SLIM_MODE="true"
ARG BUNDLED_CLI_VERSION
ENV ORRERY_BUNDLED_VENV_PATH=/opt/orrery/venvs
ENV BUNDLED_VENV_PLUGINS=${BUNDLED_VENV_PLUGINS}
ENV BUNDLED_VENV_PLUGINS_COMMON=${BUNDLED_VENV_PLUGINS}
ENV BUNDLED_VENV_SLIM_MODE=${BUNDLED_VENV_SLIM_MODE}
ENV BUNDLED_CLI_VERSION=${BUNDLED_CLI_VERSION}
RUN test -n "$BUNDLED_CLI_VERSION"

# Create venv directory
RUN mkdir -p $ORRERY_BUNDLED_VENV_PATH && \
    chown -R orrery:orrery $ORRERY_BUNDLED_VENV_PATH

# Copy metadata-ingestion source (needed to build wheels)
COPY --chown=orrery:orrery ./metadata-ingestion /metadata-ingestion

# Copy the self-contained venv build scripts
COPY --chown=orrery:orrery ./docker/snippets/ingestion/build_bundled_venvs_unified.py /tmp/
COPY --chown=orrery:orrery ./docker/snippets/ingestion/build_bundled_venvs_unified.sh /tmp/
COPY --chown=orrery:orrery ./docker/snippets/ingestion/bundled_venv_config.py /tmp/
COPY --chown=orrery:orrery ./docker/snippets/ingestion/constraints.txt ${ORRERY_BUNDLED_VENV_PATH}/

# Make scripts executable
RUN chmod +x /tmp/build_bundled_venvs_unified.sh && \
    chmod +x /tmp/build_bundled_venvs_unified.py

USER orrery

# Build bundled venvs using our self-contained script (s3-slim without PySpark)
WORKDIR /tmp
RUN --mount=type=secret,id=netrc,target=/home/orrery/.netrc,required=false,uid=1000,gid=1000 \
    ./build_bundled_venvs_unified.sh

USER orrery

# =============================================================================
# END BUNDLED VENVS SECTION
# =============================================================================

# =============================================================================
# FINAL STAGE - FULL VARIANT (default, with PySpark, network enabled)
# =============================================================================

FROM ingestion-base-full AS final-full

USER root

ENV ORRERY_BUNDLED_VENV_PATH=/opt/orrery/venvs
ARG BUNDLED_CLI_VERSION
ENV BUNDLED_CLI_VERSION=${BUNDLED_CLI_VERSION}
ARG BUNDLED_VENV_PLUGINS="s3,demo-data,file,orrery-gc,orrery-documents"
ENV BUNDLED_VENV_PLUGINS=${BUNDLED_VENV_PLUGINS}
ENV BUNDLED_VENV_PLUGINS_COMMON=${BUNDLED_VENV_PLUGINS}
ARG BUNDLED_VENV_SLIM_MODE="false"
ENV BUNDLED_VENV_SLIM_MODE=${BUNDLED_VENV_SLIM_MODE}
COPY --from=bundled-venvs-full $ORRERY_BUNDLED_VENV_PATH $ORRERY_BUNDLED_VENV_PATH

# Shipped for downstream Dockerfiles that extend the published image without cloning the repo (see docs/docker/bundled-ingestion-venvs.md).
COPY --chown=orrery:orrery \
    ./docker/snippets/ingestion/build_bundled_venvs_unified.py \
    ./docker/snippets/ingestion/build_bundled_venvs_unified.sh \
    ./docker/snippets/ingestion/bundled_venv_config.py \
    ./docker/snippets/ingestion/constraints.txt \
    /opt/orrery/bundled-venv-build/
RUN chmod 755 /opt/orrery/bundled-venv-build/build_bundled_venvs_unified.sh && \
    chown -R orrery:orrery /opt/orrery/bundled-venv-build

COPY --chown=orrery:orrery ./docker/orrery-actions/start.sh /start_orrery_actions.sh
COPY --chown=orrery:orrery ./docker/orrery-actions/readiness-check.sh /readiness-check.sh

RUN chmod a+x /start_orrery_actions.sh && \
    mkdir -p /etc/orrery/actions && \
    mkdir -p /tmp/orrery/logs/actions/system && \
    chown -R orrery:orrery /etc/orrery /tmp/orrery

# Install a cacheble layer that installs external dependencies and does not get invalidated due to changes in ingestion or actions code
# Copy just enough to enable pip compile to work. Other code changes wont invalidate this layer.
COPY --chown=orrery:orrery ./metadata-ingestion/setup.py /metadata-ingestion/
COPY --chown=orrery:orrery ./metadata-ingestion/src/orrery/_version.py /metadata-ingestion/src/orrery/
COPY --chown=orrery:orrery ./orrery-actions/setup.py /orrery-actions/
COPY --chown=orrery:orrery ./orrery-actions/src/orrery_actions/_version.py /orrery-actions/src/orrery_actions/
COPY --chown=orrery:orrery ./orrery-actions/README.md /orrery-actions/

# Apply CVE floors from docker/snippets/ingestion/constraints.txt to the default app venv
# (bundled venvs already pass --constraint explicitly).
ENV UV_CONSTRAINT=${ORRERY_BUNDLED_VENV_PATH}/constraints.txt

USER orrery
RUN --mount=type=secret,id=netrc,target=/home/orrery/.netrc,required=false,uid=1000,gid=1000 \
    printf -- '-e /metadata-ingestion/\n-e /orrery-actions/[all]\n' | uv pip compile /dev/stdin | grep -v "\-e" | uv pip install -r /dev/stdin
USER 0

COPY --chown=orrery:orrery ./metadata-ingestion /metadata-ingestion
COPY --chown=orrery:orrery ./orrery-actions /orrery-actions
COPY --chown=orrery:orrery ./docker/orrery-actions/config /etc/orrery/actions/system/conf
COPY --chown=orrery:orrery ./docker/orrery-actions/config-postgres-pgqueue /etc/orrery/actions/system/conf-postgres-pgqueue

USER orrery

ARG RELEASE_VERSION
RUN test -n "$RELEASE_VERSION" # RELEASE_VERSION is a required build arg
RUN --mount=type=bind,source=./python-build/version_updater.py,target=/version_updater.py \
    python /version_updater.py --directory /metadata-ingestion/ --version "$RELEASE_VERSION" --expected-update-count 1 && \
    python /version_updater.py --directory /orrery-actions/ --version "$RELEASE_VERSION" --expected-update-count 1

# Install metadata-ingestion with base extras (network enabled, can install more at runtime)
RUN --mount=type=cache,target=$HOME/.cache/uv,uid=1000,gid=1000,id=orrery-actions,sharing=private \
    --mount=type=secret,id=netrc,target=/home/orrery/.netrc,required=false,uid=1000,gid=1000 \
  uv pip install -e '/metadata-ingestion/[base,s3,gcs,abs]'

# Install orrery-actions with all extras
RUN --mount=type=cache,target=$HOME/.cache/uv,uid=1000,gid=1000,id=orrery-actions,sharing=private \
    --mount=type=secret,id=netrc,target=/home/orrery/.netrc,required=false,uid=1000,gid=1000 \
  uv pip install -e '/orrery-actions/[all]'
RUN rm -rf $HOME/.config/uv && \
    # pip's vendored CycloneDX BOM lists build-time setuptools/msgpack pins that Trivy
    # reports as installed packages (AnalyzedBy=sbom). Remove the BOM; the real packages
    # in site-packages remain the source of truth.
    find /home/orrery/.venv /opt/orrery/venvs -path '*/pip/_vendor/bom.cdx.json' -delete

ENTRYPOINT [ ]
CMD ["/bin/bash", "/start_orrery_actions.sh"]

# =============================================================================
# FINAL STAGE - SLIM VARIANT (no PySpark, network enabled)
# =============================================================================

FROM ingestion-base-slim AS final-slim

USER root

ENV ORRERY_BUNDLED_VENV_PATH=/opt/orrery/venvs
ARG BUNDLED_CLI_VERSION
ENV BUNDLED_CLI_VERSION=${BUNDLED_CLI_VERSION}
ARG BUNDLED_VENV_PLUGINS="s3,demo-data,file,orrery-gc,orrery-documents"
ENV BUNDLED_VENV_PLUGINS=${BUNDLED_VENV_PLUGINS}
ENV BUNDLED_VENV_PLUGINS_COMMON=${BUNDLED_VENV_PLUGINS}
ARG BUNDLED_VENV_SLIM_MODE="true"
ENV BUNDLED_VENV_SLIM_MODE=${BUNDLED_VENV_SLIM_MODE}
COPY --from=bundled-venvs-slim $ORRERY_BUNDLED_VENV_PATH $ORRERY_BUNDLED_VENV_PATH

# Shipped for downstream Dockerfiles that extend the published image without cloning the repo (see docs/docker/bundled-ingestion-venvs.md).
COPY --chown=orrery:orrery \
    ./docker/snippets/ingestion/build_bundled_venvs_unified.py \
    ./docker/snippets/ingestion/build_bundled_venvs_unified.sh \
    ./docker/snippets/ingestion/bundled_venv_config.py \
    ./docker/snippets/ingestion/constraints.txt \
    /opt/orrery/bundled-venv-build/
RUN chmod 755 /opt/orrery/bundled-venv-build/build_bundled_venvs_unified.sh && \
    chown -R orrery:orrery /opt/orrery/bundled-venv-build

COPY --chown=orrery:orrery ./docker/orrery-actions/start.sh /start_orrery_actions.sh
COPY --chown=orrery:orrery ./docker/orrery-actions/readiness-check.sh /readiness-check.sh

RUN chmod a+x /start_orrery_actions.sh && \
    mkdir -p /etc/orrery/actions && \
    mkdir -p /tmp/orrery/logs/actions/system && \
    chown -R orrery:orrery /etc/orrery /tmp/orrery

# Install a cacheable layer that installs external dependencies
COPY --chown=orrery:orrery ./metadata-ingestion/setup.py /metadata-ingestion/
COPY --chown=orrery:orrery ./metadata-ingestion/src/orrery/_version.py /metadata-ingestion/src/orrery/
COPY --chown=orrery:orrery ./orrery-actions/setup.py /orrery-actions/
COPY --chown=orrery:orrery ./orrery-actions/src/orrery_actions/_version.py /orrery-actions/src/orrery_actions/
COPY --chown=orrery:orrery ./orrery-actions/README.md /orrery-actions/

# Apply CVE floors from docker/snippets/ingestion/constraints.txt to the default app venv
# (bundled venvs already pass --constraint explicitly).
ENV UV_CONSTRAINT=${ORRERY_BUNDLED_VENV_PATH}/constraints.txt

USER orrery
RUN --mount=type=secret,id=netrc,target=/home/orrery/.netrc,required=false,uid=1000,gid=1000 \
    printf -- '-e /metadata-ingestion/\n-e /orrery-actions/[all]\n' | uv pip compile /dev/stdin | grep -v "\-e" | uv pip install -r /dev/stdin
USER 0

COPY --chown=orrery:orrery ./metadata-ingestion /metadata-ingestion
COPY --chown=orrery:orrery ./orrery-actions /orrery-actions
COPY --chown=orrery:orrery ./docker/orrery-actions/config /etc/orrery/actions/system/conf
COPY --chown=orrery:orrery ./docker/orrery-actions/config-postgres-pgqueue /etc/orrery/actions/system/conf-postgres-pgqueue

USER orrery

ARG RELEASE_VERSION
RUN test -n "$RELEASE_VERSION"
RUN --mount=type=bind,source=./python-build/version_updater.py,target=/version_updater.py \
    python /version_updater.py --directory /metadata-ingestion/ --version "$RELEASE_VERSION" --expected-update-count 1 && \
    python /version_updater.py --directory /orrery-actions/ --version "$RELEASE_VERSION" --expected-update-count 1

# Install metadata-ingestion with SLIM extras (no PySpark, network enabled for flexibility)
RUN --mount=type=cache,target=$HOME/.cache/uv,uid=1000,gid=1000,id=orrery-actions,sharing=private \
    --mount=type=secret,id=netrc,target=/home/orrery/.netrc,required=false,uid=1000,gid=1000 \
  uv pip install -e '/metadata-ingestion/[base,s3-slim,gcs-slim,abs-slim]'

# Install orrery-actions with all extras
RUN --mount=type=cache,target=$HOME/.cache/uv,uid=1000,gid=1000,id=orrery-actions,sharing=private \
    --mount=type=secret,id=netrc,target=/home/orrery/.netrc,required=false,uid=1000,gid=1000 \
  uv pip install -e '/orrery-actions/[all]'
RUN rm -rf $HOME/.config/uv && \
    # pip's vendored CycloneDX BOM lists build-time setuptools/msgpack pins that Trivy
    # reports as installed packages (AnalyzedBy=sbom). Remove the BOM; the real packages
    # in site-packages remain the source of truth.
    find /home/orrery/.venv /opt/orrery/venvs -path '*/pip/_vendor/bom.cdx.json' -delete

ENTRYPOINT [ ]
CMD ["/bin/bash", "/start_orrery_actions.sh"]

# =============================================================================
# FINAL STAGE - LOCKED VARIANT (no PySpark, network BLOCKED, bundled venvs only)
# =============================================================================

FROM ingestion-base-slim AS final-locked

USER root

ENV ORRERY_BUNDLED_VENV_PATH=/opt/orrery/venvs
ARG BUNDLED_CLI_VERSION
ENV BUNDLED_CLI_VERSION=${BUNDLED_CLI_VERSION}
ARG BUNDLED_VENV_PLUGINS="s3,demo-data,file,orrery-gc,orrery-documents"
ENV BUNDLED_VENV_PLUGINS=${BUNDLED_VENV_PLUGINS}
ENV BUNDLED_VENV_PLUGINS_COMMON=${BUNDLED_VENV_PLUGINS}
ARG BUNDLED_VENV_SLIM_MODE="true"
ENV BUNDLED_VENV_SLIM_MODE=${BUNDLED_VENV_SLIM_MODE}
COPY --from=bundled-venvs-locked $ORRERY_BUNDLED_VENV_PATH $ORRERY_BUNDLED_VENV_PATH

COPY --chown=orrery:orrery ./docker/orrery-actions/start.sh /start_orrery_actions.sh
COPY --chown=orrery:orrery ./docker/orrery-actions/readiness-check.sh /readiness-check.sh

RUN chmod a+x /start_orrery_actions.sh && \
    mkdir -p /etc/orrery/actions && \
    mkdir -p /tmp/orrery/logs/actions/system && \
    chown -R orrery:orrery /etc/orrery /tmp/orrery

# Bundled venvs under /opt/orrery/venvs are built with editable installs against
# /metadata-ingestion. Ship that tree so subprocess ingestion can import orrery;
# the default app venv still does not pip-install metadata-ingestion (PyPI blocked below).
COPY --chown=orrery:orrery ./metadata-ingestion /metadata-ingestion

# Copy only orrery-actions code for the actions framework venv (not a pip install of metadata-ingestion)
COPY --chown=orrery:orrery ./orrery-actions/setup.py /orrery-actions/
COPY --chown=orrery:orrery ./orrery-actions/src/orrery_actions/_version.py /orrery-actions/src/orrery_actions/
COPY --chown=orrery:orrery ./orrery-actions/README.md /orrery-actions/

# Apply CVE floors from docker/snippets/ingestion/constraints.txt to the default app venv
# (bundled venvs already pass --constraint explicitly).
ENV UV_CONSTRAINT=${ORRERY_BUNDLED_VENV_PATH}/constraints.txt

USER orrery
# Install only orrery-actions, NOT metadata-ingestion
RUN --mount=type=secret,id=netrc,target=/home/orrery/.netrc,required=false,uid=1000,gid=1000 \
    printf -- '-e /orrery-actions/[all]\n' | uv pip compile /dev/stdin | grep -v "\-e" | uv pip install -r /dev/stdin
USER 0

COPY --chown=orrery:orrery ./orrery-actions /orrery-actions
COPY --chown=orrery:orrery ./docker/orrery-actions/config /etc/orrery/actions/system/conf
COPY --chown=orrery:orrery ./docker/orrery-actions/config-postgres-pgqueue /etc/orrery/actions/system/conf-postgres-pgqueue

USER orrery

ARG RELEASE_VERSION
RUN test -n "$RELEASE_VERSION"
RUN --mount=type=bind,source=./python-build/version_updater.py,target=/version_updater.py \
    python /version_updater.py --directory /metadata-ingestion/ --version "$RELEASE_VERSION" --expected-update-count 1 && \
    python /version_updater.py --directory /orrery-actions/ --version "$RELEASE_VERSION" --expected-update-count 1

# Install ONLY orrery-actions into the default venv (not metadata-ingestion)
RUN --mount=type=cache,target=$HOME/.cache/uv,uid=1000,gid=1000,id=orrery-actions,sharing=private \
    --mount=type=secret,id=netrc,target=/home/orrery/.netrc,required=false,uid=1000,gid=1000 \
  uv pip install -e '/orrery-actions/[all]'
RUN rm -rf $HOME/.config/uv && \
    # pip's vendored CycloneDX BOM lists build-time setuptools/msgpack pins that Trivy
    # reports as installed packages (AnalyzedBy=sbom). Remove before strip_pip removes pip.
    find /home/orrery/.venv /opt/orrery/venvs -path '*/pip/_vendor/bom.cdx.json' -delete

# No uv/pip in the final locked image: build is done; package managers are unnecessary attack surface.
# Script file avoids Docker RUN treating $v / $sp as empty Docker env vars (see docker/snippets/strip_pip_uv_from_venvs.sh).
USER root
COPY ./docker/snippets/strip_pip_uv_from_venvs.sh /tmp/strip_pip_uv_from_venvs.sh
RUN chmod 755 /tmp/strip_pip_uv_from_venvs.sh
USER orrery
RUN sh /tmp/strip_pip_uv_from_venvs.sh
USER 0
# py3-pip-wheel pulls in a bootstrap pip wheel; it cannot be apk-removed while python-* is
# installed. Locked images strip pip/uv and block network — delete the wheel file only here
# (CVE-2026-3219: bundled pip wheel surface).
RUN rm -f /tmp/strip_pip_uv_from_venvs.sh && apk del --no-cache uv && \
    rm -f /usr/share/python-wheels/pip-*.whl
USER orrery

# Defense in depth: if any pip/uv-style tool is reintroduced, default index URL is unusable.
ENV UV_INDEX_URL=http://127.0.0.1:1/simple
ENV PIP_INDEX_URL=http://127.0.0.1:1/simple

ENTRYPOINT [ ]
CMD ["/bin/bash", "/start_orrery_actions.sh"]

# =============================================================================
# DEFAULT EXPORT - Use APP_ENV to select variant (defaults to full)
# =============================================================================

FROM final-${APP_ENV} AS final

FROM dxlxz/orrery-ingestion:head-slim AS base

USER 0
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y \
    sudo \
    python3-dev \
    libgtk2.0-0 \
    libgtk-3-0 \
    libgbm-dev \
    libnotify-dev \
    libnss3 \
    libxss1 \
    libasound2t64 \
    libxtst6 \
    xauth \
    xvfb \
    openjdk-25-jdk && \
    rm -rf /var/lib/apt/lists/* /var/cache/apk/*

USER orrery
COPY . /orrery-src
ARG RELEASE_VERSION
RUN cd /orrery-src && \
    sed -i.bak "s/__version__ = .*$/__version__ = \"$(echo $RELEASE_VERSION|sed s/-/+/)\"/" metadata-ingestion/src/orrery/_version.py && \
    sed -i.bak "s/__version__ = .*$/__version__ = \"$(echo $RELEASE_VERSION|sed s/-/+/)\"/" metadata-ingestion-modules/airflow-plugin/src/orrery_airflow_plugin/_version.py && \
    cat metadata-ingestion/src/orrery/_version.py && \
    ./gradlew :metadata-ingestion:codegen && \
    pip install file:metadata-ingestion-modules/airflow-plugin#egg=orrery-airflow-plugin file:metadata-ingestion#egg=orrery


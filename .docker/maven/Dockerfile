# syntax=docker/dockerfile:1.7@sha256:a57df69d0ea827fb7266491f2813635de6f17269be881f696fbfdf2d83dda33e
# Firefly build/test image: JDK 25 + Maven + the OS libraries Playwright's Chromium needs
# (webtui-plugin browser tests, doc screenshots). Ubuntu 24.04 (noble) because Playwright
# does not yet support Chromium on 26.04. Built and cached by scripts/mvn.sh.
ARG BASE_IMAGE=maven:3-eclipse-temurin-25-noble@sha256:d4bb5796a17ab2422f609b764ba16ddcbd2e6ba5b639168eb9e99e6de8f1f431
FROM ${BASE_IMAGE}
# Optional build-time CA for TLS-intercepting proxies: --secret id=ca,src=ca.crt (removed again
# in the same layer). Proxies: --build-arg https_proxy=...
# hadolint ignore=DL3008
RUN --mount=type=secret,id=ca,required=false \
    set -eu; \
    if [ -f /run/secrets/ca ]; then \
      cp /run/secrets/ca /usr/local/share/ca-certificates/build-ca.crt && update-ca-certificates >/dev/null; \
      sed -i 's#http://#https://#g' /etc/apt/sources.list.d/ubuntu.sources; \
    fi; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
      libnss3 libnspr4 libatk1.0-0t64 libatk-bridge2.0-0t64 libcups2t64 libdrm2 libxkbcommon0 \
      libxcomposite1 libxdamage1 libxfixes3 libxrandr2 libgbm1 libpango-1.0-0 libcairo2 \
      libasound2t64 libatspi2.0-0t64 libx11-xcb1 libxcursor1 libgtk-3-0t64 fonts-liberation; \
    rm -rf /var/lib/apt/lists/*; \
    if [ -f /usr/local/share/ca-certificates/build-ca.crt ]; then \
      rm /usr/local/share/ca-certificates/build-ca.crt && update-ca-certificates --fresh >/dev/null; \
      sed -i 's#https://#http://#g' /etc/apt/sources.list.d/ubuntu.sources; \
    fi
# Browsers live in the mounted cache next to the Maven repository, so they download once.
ENV PLAYWRIGHT_BROWSERS_PATH=/tmp/.m2/ms-playwright

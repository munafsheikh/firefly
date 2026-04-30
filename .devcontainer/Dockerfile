FROM eclipse-temurin:26-jdk

RUN apt-get update && apt-get install -y \
    git \
    curl \
    procps \
    sudo \
    docker.io \
    && rm -rf /var/lib/apt/lists/*

RUN useradd -m -s /bin/bash dev \
    && echo "dev ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

WORKDIR /workspace

USER dev

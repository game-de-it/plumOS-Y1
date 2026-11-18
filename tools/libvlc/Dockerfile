FROM --platform=linux/amd64 debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends \
 ca-certificates curl wget unzip zip git patch build-essential autoconf automake \
 libtool libtool-bin libltdl-dev autopoint gettext pkg-config cmake ninja-build meson \
 python3 python3-setuptools flex bison gperf nasm yasm xz-utils bzip2 ant \
 openjdk-17-jdk-headless file texinfo && rm -rf /var/lib/apt/lists/*
WORKDIR /work

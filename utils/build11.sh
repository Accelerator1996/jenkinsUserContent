#!/usr/bin/env bash
# Prepare variables
BINARY=$1
DIR="dragonwell11"
IMAGE_TAG=$2
BINARYALPINE=$3
ARCH=`arch`


# Prepare Dockerfiles
LICENSE="#
# Licensed under the Apache License, Version 2.0 (the \"License\");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an \"AS IS\" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#

# based on adoptopenjdk script:
# https://github.com/AdoptOpenJDK/openjdk-docker/blob/master/8/jdk/alpine/Dockerfile.hotspot.nightly.full"

cat > Dockerfile << END_SH
$LICENSE

FROM centos:7

RUN yum install -y tzdata openssl curl ca-certificates fontconfig gzip tar \\
    && yum update -y; yum clean all

ENV JAVA_VERSION dragonwel11u

RUN set -eux; \\
    BINARY_URL='$BINARY'; \\
    DRAGONWELL_DIR=$DIR; \\
    curl -LSo /tmp/dragonwel11.tar.gz \${BINARY_URL}; \\
    rm -rf /opt/alibaba/; \\
    mkdir -p /opt/alibaba/\${DRAGONWELL_DIR}; \\
    cd /opt/alibaba/\${DRAGONWELL_DIR}; \\
    tar -xf /tmp/dragonwel11.tar.gz --strip-components=1; \\
    rm -rf /tmp/dragonwel11.tar.gz;

ENV JAVA_HOME=/opt/alibaba/$DIR \\
    PATH="/opt/alibaba/$DIR/bin:\$PATH"

END_SH

cat > Dockerfile.alpine << END_SH
$LICENSE

FROM alpine:3.14


RUN apk --update add --no-cache --virtual .build-deps curl binutils \
    && GLIBC_VER="2.28-r0" \
    && ALPINE_GLIBC_REPO="https://github.com/sgerrand/alpine-pkg-glibc/releases/download" \
    && GCC_LIBS_URL="https://archive.archlinux.org/packages/g/gcc-libs/gcc-libs-8.2.1%2B20180831-1-x86_64.pkg.tar.xz" \
    && GCC_LIBS_SHA256=e4b39fb1f5957c5aab5c2ce0c46e03d30426f3b94b9992b009d417ff2d56af4d \
    && ZLIB_URL="https://archive.archlinux.org/packages/z/zlib/zlib-1%3A1.2.9-1-x86_64.pkg.tar.xz" \
    && ZLIB_SHA256=bb0959c08c1735de27abf01440a6f8a17c5c51e61c3b4c707e988c906d3b7f67 \
    && curl -Ls https://alpine-pkgs.sgerrand.com/sgerrand.rsa.pub -o /etc/apk/keys/sgerrand.rsa.pub \
    && curl -Ls ${ALPINE_GLIBC_REPO}/${GLIBC_VER}/glibc-${GLIBC_VER}.apk > /tmp/${GLIBC_VER}.apk \
    && apk add /tmp/${GLIBC_VER}.apk \
    && curl -Ls ${GCC_LIBS_URL} -o /tmp/gcc-libs.tar.xz \
    && echo "${GCC_LIBS_SHA256}  /tmp/gcc-libs.tar.xz" | sha256sum -c - \
    && mkdir /tmp/gcc \
    && tar -xf /tmp/gcc-libs.tar.xz -C /tmp/gcc \
    && mv /tmp/gcc/usr/lib/libgcc* /tmp/gcc/usr/lib/libstdc++* /usr/glibc-compat/lib \
    && strip /usr/glibc-compat/lib/libgcc_s.so.* /usr/glibc-compat/lib/libstdc++.so* \
    && curl -Ls ${ZLIB_URL} -o /tmp/libz.tar.xz \
    && echo "${ZLIB_SHA256}  /tmp/libz.tar.xz" | sha256sum -c - \
    && mkdir /tmp/libz \
    && tar -xf /tmp/libz.tar.xz -C /tmp/libz \
    && mv /tmp/libz/usr/lib/libz.so* /usr/glibc-compat/lib \
    && apk del --purge .build-deps \
    && rm -rf /tmp/${GLIBC_VER}.apk /tmp/gcc /tmp/gcc-libs.tar.xz /tmp/libz /tmp/libz.tar.xz /var/cache/apk/*

ENV JAVA_VERSION dragonwel11u

RUN set -eux; \\
    BINARY_URL='$BINARYALPINE'; \\
    DRAGONWELL_DIR=$DIR; \\
    curl -LSo /tmp/dragonwel11.tar.gz \${BINARY_URL}; \\
    rm -rf /opt/alibaba/; \\
    mkdir -p /opt/alibaba/\${DRAGONWELL_DIR}; \\
    cd /opt/alibaba/\${DRAGONWELL_DIR}; \\
    tar -xf /tmp/dragonwel11.tar.gz --strip-components=1; \\
    rm -rf /tmp/dragonwel11.tar.gz;

ENV JAVA_HOME=/opt/alibaba/$DIR \\
    PATH="/opt/alibaba/$DIR/bin:\$PATH"

END_SH

cat > Dockerfile.slim << END_SH
$LICENSE

FROM centos:7

RUN yum install -y tzdata openssl curl ca-certificates fontconfig gzip tar \\
    && yum update -y; yum clean all

COPY slim-java/* /usr/local/bin/

ENV JAVA_VERSION dragonwel11u

RUN set -eux; \\
    BINARY_URL='$BINARY'; \\
    DRAGONWELL_DIR=$DIR; \\
    curl -LSo /tmp/dragonwel11.tar.gz \${BINARY_URL}; \\
    rm -rf /opt/alibaba/; \\
    mkdir -p /opt/alibaba/\${DRAGONWELL_DIR}; \\
    cd /opt/alibaba/\${DRAGONWELL_DIR}; \\
    tar -xf /tmp/dragonwel11.tar.gz --strip-components=1; \\
    export PATH="/opt/alibaba/\${DRAGONWELL_DIR}/bin:\$PATH"; \\
    chmod 777 /usr/local/bin/slim-java.sh; \\
    /usr/local/bin/slim-java.sh /opt/alibaba/\${DRAGONWELL_DIR}; \\
    rm -rf /tmp/dragonwel11.tar.gz;

ENV JAVA_HOME=/opt/alibaba/$DIR \\
    PATH="/opt/alibaba/$DIR/bin:\$PATH"

END_SH

operation_docker() {
    docker build -f $DOCKER_FILE -t ${DOCKER_ID} .
    RET=`docker run -it --rm ${DOCKER_ID} sh -c "java -version"`
    str_sanitized=$(echo $RET | tr -d '\r')
    echo "Java version check:"
    echo "$RET"
    docker push ${DOCKER_ID}
}


export DOCKER_ID="registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell:${IMAGE_TAG}_${ARCH}"
export DOCKER_FILE=Dockerfile
operation_docker

if [ ! -z "${BINARYALPINE}" ]; then
export DOCKER_ID="registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell:${IMAGE_TAG}_alpine_${ARCH}"
export DOCKER_FILE=Dockerfile.alpine
operation_docker
docker push ${DOCKER_ID}
fi


echo "build slim java"
export DOCKER_ID="registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell:${IMAGE_TAG}_${ARCH}_slim"
export DOCKER_FILE=Dockerfile.slim
mkdir slim-java
wget https://raw.githubusercontent.com/AdoptOpenJDK/openjdk-docker/master/8/jdk/ubuntu/slim-java.sh -O slim-java/slim-java.sh
wget https://raw.githubusercontent.com/AdoptOpenJDK/openjdk-docker/master/8/jdk/ubuntu/slim-java_rtjar_keep.list -O slim-java/slim-java_rtjar_keep.list;
wget https://raw.githubusercontent.com/AdoptOpenJDK/openjdk-docker/master/8/jdk/ubuntu/slim-java_rtjar_del.list -O slim-java/slim-java_rtjar_del.list;
operation_docker
docker push ${DOCKER_ID}
rm -rf slim-java

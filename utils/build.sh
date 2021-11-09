#!/usr/bin/env bash
# Prepare variables
BINARY=$1
DIR="dragonwell8"
IMAGE_TAG=$2
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

ENV JAVA_VERSION dragonwell8u

RUN set -eux; \\
    BINARY_URL='$BINARY'; \\
    DRAGONWELL_DIR=$DIR; \\
    curl -LSo /tmp/dragonwell8.tar.gz \${BINARY_URL}; \\
    rm -rf /opt/alibaba/; \\
    mkdir -p /opt/alibaba/\${DRAGONWELL_DIR}; \\
    cd /opt/alibaba/\${DRAGONWELL_DIR}; \\
    tar -xf /tmp/dragonwell8.tar.gz --strip-components=1; \\
    rm -rf /tmp/dragonwell8.tar.gz;

ENV JAVA_HOME=/opt/alibaba/$DIR \\
    PATH="/opt/alibaba/$DIR/bin:\$PATH"

END_SH

cat > Dockerfile.slim << END_SH
$LICENSE

FROM centos:7

COPY slim-java/* /usr/local/bin/

RUN yum install -y tzdata openssl curl ca-certificates fontconfig gzip tar \\
    && yum update -y; yum clean all

ENV JAVA_VERSION dragonwell8u

RUN set -eux; \\
    BINARY_URL='$BINARY'; \\
    DRAGONWELL_DIR=$DIR; \\
    curl -LSo /tmp/dragonwell8.tar.gz \${BINARY_URL}; \\
    rm -rf /opt/alibaba/; \\
    mkdir -p /opt/alibaba/\${DRAGONWELL_DIR}; \\
    cd /opt/alibaba/\${DRAGONWELL_DIR}; \\
    tar -xf /tmp/dragonwell8.tar.gz --strip-components=1; \\
    export PATH="/opt/alibaba/\${DRAGONWELL_DIR}/bin:\$PATH"; \\
    chmod 777 /usr/local/bin/slim-java.sh; \\
    /usr/local/bin/slim-java.sh /opt/alibaba/\${DRAGONWELL_DIR}; \\
    rm -rf /tmp/dragonwell8.tar.gz;

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


echo "build slim java"
export DOCKER_ID="registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell:${IMAGE_TAG}_${ARCH}_slim"
export DOCKER_FILE=Dockerfile.slim
mkdir slim-java
wget https://raw.githubusercontent.com/AdoptOpenJDK/openjdk-docker/master/8/jdk/ubuntu/slim-java.sh -O slim-java/slim-java.sh
wget https://raw.githubusercontent.com/AdoptOpenJDK/openjdk-docker/master/8/jdk/ubuntu/slim-java_rtjar_keep.list -O slim-java/slim-java_rtjar_keep.list;
wget https://raw.githubusercontent.com/AdoptOpenJDK/openjdk-docker/master/8/jdk/ubuntu/slim-java_rtjar_del.list -O slim-java/slim-java_rtjar_del.list;
operation_docker
rm -rf slim-java


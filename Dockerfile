FROM ubuntu:latest
RUN apt-get update && apt-get install -y \
    vim \
    mysql-client \
    openjdk-8-jdk \
    git \
&& apt-get clean && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /home/test/mo-tester
WORKDIR /home/test/
COPY . mo-tester/
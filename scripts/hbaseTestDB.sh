#!/bin/bash

docker pull daunnc/geodocker-hbase-standalone:latest

echo "Requires additional setup in case of Windows / OS X, known limitations: https://docs.docker.com/docker-for-mac/networking/"
echo "Starting HBase container"

id=`docker run -d --name=hbase-docker -h localhost daunnc/geodocker-hbase-standalone:latest`

echo "Container has ID $id"

# Get the hostname and IP inside the container
docker_hostname=`docker inspect --format '{{ .Config.Hostname }}' $id`
docker_ip=`docker inspect --format '{{ .NetworkSettings.IPAddress }}' $id`

echo "Updating /etc/hosts to make hbase-docker point to $docker_ip ($docker_hostname)"
if grep 'hbase-docker' /etc/hosts >/dev/null; then
  sudo sed -i .bak "s/^.*hbase-docker.*\$/$docker_ip hbase-docker $docker_hostname/" /etc/hosts
else
  sudo sh -c "echo '$docker_ip localhost hbase-docker $docker_hostname' >> /etc/hosts"
fi

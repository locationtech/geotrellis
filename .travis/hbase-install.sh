#! /bin/bash

sudo wget http://apache-mirror.rbc.ru/pub/apache/hbase/1.2.3/hbase-1.2.3-bin.tar.gz
sudo tar xzf hbase-1.2.3-bin.tar.gz
sudo rm -f hbase-1.2.3/conf/hbase-site.xml && sudo mv .travis/hbase/hbase-site.xml hbase-1.2.3/conf
sudo hbase-1.2.3/bin/start-hbase.sh

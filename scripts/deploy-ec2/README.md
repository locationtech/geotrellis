# Deploy EC2

This directory contains a set of Ansible scripts that will deploy a Sparc/Hadoop-HDFS cluster.

## Settings

  - Edit `group_vars/all.yml` with your cluster settings.
  - Copy your EC2 .pem key to keys folder, it will be used to create the cluster
  - Spark and Hadoop roles contain config files under `templates` folder

## Allocate cluster

`ansible-playbook allocate-cluster.yml -i localhost,`

The allocate script does not requires an inventory file so we give it only the localhost target.
This script will provision the cluster machines and create the cluster inventory in `hosts` file.
Some additional information is written to `group_vars/cluster_vars.yml` for use by other playbooks.

It is safe to run this playbook multiple times, it uses instances tags to make sure not to allocate duplicate machines between runs.

_Note_: When instances are restarted/rebooted they sometimes are given different IPs. When this happens it is important to re-run this script to refresh the `hosts` inventory.

## Setup cluster

`ansible-playbook setup-common.yml -i hosts`

This playbook with install the OpenJDK 7, perform key management to make sure instances can ssh amongst themselves and add the Cloudera apt repository. This is a separate playbook because it is likely to only be run once.

`ansible-playbook setup-cluster.yml -i hosts`

This script will perform all the Spark and HDFS configurations. When running this script multiple times changes in config files will cause respective services to restart. HDFS NameNode will only be formatted the first time.

## Stop/Start

- `start-cluster.yml`
- `stop-cluster.yml`
- `start-cluster.yml`
- `terminate-cluster.yml`
- `restart-spark.yml`

_Note_: terminate playbook will have no effect on stopped cluster, you will have to bring such a cluster up to running state before for it to have the desired effect.


## VPN

`setup-master.yml` installs pptpd on the master node. This is very useful because spark master/workers expose to HTTP interface that is bound to the private interface. Look under `roles/pptpd/files` to configure user/password. 

- You will have to add `ec2.internal` to the VPN interface for the DNS resolution to work
- Make sure that your subnet has access to the VPC by changing the security group section in the `allocate-cluster.yml`


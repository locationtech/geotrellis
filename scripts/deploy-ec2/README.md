# Deploy EC2

This directory contains a set of Ansible scripts that will deploy a Spark/Hadoop-HDFS cluster.

### Prerequisites for running
  - Python >2.5
  - [Ansible >1.6](http://www.ansible.com/home) (For Ubuntu users, [check here to install the latest version](http://docs.ansible.com/intro_installation.html#latest-releases-via-apt-ubuntu))
  - [Boto](http://boto.readthedocs.org/en/latest/)

To set up a spark\HDFS cluster, follow these steps:

## Settings

  - Copy `group_vars/all.yml.template` to `group_vars/all.yml` and edit with your cluster settings.
  - Copy your EC2 .pem key to the `keys` folder (create if not present). Each instance in the cluster needs this .pem key.

Spark and Hadoop anslible `roles` contain config files under `templates` folder, which you may optionally edit.

## Allocate cluster

`ansible-playbook allocate-cluster.yml -i localhost,`

The allocate script does not require an inventory file so we give it only the `localhost,` target (the comma is necessary because of ansible formatting).
This script will provision the cluster machines and create the cluster inventory in `hosts` file.
The allocated cluster instances will be written to `group_vars/cluster_vars.yml` for use by other playbooks.

It is safe to run this playbook multiple times, it uses instances tags to make sure not to allocate duplicate machines between runs.

_Note_: When instances are restarted/rebooted they sometimes are given different IPs. When this happens it is important to re-run this script to refresh the `hosts` inventory.

## Setup cluster

`ansible-playbook setup-common.yml -i hosts`

This playbook will install the OpenJDK 7, perform key management to make sure instances can ssh amongst themselves and add the Cloudera apt repository. This is a separate playbook because it is likely to only be run once.

`ansible-playbook setup-cluster.yml -i hosts`

This script will perform all the Spark and HDFS configurations. When running this script multiple times, changes in the config files will cause respective services to restart. HDFS NameNode will only be formatted the first time.

## Setup VPN

This is an optional step that will allow you to use the `pptp` vpn client to connect to the master node of the spark cluster and use the (web interfaces for monitoring and instrumentation)[http://spark.apache.org/docs/latest/monitoring.html].

Make sure that before the *Setup Cluster* section was run, the `all.yml` group_vars file contained your local network's subnet in the `allow_vpn_from_subnet` variable.

Copy `roles/pptpd/files/chap-secrets.example` to `roles/pptpd/files/chap-secrets` and make up a `username` and `password` for the cluster's VPN.

`ansible-playbook setup-vpn.yml -i hosts`

This playbook installs pptpd on the master node.

Now add the `ec2.internal` domain to your local VPN network interface. This should allow the DNS resolution to work on the EC2 cluster instances.

Now you can use the (`pptp` client)[https://help.ubuntu.com/community/VPNClient] to connect to the cluster VPN.

## Stop/Start

- `start-cluster.yml`
- `stop-cluster.yml`
- `start-cluster.yml`
- `terminate-cluster.yml`
- `restart-spark.yml`

_Note_: the `terminate-cluster.yml` playbook will have no effect on a stopped cluster. You will have to bring a stopped cluster up to a running state for it to have the desired effect.

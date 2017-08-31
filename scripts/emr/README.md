# GeoTrellis EMR

This directory contains a make file to spin up an EMR cluster using [terraform](https://github.com/hashicorp/terraform).

- [Requirements](#requirements)
- [Makefile](#makefile)
- [Terraform setup](#terraform-setup)
- [Running](#running)

## Requirements

You need to [install terraform, master branch build until release with spot instances support would be published](#terraform) and [jq](https://stedolan.github.io/jq/) to parse terraform json configuration (can be installed via brew on Mac OS).

## Settings

[variables.tf.json](terraform/variables.tf.json) contains a set of variables which should be specified to make EMR deployment work.

## Makefile

| Command          | Description
|------------------|------------------------------------------------------------|
|terraform-init    |`terraform` init, if it's the first run                     |
|create-cluster    |Create EMR cluster with configurations                      |
|destroy-cluster   |Destroy EMR cluster                                         |
|create-cluster    |Create EMR cluster with configurations                      |
|proxy             |Create SOCKS proxy for active cluster                       |
|ssh               |SSH into cluster master                                     |
|cleanup-zeppelin  |Cleanups all GeoTrellis jars from Zeppelin classpath        |
|restart-zeppelin  |Restart Zeppelin                                            |
|stop-zeppelin     |Stop Zeppelin                                               |
|start-zeppelin    |Start Zeppelin                                              |

## Terraform setup

(Instructions are written by @fosskers)

Terraform has the concept of *providers*, which are web services like AWS that can
provide *resources*. A *resource* is Terraform-lingo for anything that can "do work"
on the web. In the case of AWS, these are things like EC2 instances, load balancers,
and **EMR clusters**. Terraform is great because it helps us create/destroy these resources
with very little config, and it also helps us keep track of their state.

Providers used to be built-in to Terraform. As of a recent version, they've been
removed and are instead provided by "plugins" that are usually automatically installable.
For instance, we will use the `terraform-provider-aws` plugin. We can't use the automatically
installed one right now, however, because we need a side-feature that only exists in some guy's
PR. It will be merged soon, since contributors have responded to the PR and are just waiting
for a few more additions.

### Installing Terraform Core

First, remove `terraform` from your machine if you have it installed already.
Now, we need the most recent terraform, available on their `master` branch:

```
git clone git@github.com:hashicorp/terraform.git
```

Make sure to set up your `$GOPATH` properly as explained in their README. `make dev` will
create an executable for you that lives in your `$GOPATH`. If you can do `terraform -v`
and see something like:

```
Terraform v0.10.0-dev (870617d22df3f9245889a75c63119b94057c6e48+CHANGES)
```

### Installing the AWS Provider Plugin

Follow [the README instructions](https://github.com/terraform-providers/terraform-provider-aws/)
for building the plugin manually from the current master branch. Building the plugin
will install it to the correct place in your `$GOPATH`, and it'll be automatically
visible to Terraform.

## Running

Run cluster and upload assembly on EMR master node:

```bash
make terraform-init && \
make create-cluster && \
make upload-assembly
```

Make proxy and access Zeppelin though UI:

```bash
make proxy
```

![Zeppelin Welcome](./images/zeppelin-welcome.png)

Create a new notebook: 

![Zeppelin GeoTrellis Notebook](./images/zeppelin-geotrellis-notebook.png)

Go into interpreters tab:

![Zeppelin interpreters](./images/zeppelin-interpreters.png)

Edit spark interpreter, and add GeoTrellis jar into deps (make sure that you uploaded GeoTrellis 
jar via `make upload-assembly` into `/tmp/geotrellis-spark-etl-assembly-1.2.0-SNAPSHOT.jar` directory):

![Zeppelin interpreter edit](./images/zeppelin-interpreter-edit.png)

After that GeoTrellis deps can be imported:

![Zeppelin GeoTrellis example](./images/zeppelin-geotrellis-example.png)
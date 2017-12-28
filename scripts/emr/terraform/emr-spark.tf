# Marks AWS as a resource provider.
provider "aws" {
  access_key = "${var.access_key}"
  secret_key = "${var.secret_key}"
  region     = "${var.region}"
}

# `aws_emr_cluster` is built-in to Terraform. We name ours `emr-spark-cluster`.
resource "aws_emr_cluster" "emr-spark-cluster" {
  name          = "EMR GeoTrellis - ${var.user}"
  release_label = "emr-5.8.0"

  # This it will work if only `Spark` is named here, but booting the cluster seems
  # to be much faster when `Hadoop` is included. Ingests, etc., will succeed
  # even if `Hadoop` is missing here.
  applications = ["Hadoop", "Spark", "Ganglia", "Zeppelin"]

  ec2_attributes {
    key_name         = "${var.key_name}"
    instance_profile = "EMR_EC2_DefaultRole" # This seems to be the only necessary field.
  }

  # MASTER group must have an instance_count of 1.
  # `xlarge` seems to be the smallest type they'll allow (large didn't work).
  instance_group {
    bid_price      = "${var.spot_price}"
    instance_count = 1
    instance_role  = "MASTER"
    instance_type  = "m3.xlarge"
    name           = "EmrGeoTrellisZeppelin-MasterGroup"
  }

  instance_group {
    bid_price      = "${var.spot_price}"
    instance_count = "${var.worker_count}"
    instance_role  = "CORE"
    instance_type  = "m3.xlarge"
    name           = "EmrGeoTrellisZeppelin-CoreGroup"
  }

  # Location to dump logs
  log_uri = "${var.s3_uri}"

  # These can be altered freely, they don't affect the config.
  tags {
    name = "GeoTrellis Zeppelin Demo Spark Cluster"
    role = "EMR_DefaultRole"
    env  = "env"
  }

  # This is the effect of `aws emr create-cluster --use-default-roles`.
  service_role = "EMR_DefaultRole"

  # Spark YARN config to S3 for the ECS cluster to grab.
  provisioner "remote-exec" {
    # Necessary to massage settings the way AWS wants them.
    connection {
      type        = "ssh"
      user        = "hadoop"
      host        = "${aws_emr_cluster.emr-spark-cluster.master_public_dns}"
      private_key = "${file("${var.pem_path}")}"
    }
  }

  provisioner "file" {
    source      = "bootstrap.sh"
    destination = "/tmp/bootstrap.sh"
    connection {
      type        = "ssh"
      user        = "hadoop"
      host        = "${aws_emr_cluster.emr-spark-cluster.master_public_dns}"
      private_key = "${file("${var.pem_path}")}"
    }
  }

  provisioner "file" {
    source      = "boilerplate.js"
    destination = "/tmp/bp.js"
    connection {
      type        = "ssh"
      user        = "hadoop"
      host        = "${aws_emr_cluster.emr-spark-cluster.master_public_dns}"
      private_key = "${file("${var.pem_path}")}"
    }
  }

  provisioner "remote-exec" {
    inline=[
      "chmod +x /tmp/bootstrap.sh",
      "/tmp/bootstrap.sh ${var.access_key} ${var.secret_key} ${var.s3_notebook_bucket} ${var.s3_notebook_prefix} ${var.install_jupyter}"
    ]
    connection {
      type        = "ssh"
      user        = "hadoop"
      host        = "${aws_emr_cluster.emr-spark-cluster.master_public_dns}"
      private_key = "${file("${var.pem_path}")}"
    }
  }
}

# Pipable to other programs.
output "emrID" {
  value = "${aws_emr_cluster.emr-spark-cluster.id}"
}

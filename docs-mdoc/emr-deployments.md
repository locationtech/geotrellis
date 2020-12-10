---
id: emr_deployment
title: GeoTrellis EMR Deployment with GDAL
sidebar_label: GeoTrellis EMR Deployment with GDAL
---

GeoTrellis can be used with _EMR 6_. However, if there is a need in GDAL bindings usage there can be some certain complexities.
This section describes the entire _EMR 6_ deployment and GDAL installation in a way that would satisfy GeoTrellis GDAL requirements.

Starting _GeoTrellis_ `3.4.2`, _GeoTrellis_ is compatible only with the GDAL`3.1 +` versions.

## Installing GDAL on EMR

To properly install GDAL on EMR it is neccesary to install GDAL on all the nodes using the [EMR bootstrap script](#emr-bootsrap-script-gdal-installation-on-emr). And to make these installed GDAL dependencies
available for the run-time shared library loader (to set a proper `LD_LIBRARY_PATH` environment variable).

If you're a Scala user and use [SBT](https://www.scala-sbt.org/) for the development work, there a [GeoTrellis Spark Job](https://github.com/geotrellis/geotrellis-spark-job.g8) template project that contains a template for the GeoTrellis project and configures the 
[SBT Lighter plugin](#configuration-for-sbt-lighter-plugin) to install GDAL.

To summarize, the whole process can be represented the following way:

1. Install _GDAL_
2. Set Spark `LD_LIBRARY_PATH`. In this case `GeoTrellis GDAL` would be able to use the installed `GDAL`

### EMR Bootsrap script (GDAL installation on EMR)

The EMR bootstrap script for GeoTrellis + GDAL can be downloaded at: [s3://geotrellis-test/emr-gdal/bootstrap.sh](s3://geotrellis-test/emr-gdal/bootstrap.sh).

It installs GDAL 3.1.2 (by default) through conda and sets all appropriate env variables in case 
it would be required to log in on the cluster nodes and experiment with the `spark-shell`.

#### spark-defaults settings

To make GDAL bindings work it is neccesary to pass the `LD_LIBRARY_PATH` variable into the Spark task.
To set `LD_LIBRARY_PATH` for spark, through the `spark-defaults` settings it is possible to do the following:

```json
{
  "Classification": "spark-defaults",
  "Properties": {
    "spark.yarn.appMasterEnv.LD_LIBRARY_PATH": "/usr/local/miniconda/lib/:/usr/local/lib",
    "spark.executorEnv.LD_LIBRARY_PATH": "/usr/local/miniconda/lib/:/usr/local/lib"
  }
}
```

These parameters above can be also set / overrided via the `spark-submit --conf` settings.

### Configuration for SBT Lighter Plugin

Before looking into this section, you may be interested in the [GeoTrellis Spark Job](https://github.com/geotrellis/geotrellis-spark-job.g8) template project as well.

The SBT Lighter Plugin configuration example:

```scala
/** addSbtPlugin("net.pishen" % "sbt-lighter" % "1.2.0") */

import sbtlighter._

LighterPlugin.disable

lazy val EMRSettings = LighterPlugin.baseSettings ++ Seq(
  sparkEmrRelease := "emr-6.0.0",
  sparkAwsRegion := "us-east-1",
  sparkEmrApplications := Seq("Hadoop", "Spark", "Ganglia", "Zeppelin"),
  sparkEmrBootstrap := List(
    BootstrapAction(
      "Install GDAL dependencies",
      "s3://geotrellis-test/emr-gdal/bootstrap.sh",
      "3.1.2"
    )
  ),
  sparkS3JarFolder := "s3://geotrellis-test/rastersource-performance/jars",
  sparkInstanceCount := 11,
  sparkMasterType := "i3.xlarge",
  sparkCoreType := "i3.xlarge",
  sparkMasterPrice := Some(0.5),
  sparkCorePrice := Some(0.5),
  sparkClusterName := s"GeoTrellis VLM Performance ${sys.env.getOrElse("USER", "<anonymous user>")}",
  sparkEmrServiceRole := "EMR_DefaultRole",
  sparkInstanceRole := "EMR_EC2_DefaultRole",
  sparkMasterEbsSize := None, // Some(64)
  sparkCoreEbsSize := None, // Some(64)
  sparkJobFlowInstancesConfig := sparkJobFlowInstancesConfig.value.withEc2KeyName("geotrellis-emr"),
  sparkS3LogUri := Some("s3://geotrellis-test/rastersource-performance/logs"),
  sparkEmrConfigs := List(
    EmrConfig("spark").withProperties(
      "maximizeResourceAllocation" -> "false" // be careful with setting this param to true
    ),
    EmrConfig("spark-defaults").withProperties(
      "spark.driver.maxResultSize" -> "4200M",
      "spark.dynamicAllocation.enabled" -> "true",
      "spark.shuffle.service.enabled" -> "true",
      "spark.shuffle.compress" -> "true",
      "spark.shuffle.spill.compress" -> "true",
      "spark.rdd.compress" -> "true",
      "spark.driver.extraJavaOptions" ->"-XX:+UseParallelGC -XX:+UseParallelOldGC -XX:OnOutOfMemoryError='kill -9 %p'",
      "spark.executor.extraJavaOptions" -> "-XX:+UseParallelGC -XX:+UseParallelOldGC -XX:OnOutOfMemoryError='kill -9 %p'",
      "spark.yarn.appMasterEnv.LD_LIBRARY_PATH" -> "/usr/local/miniconda/lib/:/usr/local/lib",
      "spark.executorEnv.LD_LIBRARY_PATH" -> "/usr/local/miniconda/lib/:/usr/local/lib"
    ),
    EmrConfig("yarn-site").withProperties(
      "yarn.resourcemanager.am.max-attempts" -> "1",
      "yarn.nodemanager.vmem-check-enabled" -> "false",
      "yarn.nodemanager.pmem-check-enabled" -> "false"
    )
  )
)
```

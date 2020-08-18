/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt._

object Version {
  val geotools    = "23.2"
  val spire       = "0.13.0"
  val accumulo    = "1.9.3"
  val cassandra   = "3.7.2"
  val hbase       = "2.2.5"
  val geomesa     = "2.3.1"
  val geowave     = "0.9.3"
  val hadoop      = "2.8.5"
  val spark       = "2.4.4"
  val gdal        = "3.1.0"
  val gdalWarp    = "1.1.0"

  val previousVersion = "3.4.0"
}
import sbt.Keys._

object Dependencies {
  private def ver(for211: String, for212: String) = Def.setting {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => for211
      case Some((2, 12)) => for212
      case _ => sys.error("not good")
    }
  }

  def monocle(module: String) = Def.setting {
    "com.github.julien-truffaut" %% s"monocle-$module" % ver("1.5.1-cats", "2.1.0").value
  }

  def cats(module: String) = Def.setting {
    module match {
      case "core"   => "org.typelevel" %% s"cats-$module" % ver("1.6.1", "2.1.1").value
      case "effect" => "org.typelevel" %% s"cats-$module" % ver("1.3.1", "2.1.3").value
    }
  }

  def circe(module: String) = Def.setting {
    "io.circe" %% s"circe-$module" % ver("0.11.1", "0.13.0").value
  }

  def fs2(module: String) = Def.setting {
    "co.fs2" %% s"fs2-$module" % ver("1.0.5", "2.4.2").value
  }

  val scalaURI = Def.setting {
    "io.lemonlabs" %% "scala-uri" % ver("1.4.10", "1.5.1").value
  }

  val sparkCore           = "org.apache.spark"           %% "spark-core"               % Version.spark
  val sparkSql            = "org.apache.spark"           %% "spark-sql"                % Version.spark
  val pureconfig          = "com.github.pureconfig"      %% "pureconfig"               % "0.13.0"
  val logging             = "org.log4s"                  %% "log4s"                    % "1.8.2"
  val scalatest           = "org.scalatest"              %% "scalatest"                % "3.2.0"
  val scalacheck          = "org.scalacheck"             %% "scalacheck"               % "1.14.3"
  val scalaXml            = "org.scala-lang.modules"     %% "scala-xml"                % "1.3.0"
  val jts                 = "org.locationtech.jts"        % "jts-core"                 % "1.16.1"
  val proj4j              = "org.locationtech.proj4j"     % "proj4j"                   % "1.1.1"
  val openCSV             = "com.opencsv"                 % "opencsv"                  % "5.2"
  val spire               = "org.spire-math"             %% "spire"                    % Version.spire
  val spireMacro          = "org.spire-math"             %% "spire-macros"             % Version.spire
  val apacheIO            = "commons-io"                  % "commons-io"               % "2.7"
  val apacheMath          = "org.apache.commons"          % "commons-math3"            % "3.6.1"
  val chronoscala         = "jp.ne.opt"                  %% "chronoscala"              % "0.3.2"
  val awsSdkS3            = "software.amazon.awssdk"      % "s3"                       % "2.13.74"
  val hadoopClient        = "org.apache.hadoop"           % "hadoop-client"            % Version.hadoop
  val avro                = "org.apache.avro"             % "avro"                     % "1.7.7"
  val parserCombinators   = "org.scala-lang.modules"     %% "scala-parser-combinators" % "1.1.2"
  val jsonSchemaValidator = "com.networknt"               % "json-schema-validator"    % "0.1.23"
  val scaffeine           = "com.github.blemale"         %% "scaffeine"                % "4.0.1"
  val accumuloCore        = "org.apache.accumulo"         % "accumulo-core"            % Version.accumulo
  val sl4jnop             = "org.slf4j"                   % "slf4j-nop"                % "1.7.25"
  val cassandraDriverCore = "com.datastax.cassandra"      % "cassandra-driver-core"    % Version.cassandra
  val geomesaJobs              = "org.locationtech.geomesa" %% "geomesa-jobs"               % Version.geomesa
  val geomesaAccumuloJobs      = "org.locationtech.geomesa" %% "geomesa-accumulo-jobs"      % Version.geomesa
  val geomesaAccumuloDatastore = "org.locationtech.geomesa" %% "geomesa-accumulo-datastore" % Version.geomesa
  val geomesaUtils             = "org.locationtech.geomesa" %% "geomesa-utils"              % Version.geomesa

  val geotoolsCoverage    = "org.geotools"                 % "gt-coverage"             % Version.geotools
  val geotoolsHsql        = "org.geotools"                 % "gt-epsg-hsql"            % Version.geotools
  val geotoolsMain        = "org.geotools"                 % "gt-main"                 % Version.geotools
  val geotoolsReferencing = "org.geotools"                 % "gt-referencing"          % Version.geotools
  val geotoolsGeoTiff     = "org.geotools"                 % "gt-geotiff"              % Version.geotools
  val geotoolsShapefile   = "org.geotools"                 % "gt-shapefile"            % Version.geotools

  // This is one finicky dependency. Being explicit in hopes it will stop hurting Travis.
  val jaiCore             = "javax.media" % "jai_core"     % "1.1.3" from "https://repo.osgeo.org/repository/release/javax/media/jai_core/1.1.3/jai_core-1.1.3.jar"

  val geowaveRaster       = "mil.nga.giat"                 % "geowave-adapter-raster"     % Version.geowave
  val geowaveVector       = "mil.nga.giat"                 % "geowave-adapter-vector"     % Version.geowave
  val geowaveStore        = "mil.nga.giat"                 % "geowave-core-store"         % Version.geowave
  val geowaveGeotime      = "mil.nga.giat"                 % "geowave-core-geotime"       % Version.geowave
  val geowaveAccumulo     = "mil.nga.giat"                 % "geowave-datastore-accumulo" % Version.geowave

  val scalaArm            = "com.jsuereth"                %% "scala-arm"               % "2.0"

  val kryoSerializers     = "de.javakaffee"                % "kryo-serializers"        % "0.38"
  val kryoShaded          = "com.esotericsoftware"         % "kryo-shaded"             % "3.0.3"

  val hbaseCommon         = "org.apache.hbase" % "hbase-common"         % Version.hbase
  val hbaseClient         = "org.apache.hbase" % "hbase-client"         % Version.hbase
  val hbaseMapReduce      = "org.apache.hbase" % "hbase-mapreduce"      % Version.hbase
  val hbaseServer         = "org.apache.hbase" % "hbase-server"         % Version.hbase
  val hbaseHadoopCompact  = "org.apache.hbase" % "hbase-hadoop-compat"  % Version.hbase
  val hbaseHadoop2Compact = "org.apache.hbase" % "hbase-hadoop2-compat" % Version.hbase
  val hbaseMetrics        = "org.apache.hbase" % "hbase-metrics"        % Version.hbase
  val hbaseMetricsApi     = "org.apache.hbase" % "hbase-metrics-api"    % Version.hbase
  val hbaseZooKeeper      = "org.apache.hbase" % "hbase-zookeeper"      % Version.hbase

  val jacksonCoreAsl      = "org.codehaus.jackson"         % "jackson-core-asl"        % "1.9.13"

  val uzaygezenCore       = "com.google.uzaygezen"         % "uzaygezen-core"          % "0.2"

  val scalaj              = "org.scalaj"                  %% "scalaj-http"             % "2.4.2"

  val scalapbRuntime      = "com.thesamet.scalapb"        %% "scalapb-runtime"         % scalapb.compiler.Version.scalapbVersion

  val squants             = "org.typelevel"               %% "squants"                 % "1.7.0"
  val scalactic           = "org.scalactic"               %% "scalactic"               % "3.2.1"

  val gdalBindings        = "org.gdal"                     % "gdal"                    % Version.gdal
  val gdalWarp            = "com.azavea.geotrellis"        % "gdal-warp-bindings"      % Version.gdalWarp

  val jacksonCore         = "com.fasterxml.jackson.core"    % "jackson-core"             % "2.6.7"
  val jacksonDatabind     = "com.fasterxml.jackson.core"    % "jackson-databind"         % "2.6.7"
  val jacksonAnnotations  = "com.fasterxml.jackson.core"    % "jackson-annotations"      % "2.6.7"
  val jacksonModuleScala  = "com.fasterxml.jackson.module" %% "jackson-module-scala"     % "2.6.7"
}

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

object Dependencies {
  val pureconfig          = "com.github.pureconfig"      %% "pureconfig"               % "0.10.2"
  val logging             = "com.typesafe.scala-logging" %% "scala-logging"            % "3.9.0"
  val scalatest           = "org.scalatest"              %% "scalatest"                % "3.0.7"
  val scalacheck          = "org.scalacheck"             %% "scalacheck"               % "1.14.0"
  val jts                 = "org.locationtech.jts"        % "jts-core"                 % "1.16.1"
  val proj4j              = "org.locationtech.proj4j"     % "proj4j"                   % "1.0.0"

  val monocleCore         = "com.github.julien-truffaut" %% "monocle-core"             % Version.monocle
  val monocleMacro        = "com.github.julien-truffaut" %% "monocle-macro"            % Version.monocle

  val openCSV             = "com.opencsv"                 % "opencsv"                  % "4.5"

  val spire               = "org.spire-math"            %% "spire"                    % Version.spire
  val spireMacro          = "org.spire-math"            %% "spire-macros"             % Version.spire

  val sprayJson           = "io.spray"                   %% "spray-json"               % Version.sprayJson

  val apacheIO            = "commons-io"                  % "commons-io"               % "2.6"
  val apacheMath          = "org.apache.commons"          % "commons-math3"            % "3.6.1"

  val chronoscala         = "jp.ne.opt"                  %% "chronoscala"              % "0.3.0"

  val awsSdkS3            = "com.amazonaws"               % "aws-java-sdk-s3"          % "1.11.535"

  val catsCore            = "org.typelevel"              %% "cats-core"                % "1.6.0"
  val catsEffect          = "org.typelevel"              %% "cats-effect"              % "1.2.0"

  val fs2Core             = "co.fs2"                     %% "fs2-core"                 % "1.0.4"
  val fs2Io               = "co.fs2"                     %% "fs2-io"                   % "1.0.4"

  val sparkCore           = "org.apache.spark"           %% "spark-core"               % Version.spark
  val sparkSQL            = "org.apache.spark"           %% "spark-sql"                % Version.spark

  val hadoopClient        = "org.apache.hadoop"           % "hadoop-client"            % Version.hadoop

  val avro                = "org.apache.avro"             % "avro"                     % "1.7.7"

  val parserCombinators   = "org.scala-lang.modules"     %% "scala-parser-combinators" % "1.1.2"

  val jsonSchemaValidator = "com.networknt"               % "json-schema-validator"    % "0.1.23"

  val scaffeine           = "com.github.blemale"         %% "scaffeine"                % "2.6.0"

  val circeCore           = "io.circe"                   %% "circe-core"               % Version.circe
  val circeGeneric        = "io.circe"                   %% "circe-generic"            % Version.circe
  val circeGenericExtras  = "io.circe"                   %% "circe-generic-extras"     % Version.circe
  val circeParser         = "io.circe"                   %% "circe-parser"             % Version.circe

  val accumuloCore        = "org.apache.accumulo"          % "accumulo-core"           % Version.accumulo

  val sl4jnop             = "org.slf4j"                    % "slf4j-nop"               % "1.7.25"

  val cassandraDriverCore = "com.datastax.cassandra"       % "cassandra-driver-core"   % Version.cassandra

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

  val jaiCore             = "javax.media"                  % "jai_core"                % "1.1.3"

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

  val scalaj              = "org.scalaj"                  %% "scalaj-http"             % "2.4.1"

  val scalapbRuntime      = "com.thesamet.scalapb"        %% "scalapb-runtime"         % scalapb.compiler.Version.scalapbVersion
}

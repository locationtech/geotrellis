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
  val geotools    = "33.0"
  val spire       = "0.17.0"
  val accumulo    = "1.10.4"
  val cassandra   = "4.17.0"
  val hbase       = "2.6.2"
  val hadoop      = "3.4.1"
  val gdal        = "3.10.1"
  val gdalWarp    = "3.10.0"

  val previousVersion = "3.6.0"
}
import sbt.Keys._

object Dependencies {
  private def ver(for212: String, for213: String) = Def.setting {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 12)) => for212
      case Some((2, 13)) => for213
      case _ => sys.error("not good")
    }
  }

  def monocle(module: String) = Def.setting {
    "com.github.julien-truffaut" %% s"monocle-$module" % "2.1.0"
  }

  def cats(module: String) = Def.setting {
    module match {
      case "effect" => "org.typelevel" %% s"cats-$module" % "3.6.1"
      case _        => "org.typelevel" %% s"cats-$module" % "2.13.0"
    }
  }

  def circe(module: String) = Def.setting {
    module match {
      case "json-schema"    => "io.circe" %% s"circe-$module" % "0.2.0"
      case "generic-extras" => "io.circe" %% s"circe-$module" % "0.14.4"
      case _                => "io.circe" %% s"circe-$module" % "0.14.12"
    }
  }

  def fs2(module: String) = Def.setting {
    "co.fs2" %% s"fs2-$module" % "3.12.0"
  }

  def apacheSpark(module: String) = Def.setting {
    "org.apache.spark"  %% s"spark-$module" % "3.5.5"
  }

  def scalaReflect(version: String) = "org.scala-lang" % "scala-reflect" % version

  val pureconfig          = "com.github.pureconfig"      %% "pureconfig"               % "0.17.8"
  val log4s               = "org.log4s"                  %% "log4s"                    % "1.10.0"
  val scalatest           = "org.scalatest"              %% "scalatest"                % "3.2.19"
  val scalacheck          = "org.scalacheck"             %% "scalacheck"               % "1.18.1"
  val scalaXml            = "org.scala-lang.modules"     %% "scala-xml"                % "2.3.0"
  val jts                 = "org.locationtech.jts"        % "jts-core"                 % "1.20.0"
  val proj4j              = "org.locationtech.proj4j"     % "proj4j"                   % "1.4.0"
  val proj4jEPSG          = "org.locationtech.proj4j"     % "proj4j-epsg"              % "1.4.0"
  val openCSV             = "com.opencsv"                 % "opencsv"                  % "5.10"
  val spire               = "org.typelevel"              %% "spire"                    % Version.spire
  val spireMacro          = "org.typelevel"              %% "spire-macros"             % Version.spire
  val apacheIO            = "commons-io"                  % "commons-io"               % "2.19.0"
  val apacheLang3         = "org.apache.commons"          % "commons-lang3"            % "3.17.0"
  val apacheMath          = "org.apache.commons"          % "commons-math3"            % "3.6.1"
  val chronoscala         = "jp.ne.opt"                  %% "chronoscala"              % "1.0.0"
  val awsSdkS3            = "software.amazon.awssdk"      % "s3"                       % "2.28.17"
  val hadoopClient        = "org.apache.hadoop"           % "hadoop-client"            % Version.hadoop
  val avro                = "org.apache.avro"             % "avro"                     % "1.11.4" // aligned with the Spark version
  val parserCombinators   = "org.scala-lang.modules"     %% "scala-parser-combinators" % "2.4.0"
  val jsonSchemaValidator = "com.networknt"               % "json-schema-validator"    % "0.1.23"
  val accumuloCore        = "org.apache.accumulo"         % "accumulo-core"            % Version.accumulo
  val sl4jnop             = "org.slf4j"                   % "slf4j-nop"                % "1.7.25"
  val logbackClassic      = "ch.qos.logback"              % "logback-classic"          % "1.2.3"
  val guava               = "com.google.guava"            % "guava"                    % "16.0.1"

  val cassandraDriverCore         = "com.datastax.oss" % "java-driver-core"          % Version.cassandra
  val cassandraDriverQueryBuilder = "com.datastax.oss" % "java-driver-query-builder" % Version.cassandra

  val scaffeine = "com.github.blemale"           %% "scaffeine" % "5.3.0"
  val caffeine  = "com.github.ben-manes.caffeine" % "caffeine"  % "3.2.0"

  val geotoolsCoverage    = "org.geotools"                 % "gt-coverage"             % Version.geotools
  val geotoolsHsql        = "org.geotools"                 % "gt-epsg-hsql"            % Version.geotools
  val geotoolsMain        = "org.geotools"                 % "gt-main"                 % Version.geotools
  val geotoolsReferencing = "org.geotools"                 % "gt-referencing"          % Version.geotools
  val geotoolsGeoTiff     = "org.geotools"                 % "gt-geotiff"              % Version.geotools
  val geotoolsShapefile   = "org.geotools"                 % "gt-shapefile"            % Version.geotools
  val geotoolsMetadata    = "org.geotools"                 % "gt-metadata"             % Version.geotools

  val scalaArm            = "com.jsuereth"                %% "scala-arm"               % "2.0"

  val kryoSerializers     = "de.javakaffee"                % "kryo-serializers"        % "0.38"
  val kryoShaded          = "com.esotericsoftware"         % "kryo-shaded"             % "3.0.3"

  val hbaseMapReduce      = "org.apache.hbase" % "hbase-mapreduce" % Version.hbase

  val woodstoxCore          = "com.fasterxml.woodstox" % "woodstox-core"          % "7.1.0"
  val stax2Api              = "org.codehaus.woodstox"  % "stax2-api"              % "4.2.2"
  val commonsConfiguration2 = "org.apache.commons"     % "commons-configuration2" % "2.11.0"
  val re2j                  = "com.google.re2j"        % "re2j"                   % "1.8"

  val jacksonCoreAsl      = "org.codehaus.jackson"         % "jackson-core-asl"        % "1.9.13"

  val uzaygezenCore       = "com.google.uzaygezen"         % "uzaygezen-core"          % "0.2"

  val scalaj              = "org.scalaj"                  %% "scalaj-http"             % "2.4.2"

  val scalapbRuntime      = "com.thesamet.scalapb"        %% "scalapb-runtime"         % scalapb.compiler.Version.scalapbVersion
  val scalapbLenses       = "com.thesamet.scalapb"        %% "lenses"                  % scalapb.compiler.Version.scalapbVersion
  val protobufJava        = "com.google.protobuf"          % "protobuf-java"           % "4.30.2"

  val squants             = "org.typelevel"               %% "squants"                 % "1.8.3"
  val scalactic           = "org.scalactic"               %% "scalactic"               % "3.2.19"

  val gdalBindings        = "org.gdal"                     % "gdal"                    % Version.gdal
  val gdalWarp            = "com.azavea.geotrellis"        % "gdal-warp-bindings"      % Version.gdalWarp

  val shapeless           = "com.chuusai"  %% "shapeless" % "2.3.13"

  // aligned with the GeoTools version
  val unitApi             = "javax.measure" % "unit-api"  % "2.2"

  val scalaURI            = "io.lemonlabs" %% "scala-uri" % "4.0.3"
  val java8Compat         = "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.1"

  // located in the OSGeo repo: https://repo.osgeo.org/repository/release/
  // 'works with' due to license issues
  val jaiCore             = "javax.media" % "jai_core"     % "1.1.3"
  val jaiCodec            = "javax.media" % "jai_codec"    % "1.1.3"
  val imageIo             = "javax.media" % "jai_imageio"  % "1.1"

  val imageioExtUtilities = "it.geosolutions.imageio-ext" % "imageio-ext-utilities" % "1.4.15"

  val worksWithDependencies = Seq(jaiCore, jaiCodec, imageIo, imageioExtUtilities).map(_ % Provided)
}

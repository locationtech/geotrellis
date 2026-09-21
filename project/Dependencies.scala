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

import sbt.*

object Version {
  val geotools    = "35.1"
  val spire       = "0.18.0" // 0.18.x is the first release cross-published for Scala 3
  val accumulo    = "2.1.4"
  val cassandra   = "4.19.3"
  val hbase       = "2.6.6"
  val hadoop      = "3.4.3"
  val gdal        = "3.12.0"
  val gdalWarp    = "3.13.0"

  val previousVersion = "3.6.0"
}
import sbt.Keys.*

object Dependencies {
  private def ver(for212: String, for213: String) = Def.setting {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 12)) => for212
      case Some((2, 13)) => for213
      case _ => sys.error("not good")
    }
  }

  def monocle(module: String) = Def.setting {
    "dev.optics" %% s"monocle-$module" % "3.3.0"
  }

  def cats(module: String) = Def.setting {
    module match {
      case "effect" => "org.typelevel" %% s"cats-$module" % "3.7.0"
      case _        => "org.typelevel" %% s"cats-$module" % "2.13.0"
    }
  }

  def circe(module: String) = Def.setting {
    module match {
      case "json-schema"    => "io.circe" %% s"circe-$module" % "0.2.0"
      case "generic-extras" => "io.circe" %% s"circe-$module" % "0.14.4"
      case _                => "io.circe" %% s"circe-$module" % "0.14.15"
    }
  }

  def fs2(module: String) = Def.setting {
    "co.fs2" %% s"fs2-$module" % "3.13.0"
  }

  /** Spark has no Scala 3 build, so Scala 3 consumes the Scala 2.13 artifacts. */
  def for3Use2_13(module: ModuleID) = Def.setting {
    if (CrossVersion.partialVersion(scalaVersion.value).exists(_._1 == 3)) module.cross(CrossVersion.for3Use2_13)
    else module
  }

  def apacheSpark(module: String) = Def.setting {
    val dep = for3Use2_13("org.apache.spark" %% s"spark-$module" % "4.0.3").value
    // Spark's 2.13 jars pull the _2.13 builds of these, while the pure Scala 3 modules
    // (proj4, store) bring the _3 builds. They are the same classes, so keep one copy.
    if (CrossVersion.partialVersion(scalaVersion.value).exists(_._1 == 3))
      dep.excludeAll(
        ExclusionRule("org.scala-lang.modules", "scala-xml_2.13"),
        ExclusionRule("org.scala-lang.modules", "scala-parser-combinators_2.13")
      )
    else dep
  }

  def scalaReflect(version: String) = "org.scala-lang" % "scala-reflect" % version

  val pureconfig          = "com.github.pureconfig"      %% "pureconfig"               % "0.17.8"
  // pureconfig's aggregate and `-generic` artifacts have no Scala 3 build; core does.
  val pureconfigCore      = "com.github.pureconfig"      %% "pureconfig-core"          % "0.17.10"
  val log4s               = "org.log4s"                  %% "log4s"                    % "1.10.0"
  val scalatest           = "org.scalatest"              %% "scalatest"                % "3.2.20"
  val scalacheck          = "org.scalacheck"             %% "scalacheck"               % "1.19.0"
  val scalaXml            = "org.scala-lang.modules"     %% "scala-xml"                % "2.4.0"
  val jts                 = "org.locationtech.jts"        % "jts-core"                 % "1.20.0"
  val proj4j              = "org.locationtech.proj4j"     % "proj4j"                   % "1.4.3"
  val proj4jEPSG          = "org.locationtech.proj4j"     % "proj4j-epsg"              % "1.4.3"
  val openCSV             = "com.opencsv"                 % "opencsv"                  % "5.12.0"
  val spire               = "org.typelevel"              %% "spire"                    % Version.spire
  val spireMacro          = "org.typelevel"              %% "spire-macros"             % Version.spire
  val apacheIO            = "commons-io"                  % "commons-io"               % "2.22.0"
  val apacheLang3         = "org.apache.commons"          % "commons-lang3"            % "3.20.0"
  val apacheMath          = "org.apache.commons"          % "commons-math3"            % "3.6.1"
  // 2.0.10 is the newest release built against Scala 3.3.x; 2.0.13 targets 3.5.1 and 2.1.0
  // targets 3.6.4, whose TASTy the 3.3 LTS compiler cannot read.
  val chronoscala         = "io.github.chronoscala"      %% "chronoscala"              % "2.0.10"
  val awsSdkS3            = "software.amazon.awssdk"      % "s3"                       % "2.46.15"
  val hadoopClient        = "org.apache.hadoop"           % "hadoop-client"            % Version.hadoop
  val avro                = "org.apache.avro"             % "avro"                     % "1.11.5" // aligned with the Spark version // 1.12.0 causes test issues; 1.13.0-SNAPSHOT works
  val parserCombinators   = "org.scala-lang.modules"     %% "scala-parser-combinators" % "2.4.0"
  val jsonSchemaValidator = "com.networknt"               % "json-schema-validator"    % "0.1.23"
  val sl4jnop             = "org.slf4j"                   % "slf4j-nop"                % "1.7.25"
  val logbackClassic      = "ch.qos.logback"              % "logback-classic"          % "1.2.3"
  val guava               = "com.google.guava"            % "guava"                    % "16.0.1"
  val zstdJni             = "com.github.luben"            % "zstd-jni"                 % "1.5.7-11"

  val accumuloCore        = "org.apache.accumulo"         % "accumulo-core"             % Version.accumulo
  val accumuloHadoopMR    = "org.apache.accumulo"         % "accumulo-hadoop-mapreduce" % Version.accumulo
  val accumuloMiniCluster = "org.apache.accumulo"         % "accumulo-minicluster"      % Version.accumulo

  val cassandraDriverCore         = "org.apache.cassandra" % "java-driver-core"          % Version.cassandra
  val cassandraDriverQueryBuilder = "org.apache.cassandra" % "java-driver-query-builder" % Version.cassandra

  val scaffeine = "com.github.blemale"           %% "scaffeine" % "5.3.0"
  val caffeine  = "com.github.ben-manes.caffeine" % "caffeine"  % "3.2.4"

  val geotoolsCoverage    = "org.geotools"                 % "gt-coverage"             % Version.geotools
  val geotoolsHsql        = "org.geotools"                 % "gt-epsg-hsql"            % Version.geotools
  val geotoolsMain        = "org.geotools"                 % "gt-main"                 % Version.geotools
  val geotoolsReferencing = "org.geotools"                 % "gt-referencing"          % Version.geotools
  val geotoolsGeoTiff     = "org.geotools"                 % "gt-geotiff"              % Version.geotools
  val geotoolsShapefile   = "org.geotools"                 % "gt-shapefile"            % Version.geotools
  val geotoolsMetadata    = "org.geotools"                 % "gt-metadata"             % Version.geotools

  val scalaArm            = "com.jsuereth"                %% "scala-arm"               % "2.0"

  // cross-published runtime type tags; `scala.reflect.runtime.universe.TypeTag` has no Scala 3 equivalent
  val izumiReflect        = "dev.zio"                     %% "izumi-reflect"           % "2.3.9"

  val kryoSerializers     = "de.javakaffee"                % "kryo-serializers"        % "0.38"
  val kryoShaded          = "com.esotericsoftware"         % "kryo-shaded"             % "3.0.3"

  val hbaseMapReduce      = "org.apache.hbase" % "hbase-mapreduce" % Version.hbase

  val woodstoxCore          = "com.fasterxml.woodstox" % "woodstox-core"          % "7.2.1"
  val stax2Api              = "org.codehaus.woodstox"  % "stax2-api"              % "4.3.0"
  val commonsConfiguration2 = "org.apache.commons"     % "commons-configuration2"   % "2.15.1"
  val re2j                  = "com.google.re2j"        % "re2j"                   % "1.8"

  val jacksonCoreAsl      = "org.codehaus.jackson"         % "jackson-core-asl"        % "1.9.13"

  val uzaygezenCore       = "com.google.uzaygezen"         % "uzaygezen-core"          % "0.2"

  val scalaj              = "org.scalaj"                  %% "scalaj-http"             % "2.4.2"

  val scalapbRuntime      = "com.thesamet.scalapb"        %% "scalapb-runtime"         % scalapb.compiler.Version.scalapbVersion
  val scalapbLenses       = "com.thesamet.scalapb"        %% "lenses"                  % scalapb.compiler.Version.scalapbVersion
  val protobufJava        = "com.google.protobuf"          % "protobuf-java"           % "4.35.1"

  val squants             = "org.typelevel"               %% "squants"                 % "1.8.3"
  val scalactic           = "org.scalactic"               %% "scalactic"               % "3.2.20"

  val gdalBindings        = "org.gdal"                     % "gdal"                    % Version.gdal
  val gdalWarp            = "com.azavea.geotrellis"        % "gdal-warp-bindings"      % Version.gdalWarp

  // aligned with the GeoTools version
  val unitApi             = "javax.measure" % "unit-api"  % "2.2"

  val scalaURI            = "io.lemonlabs" %% "scala-uri" % "4.0.3"
  val java8Compat         = "org.scala-lang.modules" %% "scala-java8-compat" % "1.0.2"

  // located in the OSGeo repo: https://repo.osgeo.org/repository/release/
  // 'works with' due to license issues
  val jaiCore             = "javax.media" % "jai_core"     % "1.1.3"
  val jaiCodec            = "javax.media" % "jai_codec"    % "1.1.3"
  val imageIo             = "javax.media" % "jai_imageio"  % "1.1"

  val imageioExtUtilities = "it.geosolutions.imageio-ext" % "imageio-ext-utilities" % "2.1.0"

  val worksWithDependencies = Seq(jaiCore, jaiCodec, imageIo, imageioExtUtilities).map(_ % Provided)
}

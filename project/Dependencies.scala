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
  val typesafeConfig = "com.typesafe"        % "config"           % "1.3.1"
  val logging       = "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
  val scalatest     = "org.scalatest"       %%  "scalatest"      % "3.0.1"
  val scalacheck    = "org.scalacheck"      %% "scalacheck"      % "1.13.4"
  val jts           = "com.vividsolutions"  %  "jts-core"        % "1.14.0"

  val monocleCore   = "com.github.julien-truffaut" %% "monocle-core"    % Version.monocle
  val monocleMacro  = "com.github.julien-truffaut" %% "monocle-macro"   % Version.monocle

  val openCSV       = "com.opencsv" % "opencsv" % "3.8"

  val akkaKernel    = "com.typesafe.akka" %% "akka-kernel"  % Version.akka
  val akkaRemote    = "com.typesafe.akka" %% "akka-remote"  % Version.akka
  val akkaActor     = "com.typesafe.akka" %% "akka-actor"   % Version.akka
  val akkaCluster   = "com.typesafe.akka" %% "akka-cluster" % Version.akka

  val spire         = "org.spire-math" %% "spire" % "0.13.0"

  val sprayJson     = "io.spray"        %% "spray-json"    % Version.sprayJson

  val apacheMath    = "org.apache.commons" % "commons-math3" % "3.6.1"

  val jettyWebapp   = "org.eclipse.jetty" % "jetty-webapp" % "9.4.0.M1"
  val jerseyBundle  = "com.sun.jersey"    % "jersey-bundle" % "1.19.2"
  val slf4jApi      = "org.slf4j"         % "slf4j-api" % "1.7.22"
  val asm           = "asm"               % "asm"       % "3.3.1"

  val slick         = "com.typesafe.slick" %% "slick"      % "3.1.1"
  val postgresql    = "postgresql"         % "postgresql"  % "9.1-901.jdbc4"

  val caliper       = ("com.google.code.caliper" % "caliper" % "1.0-SNAPSHOT"
    from "http://plastic-idolatry.com/jars/caliper-1.0-SNAPSHOT.jar")

  val chronoscala   = "jp.ne.opt" %% "chronoscala" % "0.1.0"

  val awsSdkS3      = "com.amazonaws" % "aws-java-sdk-s3" % "1.11.70"

  val scalazStream  = "org.scalaz.stream" %% "scalaz-stream" % "0.8.6a"

  val sparkCore     = "org.apache.spark" %% "spark-core" % Version.spark
  val hadoopClient  = "org.apache.hadoop" % "hadoop-client" % Version.hadoop

  val avro          = "org.apache.avro" % "avro" % "1.8.1"

  val slickPG      = "com.github.tminglei" %% "slick-pg" % "0.14.4"

  val pdal         = "io.pdal" %% "pdal" % "1.4.0"

  val parserCombinators = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.5"
}

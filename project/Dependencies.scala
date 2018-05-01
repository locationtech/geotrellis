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
  val typesafeConfig      = "com.typesafe"                %  "config"                  % "1.3.1"
  val logging             = "com.typesafe.scala-logging" %% "scala-logging"            % "3.5.0"
  val scalatest           = "org.scalatest"              %%  "scalatest"               % "3.0.4"
  val scalacheck          = "org.scalacheck"             %% "scalacheck"               % "1.13.5"
  val jts                 = "com.vividsolutions"          %  "jts-core"                % "1.14.0"

  val monocleCore         = "com.github.julien-truffaut" %% "monocle-core"             % Version.monocle
  val monocleMacro        = "com.github.julien-truffaut" %% "monocle-macro"            % Version.monocle

  val openCSV             = "com.opencsv"                 % "opencsv"                  % "3.9"

  val spire               = "org.spire-math"             %% "spire"                    % Version.spire
  val spireMacro          = "org.spire-math"             %% "spire-macros"             % Version.spire

  val sprayJson           = "io.spray"                   %% "spray-json"               % Version.sprayJson

  val apacheMath          = "org.apache.commons"          % "commons-math3"            % "3.6.1"

  val chronoscala         = "jp.ne.opt"                  %% "chronoscala"              % "0.1.3"

  val awsSdkS3            = "com.amazonaws"               % "aws-java-sdk-s3"          % "1.11.143"

  val catsCore            = "org.typelevel"              %% "cats-core"                % "1.0.1"
  val catsEffect          = "org.typelevel"              %% "cats-effect"              % "0.10.1"

  val fs2Core             = "co.fs2"                     %% "fs2-core"                 % "0.10.3"
  val fs2Io               = "co.fs2"                     %% "fs2-io"                   % "0.10.3"

  val sparkCore           = "org.apache.spark"           %% "spark-core"               % Version.spark
  val hadoopClient        = "org.apache.hadoop"           % "hadoop-client"            % Version.hadoop

  val avro                = "org.apache.avro"             % "avro"                     % "1.8.2"

  val slickPG             = "com.github.tminglei"        %% "slick-pg"                 % "0.15.0"

  val parserCombinators   = "org.scala-lang.modules"     %% "scala-parser-combinators" % "1.0.6"

  val jsonSchemaValidator = "com.networknt"               % "json-schema-validator"    % "0.1.7"

  val scaffeine           = "com.github.blemale"         %% "scaffeine"                % "2.2.0"

  val circeCore           = "io.circe"                   %% "circe-core"               % Version.circe
  val circeGeneric        = "io.circe"                   %% "circe-generic"            % Version.circe
  val circeGenericExtras  = "io.circe"                   %% "circe-generic-extras"     % Version.circe
  val circeParser         = "io.circe"                   %% "circe-parser"             % Version.circe
}

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
  val resolutionRepos = Seq(
    "Local Maven Repository"  at "file://" + Path.userHome.absolutePath + "/.m2/repository",
    "NL4J Repository"         at "http://nativelibs4java.sourceforge.net/maven/",
    "maven2 dev repository"   at "http://download.java.net/maven/2",
    "Typesafe Repo"           at "http://repo.typesafe.com/typesafe/releases/",
    "spray repo"              at "http://repo.spray.io/",
    "sonatypeSnapshots"       at "http://oss.sonatype.org/content/repositories/snapshots",
    "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases"
  )
  val typesafeConfig = "com.typesafe"        % "config"           % "1.2.1"
  val logging       = "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2"
  val scalatest     = "org.scalatest"       %%  "scalatest"      % "2.2.4"
  val scalacheck    = "org.scalacheck"      %% "scalacheck"      % "1.11.1"
  val sigar         = "org.hyperic"         %  "sigar"           % "1.6.4"
  val jts           = "com.vividsolutions"  %  "jts"             % "1.13"

  val monocleCore   = "com.github.julien-truffaut" %% "monocle-core"    % Version.monocle
  val monocleMacro  = "com.github.julien-truffaut" %% "monocle-macro"   % Version.monocle

  val openCSV       = "net.sf.opencsv" % "opencsv"      % "2.3"

  val akkaKernel    = "com.typesafe.akka" %% "akka-kernel"  % Version.akka
  val akkaRemote    = "com.typesafe.akka" %% "akka-remote"  % Version.akka
  val akkaActor     = "com.typesafe.akka" %% "akka-actor"   % Version.akka
  val akkaCluster   = "com.typesafe.akka" %% "akka-cluster" % Version.akka

  val spire         = "org.spire-math" %% "spire" % "0.7.5"

  val sprayClient   = "io.spray"        %% "spray-client"  % Version.spray
  val sprayRouting  = "io.spray"        %% "spray-routing" % Version.spray
  val sprayTestkit  = "io.spray"        %% "spray-testkit" % Version.spray
  val sprayCan      = "io.spray"        %% "spray-can"     % Version.spray
  val sprayHttpx    = "io.spray"        %% "spray-httpx"   % Version.spray
  val sprayJson     = "io.spray"        %% "spray-json"    % "1.3.1"

  val apacheMath    = "org.apache.commons" % "commons-math3" % "3.2"

  val jettyWebapp   = "org.eclipse.jetty" % "jetty-webapp" % "8.1.0.RC4"
  val jerseyBundle  = "com.sun.jersey"    % "jersey-bundle" % "1.11"
  val slf4jApi      = "org.slf4j"         % "slf4j-api" % "1.6.0"
  val asm           = "asm"               % "asm"       % "3.3.1"
  
  val slick         = "com.typesafe.slick" %% "slick"      % "2.1.0"
  val postgresql    = "postgresql"         % "postgresql"  % "9.1-901.jdbc4"

  val caliper       = ("com.google.code.caliper" % "caliper" % "1.0-SNAPSHOT"
    from "http://plastic-idolatry.com/jars/caliper-1.0-SNAPSHOT.jar")

  val nscalaTime    = "com.github.nscala-time" %% "nscala-time" % "1.6.0"

  val awsSdkS3      = "com.amazonaws" % "aws-java-sdk-s3" % "1.9.34"

  val scalazStream  = "org.scalaz.stream" %% "scalaz-stream" % "0.7a"
}

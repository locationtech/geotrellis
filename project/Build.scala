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

// spark hadoop env variable
import scala.util.Properties

import sbt._
import sbt.Keys._

// sbt-assembly
import sbtassembly.Plugin._
import AssemblyKeys._

object Info {
  val description =
    "GeoTrellis is an open source geographic data processing engine for high performance applications."
  val url = "http://geotrellis.github.io"
  val tags = Seq("maps", "gis", "geographic", "data", "raster", "processing")
}

object GeotrellisBuild extends Build {
  import Dependencies._

  val vectorBenchmarkKey = AttributeKey[Boolean]("vectorJavaOptionsPatched")
  val gdalBenchmarkKey = AttributeKey[Boolean]("gdalJavaOptionsPatched")
  val benchmarkKey = AttributeKey[Boolean]("javaOptionsPatched")

  // Default settings
  override lazy val settings =
    super.settings ++
    Seq(
      shellPrompt := { s => Project.extract(s).currentProject.id + " > " },
      version := Version.geotrellis,
      scalaVersion := Version.scala,
      organization := "com.azavea.geotrellis",

      // disable annoying warnings about 2.10.x
      conflictWarning in ThisBuild := ConflictWarning.disable,
      scalacOptions ++=
        Seq("-deprecation",
          "-unchecked",
          "-Yinline-warnings",
          "-language:implicitConversions",
          "-language:postfixOps",
          "-language:existentials",
          "-feature"),

      publishMavenStyle := true,

      publishTo <<= version { (v: String) =>
        val nexus = "https://oss.sonatype.org/"
        if (v.trim.endsWith("SNAPSHOT"))
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
      },

      publishArtifact in Test := false,

      pomIncludeRepository := { _ => false },
      licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
      homepage := Some(url(Info.url)),

      pomExtra := (

        <scm>
          <url>git@github.com:geotrellis/geotrellis.git</url>
          <connection>scm:git:git@github.com:geotrellis/geotrellis.git</connection>
        </scm>
        <developers>
          <developer>
            <id>joshmarcus</id>
            <name>Josh Marcus</name>
            <url>http://github.com/joshmarcus/</url>
          </developer>
          <developer>
            <id>lossyrob</id>
            <name>Rob Emanuele</name>
            <url>http://github.com/lossyrob/</url>
          </developer>
        </developers>)
    )

  val defaultAssemblySettings =
    assemblySettings ++
    Seq(
      test in assembly := {},
      mergeStrategy in assembly <<= (mergeStrategy in assembly) {
        (old) => {
          case "reference.conf" => MergeStrategy.concat
          case "application.conf" => MergeStrategy.concat
          case "META-INF/MANIFEST.MF" => MergeStrategy.discard
          case "META-INF\\MANIFEST.MF" => MergeStrategy.discard
          case _ => MergeStrategy.first
        }
      },
      resolvers ++= resolutionRepos
    )

  // Project: root
  lazy val root =
    Project("root", file("."))
      .aggregate(raster, rasterTest)

  // Project: macros
  lazy val macros =
    Project("macros", file("macros"))
      .settings(macrosSettings: _*)

  lazy val macrosSettings = Seq(
    name := "geotrellis-macros",
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full),
    libraryDependencies ++= Seq(
      scalaReflect,
      "org.scalamacros" %% "quasiquotes" % "2.0.1"
    ),
    resolvers += Resolver.sonatypeRepo("snapshots")
  )

  // Project: vector
  lazy val vector =
    Project("vector", file("vector"))
      .settings(name := "geotrellis-vector")
      .settings(libraryDependencies ++=
        Seq(
          jts,
          sprayJson,
          sprayHttpx,
          apacheMath
        )
      )
      .settings(defaultAssemblySettings: _*)
      .dependsOn(proj4)

  // Project: vector-test
  lazy val vectorTest =
    Project("vector-test", file("vector-test"))
      .settings(name := "geotrellis-vector-test")
      .settings(libraryDependencies ++=
        Seq(
          scalatest   % "test",
          scalacheck  % "test"
        )
      )
      .dependsOn(vector, testkit)

  // Project: proj4
  lazy val proj4 =
    Project("proj4", file("proj4"))
      .settings(proj4Settings: _*)

  lazy val proj4Settings =
    Seq(
      name := "geotrellis-proj4",
      libraryDependencies ++= Seq(
        "junit" % "junit" % "3.8.1" % "test",
        "com.novocode" % "junit-interface" % "0.9" % "test"
      )
    )

  // Project: raster
  lazy val raster =
    Project("raster", file("raster"))
      .dependsOn(macros)
      .dependsOn(vector)
      .settings(rasterSettings: _*)

  lazy val rasterSettings =
    Seq(
      name := "geotrellis-raster",
      parallelExecution := false,
      fork in test := false,
      javaOptions in run += "-Xmx2G",
      scalacOptions in compile ++=
        Seq("-optimize"),
      libraryDependencies ++= Seq(
        "com.typesafe" % "config" % "1.2.1",
        scalaReflect,
        jts,
        jacksonCore,
        jacksonMapper,
        spire,
        monocleCore,
        monocleMacro,
        sprayClient // for reading args from URLs,
      )
    ) ++
    defaultAssemblySettings

  // Project: raster-test
  lazy val rasterTest =
    Project("raster-test", file("raster-test"))
      .dependsOn(raster, testkit)
      .settings(rasterTestSettings: _*)

  lazy val rasterTestSettings =
    Seq(
      name := "geotrellis-raster-test",
      parallelExecution := false,
      fork in test := false,
      javaOptions in run += "-Xmx2G",
      scalacOptions in compile ++=
        Seq("-optimize"),
      libraryDependencies ++= Seq(
        scalatest % "test",
        scalacheck  % "test",
        spire % "test",
        sprayClient % "test",
        sprayRouting % "test"
      )
    ) ++
    defaultAssemblySettings

  // Project: engine
  lazy val engine =
    Project("engine", file("engine"))
      .dependsOn(raster)
      .settings(engineSettings:_*)

  lazy val engineSettings =
    Seq(
      name := "geotrellis-engine",
      scalacOptions in compile ++=
        Seq("-optimize"),
      libraryDependencies ++= Seq(
        scalatest % "test",
        scalaReflect,
        akkaKernel,
        akkaRemote,
        akkaActor,
        akkaCluster,
        jacksonCore,
        jacksonMapper,
        spire,
        monocleCore,
        monocleMacro,
        sprayClient // for reading args from URLs,
      )
    ) ++
    defaultAssemblySettings

  // Project: engine
  lazy val engineTest =
    Project("engine-test", file("engine-test"))
      .dependsOn(engine, testkit)
      .settings(engineTestSettings:_*)

  lazy val engineTestSettings =
    Seq(
      name := "geotrellis-engine-test",
      parallelExecution := false,
      fork in test := false,
      javaOptions in run += "-Xmx2G",
      scalacOptions in compile ++=
        Seq("-optimize"),
      libraryDependencies ++= Seq(
        scalatest % "test",
        spire % "test",
        sprayClient % "test",
        sprayRouting % "test"
      )
    ) ++
    defaultAssemblySettings


  // Project: testkit
  lazy val testkit: Project =
    Project("testkit", file("testkit"))
      .dependsOn(raster, engine)
      .settings(name := "geotrellis-testkit")
      .settings(libraryDependencies += scalatest)


  // Project: services
  lazy val services: Project =
    Project("services", file("services"))
      .dependsOn(raster, vector, engine)
      .settings(name := "geotrellis-services")

  // Project: jetty
  lazy val jetty: Project =
    Project("jetty", file("jetty"))
      .settings(jettySettings: _*)
      .dependsOn(services)

  lazy val jettySettings =
    Seq(
      name := "geotrellis-jetty",
      libraryDependencies ++= Seq(
        jettyWebapp,
        jerseyBundle,
        slf4jApi,
        slf4jNop,
        asm
      )
    ) ++
    defaultAssemblySettings

  // Project: slick
  lazy val geotrellis_slick: Project =
    Project("slick", file("slick"))
      .settings(slickSettings: _*)
      .dependsOn(vector)

  lazy val slickSettings =
    Seq(
      name := "geotrellis-slick",
      libraryDependencies := Seq(
        slick,
        postgresql,
        slf4jNop,
        scalatest % "test"
      )
    ) ++
    defaultAssemblySettings

  // Project: admin
  lazy val admin: Project =
    Project("admin", file("admin"))
      .settings(adminSettings: _*)
      .dependsOn(raster, services, vector)

  lazy val adminSettings =
    Seq(
      name := "geotrellis-admin",
      fork := true,
      libraryDependencies ++= Seq(
        scalatest % "test",
        sprayTestkit % "test",
        sprayRouting,
        sprayCan,
        sprayHttpx
      )
    ) ++
    spray.revolver.RevolverPlugin.Revolver.settings ++
    defaultAssemblySettings

  // Project: spark
  lazy val spark: Project =
    Project("spark", file("spark"))
      .settings(sparkSettings: _*)
      .dependsOn(raster, testkit % "test")

  // using hadoop and spark version from environment was inspired by Spark itself
  val DEFAULT_HADOOP_VERSION = "2.3.0-cdh5.1.0"
  lazy val hadoopVersion = Properties.envOrElse("SPARK_HADOOP_VERSION", DEFAULT_HADOOP_VERSION)

  val DEFAULT_SPARK_VERSION = "1.0.0"
  lazy val sparkVersion = Properties.envOrElse("SPARK_VERSION", DEFAULT_SPARK_VERSION)
  lazy val sparkSettings =
    Seq(
      name := "geotrellis-spark",
      parallelExecution in Test := false,
      javaOptions += "-Xmx8G",
      libraryDependencies ++=
        Seq(
          // first two are just to quell the UnsupportedOperationException in Hadoop's Configuration
          // http://itellity.wordpress.com/2013/05/27/xerces-parse-error-with-hadoop-or-solr-feature-httpapache-orgxmlfeaturesxinclude-is-not-recognized/
          "xerces" % "xercesImpl" % "2.9.1",
          "xalan" % "xalan" % "2.7.1",
          "org.apache.spark" %% "spark-core" % sparkVersion
            excludeAll (
              ExclusionRule(organization = "org.apache.hadoop"),
              ExclusionRule(organization = "com.google.code.findbugs")
            ),
          "org.apache.hadoop" % "hadoop-client" % hadoopVersion % "compile"
            excludeAll (ExclusionRule(organization = "hsqldb")),
          "org.apache.hadoop" % "hadoop-client" % "0.20.2-cdh3u4" % "test"
            excludeAll (ExclusionRule(organization = "hsqldb")),
          "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.3.0"
            excludeAll (ExclusionRule(organization = "com.google.code.findbugs")),
          "com.quantifind" %% "sumac" % "0.2.3",
          spire, sprayRouting, sprayCan,
          scalatest % "test",
          "org.mockito" % "mockito-core" % "1.9.5" % "test",
          "org.apache.accumulo" % "accumulo-core" % "1.5.1"
        ),
      resolvers ++= Seq(
        "Cloudera Repo" at "https://repository.cloudera.com/artifactory/cloudera-repos"
      )
    ) ++
    defaultAssemblySettings ++
    net.virtualvoid.sbt.graph.Plugin.graphSettings

  // Project: gdal

  lazy val gdal: Project =
    Project("gdal", file("gdal"))
      .settings(gdalSettings: _*)
      .dependsOn(raster, geotools % "test")

  lazy val gdalSettings =
    Seq(
      name := "geotrellis-gdal",
      javaOptions += "-Djava.library.path=/usr/local/lib",
      libraryDependencies ++=
        Seq(
          "org.gdal" % "gdal" % "1.10.1",
          "com.github.scopt" % "scopt_2.10" % "3.2.0",
          scalatest % "test"
        ),
      resolvers ++=
        Seq(
          "OpenGeo" at "http://repo.opengeo.org/"
        ),
      fork in test := true
    ) ++
    defaultAssemblySettings

  // Project: geotools

  lazy val geotools: Project =
    Project("geotools", file("geotools"))
      .settings(geotoolsSettings: _*)
      .dependsOn(raster, engine)
      .dependsOn(testkit % "test")

  lazy val geotoolsSettings =
    Seq(
      name := "geotrellis-geotools",
      libraryDependencies ++=
        Seq(
          "java3d" % "j3d-core" % "1.3.1",
          "org.geotools" % "gt-main" % Version.geotools,
          "org.geotools" % "gt-coverage" % Version.geotools,
          "org.geotools" % "gt-shapefile" % Version.geotools,
          "org.geotools" % "gt-geotiff" % Version.geotools,
          "org.geotools" % "gt-epsg-hsql" % Version.geotools,
          "javax.media" % "jai_core" % "1.1.3" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_core/1.1.3/jai_core-1.1.3.jar"
        ),
      resolvers ++=
        Seq(
          "Geotools" at "http://download.osgeo.org/webdav/geotools/"
        ),
      fork in test := false
    ) ++
    defaultAssemblySettings

  // Project: dev

  lazy val dev: Project =
    Project("dev", file("dev"))
      .settings(devSettings: _*)
      .dependsOn(raster, engine)

  lazy val devSettings =
    Seq(
      libraryDependencies ++=
        Seq(
          scalaReflect,
          sigar
        ),
      Keys.fork in run := true,
      fork := true,
      javaOptions in run ++=
        Seq(
          "-Djava.library.path=./sigar"
        )
    ) ++
    defaultAssemblySettings

  // Project: demo
  lazy val demo: Project =
    Project("demo", file("demo"))
      .dependsOn(jetty)

  // Project: vector-benchmark

  lazy val vectorBenchmark: Project =
    Project("vector-benchmark", file("vector-benchmark"))
      .settings(vectorBenchmarkSettings: _*)
      .dependsOn(vectorTest % "compile->test")

  def vectorBenchmarkSettings =
    Seq(
      name := "geotrellis-vector-benchmark",
      libraryDependencies ++= Seq(
        scalatest % "test",
        scalacheck % "test",
        "com.google.guava" % "guava" % "r09",
        "com.google.code.java-allocation-instrumenter" % "java-allocation-instrumenter" % "2.0",
        "com.google.code.caliper" % "caliper" % "1.0-SNAPSHOT"
          from "http://plastic-idolatry.com/jars/caliper-1.0-SNAPSHOT.jar",
        "com.google.code.gson" % "gson" % "1.7.1"
      ),

      // enable forking in both run and test
      fork := true,

      // custom kludge to get caliper to see the right classpath

      // we need to add the runtime classpath as a "-cp" argument to the
      // `javaOptions in run`, otherwise caliper will not see the right classpath
      // and die with a ConfigurationException unfortunately `javaOptions` is a
      // SettingsKey and `fullClasspath in Runtime` is a TaskKey, so we need to
      // jump through these hoops here in order to feed the result of the latter
      // into the former
      onLoad in Global ~= { previous => state =>
        previous {
          state.get(vectorBenchmarkKey) match {
            case None =>
              // get the runtime classpath, turn into a colon-delimited string
              Project
                .runTask(fullClasspath in Runtime in vectorBenchmark, state)
                .get
                ._2
                .toEither match {
                  case Right(x) =>
                    val classPath =
                      x.files
                       .mkString(":")
                    // return a state with javaOptionsPatched = true and javaOptions set correctly
                    Project
                      .extract(state)
                      .append(
                        Seq(javaOptions in (benchmark, run) ++= Seq("-Xmx8G", "-cp", classPath)),
                        state.put(vectorBenchmarkKey, true)
                      )
                  case _ => state
                }

            case Some(_) =>
              state // the javaOptions are already patched
          }
        }
      }

    )

  // Project: gdal-benchmark

  lazy val gdalBenchmark: Project =
    Project("gdal-benchmark", file("gdal-benchmark"))
      .settings(gdalBenchmarkSettings:_*)
      .dependsOn(gdal, geotools)

  lazy val gdalBenchmarkSettings =
    Seq(
      organization := "com.azavea.geotrellis",
      name := "gdal-benchmark",

      scalaVersion := "2.10.3",
      // raise memory limits here if necessary
      javaOptions += "-Xmx2G",
      javaOptions += "-Djava.library.path=/usr/local/lib",

      libraryDependencies ++= Seq(
        spire,
        "com.google.guava" % "guava" % "r09",
        "com.google.code.java-allocation-instrumenter" % "java-allocation-instrumenter" % "2.0",
        "com.google.code.caliper" % "caliper" % "1.0-SNAPSHOT"
          from "http://plastic-idolatry.com/jars/caliper-1.0-SNAPSHOT.jar",
        "com.google.code.gson" % "gson" % "1.7.1"
      ),


      // enable forking in both run and test
      fork := true,
      // custom kludge to get caliper to see the right classpath

      // we need to add the runtime classpath as a "-cp" argument to the
      // `javaOptions in run`, otherwise caliper will not see the right classpath
      // and die with a ConfigurationException unfortunately `javaOptions` is a
      // SettingsKey and `fullClasspath in Runtime` is a TaskKey, so we need to
      // jump through these hoops here in order to feed the result of the latter
      // into the former
      onLoad in Global ~= { previous => state =>
        previous {
          state.get(gdalBenchmarkKey) match {
            case None =>
              // get the runtime classpath, turn into a colon-delimited string
              Project
                .runTask(fullClasspath in Runtime in benchmark, state)
                .get
                ._2
                .toEither match {
                case Right(x) =>
                  val classPath =
                    x.files
                      .mkString(":")
                  // return a state with javaOptionsPatched = true and javaOptions set correctly
                  Project
                    .extract(state)
                    .append(
                    Seq(javaOptions in (benchmark, run) ++= Seq("-Xmx8G", "-cp", classPath)),
                      state.put(gdalBenchmarkKey, true)
                  )
                case _ => state
              }
            case Some(_) =>
              state // the javaOptions are already patched
          }
        }
      }
    ) ++
  defaultAssemblySettings

  // Project: benchmark

  lazy val benchmark: Project =
    Project("benchmark", file("benchmark"))
      .settings(benchmarkSettings: _*)
      .dependsOn(raster,engine,geotools)

  def benchmarkSettings =
    Seq(
      // raise memory limits here if necessary
      javaOptions += "-Xmx8G",
      libraryDependencies ++= Seq(
        spire,
        "com.google.guava" % "guava" % "r09",
        "com.google.code.java-allocation-instrumenter" % "java-allocation-instrumenter" % "2.0",
        "com.google.code.caliper" % "caliper" % "1.0-SNAPSHOT"
          from "http://plastic-idolatry.com/jars/caliper-1.0-SNAPSHOT.jar",
        "com.google.code.gson" % "gson" % "1.7.1"
      ),

      // enable forking in both run and test
      fork := true,

      // custom kludge to get caliper to see the right classpath

      // we need to add the runtime classpath as a "-cp" argument to the
      // `javaOptions in run`, otherwise caliper will not see the right classpath
      // and die with a ConfigurationException unfortunately `javaOptions` is a
      // SettingsKey and `fullClasspath in Runtime` is a TaskKey, so we need to
      // jump through these hoops here in order to feed the result of the latter
      // into the former
      onLoad in Global ~= { previous => state =>
        previous {
          state.get(benchmarkKey) match {
            case None =>
              // get the runtime classpath, turn into a colon-delimited string
              Project
                .runTask(fullClasspath in Runtime in benchmark, state)
                .get
                ._2
                .toEither match {
                case Right(x) =>
                  val classPath =
                    x.files
                      .mkString(":")
                  // return a state with javaOptionsPatched = true and javaOptions set correctly
                  Project
                    .extract(state)
                    .append(
                    Seq(javaOptions in (benchmark, run) ++= Seq("-Xmx8G", "-cp", classPath)),
                      state.put(benchmarkKey, true)
                  )
                case _ => state
              }
            case Some(_) =>
              state // the javaOptions are already patched
          }
        }
      }
    ) ++
  defaultAssemblySettings
}

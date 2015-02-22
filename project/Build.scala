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
  val benchmarkKey = AttributeKey[Boolean]("javaOptionsPatched")

  // Default settings
  override lazy val settings =
    super.settings ++
  Seq(
    shellPrompt := { s => Project.extract(s).currentProject.id + " > " },
    version := Version.geotrellis,
    scalaVersion := Version.scala,
    crossScalaVersions := Seq("2.11.5", "2.10.4"),
    organization := "com.azavea.geotrellis",

    // disable annoying warnings about 2.10.x
    conflictWarning in ThisBuild := ConflictWarning.disable,
    scalacOptions ++=
      Seq("-deprecation",
        "-unchecked",
        "-Yinline-warnings",
        "-language:implicitConversions",
        "-language:reflectiveCalls",
        "-language:higherKinds",
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
        <id>echeipesh</id>
        <name>Eugene Cheipesh</name>
        <url>http://github.com/echeipesh/</url>
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
    Project("geotrellis", file("."))
      .dependsOn(raster, vector, proj4)
      .aggregate(raster, vector, proj4, spark, rasterTest, vectorTest)
      .settings(
      initialCommands in console:=
        """
          import geotrellis.raster._
          import geotrellis.vector._
          import geotrellis.proj4._
          """
    )

  // Project: macros
  lazy val macros =
    Project("macros", file("macros"))
      .settings(macrosSettings: _*)

  lazy val macrosSettings = Seq(
    name := "geotrellis-macros",
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full),
    libraryDependencies <++= scalaVersion { 
      case "2.10.4" => Seq(
        "org.scala-lang" %  "scala-reflect" % "2.10.4",
        "org.scalamacros" %% "quasiquotes" % "2.0.1")
      case "2.11.5" => Seq(
        "org.scala-lang" %  "scala-reflect" % "2.11.5")
    },
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
          apacheMath,
          spire
        )
    )
      .settings(defaultAssemblySettings: _*)
      .dependsOn(proj4)

  // Project: vector-test
  lazy val vectorTest =
    Project("vector-test", file("vector-test"))
      .dependsOn(vector, testkit)
      .settings(name := "geotrellis-vector-test")
      .settings(libraryDependencies ++= Seq(
        scalatest   % "test",
        scalacheck  % "test"
      )
    )


  // Project: proj4
  lazy val proj4 =
    Project("proj4", file("proj4"))
      .settings(proj4Settings: _*)

  lazy val proj4Settings =
    Seq(
      name := "geotrellis-proj4",
      libraryDependencies ++= Seq(
        "org.parboiled" %% "parboiled" % "2.0.0" % "test",
        scalatest   % "test",
        scalacheck  % "test",
        openCSV
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
      scalacOptions in compile ++= Seq("-optimize"),
      addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full),
      libraryDependencies ++= Seq(
        typesafeConfig,
        jts,
        spire,
        monocleCore, monocleMacro,
        sprayClient, // for reading args from URLs,
        openCSV
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
      addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full),
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
        akkaKernel, akkaRemote, akkaActor, akkaCluster,
        spire,
        monocleCore, monocleMacro,
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
      parallelExecution := true,
      fork in test := true,
      javaOptions in run += "-Xmx4G",
      scalacOptions in compile ++=
        Seq("-optimize"),
      libraryDependencies ++= Seq(        
        sprayClient % "test",
        sprayRouting % "test",
        scalatest % "test"
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
        slf4jApi,
        scalatest % "test"
      )
    ) ++
  defaultAssemblySettings ++
  net.virtualvoid.sbt.graph.Plugin.graphSettings

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


  val Unprovided = config("unprovided") extend Runtime

  lazy val spark: Project =
    Project("spark", file("spark"))
      .settings(sparkSettings: _*)
      .dependsOn(raster, gdal)

  lazy val sparkSettings =
    Seq(
      name := "geotrellis-spark",
      fork := true,
      parallelExecution in Test := false,
      javaOptions ++= List(
        "-Xmx8G",
        "-Djava.library.path=/usr/local/lib",
        "-Dsun.io.serialization.extendedDebugInfo=true"
      ),
      libraryDependencies ++=
        Seq(
          "org.apache.spark" %% "spark-core" % Version.spark % "provided",
          "org.apache.hadoop" % "hadoop-client" % Version.hadoop % "provided",
          "com.quantifind" %% "sumac" % "0.3.0",
          "org.apache.accumulo" % "accumulo-core" % "1.5.2",
          "de.javakaffee" % "kryo-serializers" % "0.27",
          logging, awsSdkS3,
          spire,
          monocleCore, monocleMacro,
          nscalaTime,
          sprayRouting, sprayCan,
          scalatest % "test"
        ),
      resolvers ++= Seq(
        "Cloudera Repo" at "https://repository.cloudera.com/artifactory/cloudera-repos"
      ),
      initialCommands in console :=
        """
        import geotrellis.raster._
        import geotrellis.vector._
        import geotrellis.proj4._
        import geotrellis.spark._
        import geotrellis.spark.utils._
        import geotrellis.spark.tiling._
        """
    ) ++
  defaultAssemblySettings ++
  net.virtualvoid.sbt.graph.Plugin.graphSettings

  lazy val graph: Project =
    Project("graph", file("graph"))
      .settings(graphSettings: _*)
      .dependsOn(spark % "test->test;compile->compile")

  lazy val graphSettings =
    Seq(
      name := "geotrellis-graph",
      fork := true,
      parallelExecution in Test := false,
      javaOptions ++= List(
        "-Xmx8G",
        "-Djava.library.path=/usr/local/lib",
        "-Dsun.io.serialization.extendedDebugInfo=true"
      ),
      libraryDependencies ++=
        Seq(
          "org.apache.spark" %% "spark-graphx" % Version.spark % "provided",
          scalatest % "test"
        ),
      resolvers ++= Seq(
        "Cloudera Repo" at "https://repository.cloudera.com/artifactory/cloudera-repos"
      )
    ) ++
  defaultAssemblySettings ++
  net.virtualvoid.sbt.graph.Plugin.graphSettings


  // Project: index
  lazy val index: Project =
    Project("index", file("index"))
      .settings(indexSettings: _*)

  lazy val indexSettings =
    defaultAssemblySettings ++
    Seq(
      name := "geotrellis-index",
      libraryDependencies ++=
        Seq(
          scalatest % "test"
        )
    )

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
          "org.gdal"         % "gdal"       % "1.10.1",
          "com.github.scopt" % "scopt_2.10" % "3.2.0",
          scalatest % "test"
        ),
      resolvers ++=
        Seq(
          "OpenGeo" at "http://repo.boundlessgeo.com/main"
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
      libraryDependencies += sigar,
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
        caliper,
        "com.google.guava" % "guava" % "r09",
        "com.google.code.java-allocation-instrumenter" % "java-allocation-instrumenter" % "2.0",
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

  // Project: benchmark

  lazy val benchmark: Project =
    Project("benchmark", file("benchmark"))
      .settings(benchmarkSettings: _*)
      .dependsOn(raster, engine, geotools, jetty)

  def benchmarkSettings =
    Seq(
      // raise memory limits here if necessary
      javaOptions += "-Xmx8G",
      libraryDependencies ++= Seq(
        spire,
        caliper,
        "com.google.guava" % "guava" % "r09",
        "com.google.code.java-allocation-instrumenter" % "java-allocation-instrumenter" % "2.0",
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

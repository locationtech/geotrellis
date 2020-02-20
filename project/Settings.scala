/*
 * Copyright (c) 2018 Azavea.
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

import Dependencies._
import GTBenchmarkPlugin.Keys._
import sbt._
import sbt.Keys._
import sbtassembly.AssemblyPlugin.autoImport.{MergeStrategy, assemblyMergeStrategy}
import sbtassembly.AssemblyKeys._
import sbtassembly.{PathList, ShadeRule}
import com.typesafe.tools.mima.plugin.MimaKeys._
import sbtprotoc.ProtocPlugin.autoImport.PB

object Settings {
  object Repositories {
    val locationtechReleases  = "locationtech-releases" at "https://repo.locationtech.org/content/repositories/releases/"
    val locationtechSnapshots = "locationtech-snapshots" at "https://repo.locationtech.org/content/repositories/snapshots/"
    val boundlessgeo          = "boundlessgeo" at "https://repo.boundlessgeo.com/main/"
    val boundlessgeoRelease   = "boundless" at "https://repo.boundlessgeo.com/release"
    val geosolutions          = "geosolutions" at "https://maven.geo-solutions.it/"
    val osgeo                 = "osgeo" at "https://download.osgeo.org/webdav/geotools/"
    val ivy2Local             = Resolver.file("local", file(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns)
    val mavenLocal            = Resolver.mavenLocal
    val local                 = Seq(ivy2Local, mavenLocal)
    val azaveaBintray         = Resolver.bintrayRepo("azavea", "geotrellis")
  }

  lazy val noForkInTests = Seq(
    Test / fork := false,
    Test / parallelExecution := false
  )

  val commonScalacOptions = Seq(
    "-deprecation",
    "-unchecked",
    "-feature",
    "-language:implicitConversions",
    "-language:reflectiveCalls",
    "-language:higherKinds",
    "-language:postfixOps",
    "-language:existentials",
    "-language:experimental.macros",
    "-feature",
    "-Ypartial-unification", // Required by Cats
    "-target:jvm-1.8")

  lazy val commonSettings = Seq(
    description := "geographic data processing library for high performance applications",
    licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
    homepage := Some(url("https://geotrellis.io")),
    scmInfo := Some(ScmInfo(url("https://github.com/locationtech/geotrellis"), "scm:git:git@github.com:locationtech/geotrellis.git")),
    scalacOptions ++= commonScalacOptions,
    publishMavenStyle := true,
    Test / publishArtifact := false,
    pomIncludeRepository := { _ => false },
    autoAPIMappings := true,
    useCoursier := false,
    Global / cancelable := true,

    publishTo := {
      val sonatype = "https://oss.sonatype.org/"
      val locationtech = "https://repo.locationtech.org/content/repositories"

      System.getProperty("release") match {
        case "locationtech" if isSnapshot.value =>
          Some("LocationTech Snapshot Repository" at s"${locationtech}/geotrellis-snapshots")
        case "locationtech" =>
          Some("LocationTech Release Repository" at s"${locationtech}/geotrellis-releases")
        case _ =>
          Some("Sonatype Release Repository" at s"${sonatype}service/local/staging/deploy/maven2")
      }
    },

    credentials ++= List(Path.userHome / ".ivy2" / ".credentials").filter(_.asFile.canRead).map(Credentials(_)),

    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full),
    addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.1" cross CrossVersion.full),

    pomExtra := (
      <developers>
        <developer>
          <id>echeipesh</id>
          <name>Eugene Cheipesh</name>
          <url>https://github.com/echeipesh/</url>
        </developer>
        <developer>
          <id>lossyrob</id>
          <name>Rob Emanuele</name>
          <url>https://github.com/lossyrob/</url>
        </developer>
      </developers>
    ),

    resolvers ++= Seq(
      Resolver.mavenLocal,
      Settings.Repositories.geosolutions,
      Settings.Repositories.osgeo,
      Settings.Repositories.locationtechReleases
    )
  )

  lazy val accumulo = Seq(
    name := "geotrellis-accumulo",
    libraryDependencies ++= Seq(
      accumuloCore
        exclude("org.jboss.netty", "netty")
        exclude("org.apache.hadoop", "hadoop-client"),
      spire,
      scalatest % Test,
      hadoopClient % Provided
    ),
    console / initialCommands :=
      """
      import geotrellis.proj4._
      import geotrellis.vector._
      import geotrellis.raster._
      import geotrellis.layer._
      import geotrellis.store.accumulo._
      """
  ) ++ commonSettings ++ noForkInTests

  lazy val `accumulo-spark` = Seq(
    name := "geotrellis-accumulo-spark",
    libraryDependencies ++= Seq(
      accumuloCore
        exclude("org.jboss.netty", "netty")
        exclude("org.apache.hadoop", "hadoop-client"),
      sparkCore % Provided, sparkSql % Test,
      spire,
      scalatest % Test
    ),
    console / initialCommands :=
      """
      import geotrellis.raster._
      import geotrellis.vector._
      import geotrellis.proj4._
      import geotrellis.layer._
      import geotrellis.store.accumulo._
      import geotrellis.spark._
      import geotrellis.spark.store.accumulo._
      """
  ) ++ commonSettings ++ noForkInTests

  lazy val bench = Seq(
    libraryDependencies += sl4jnop,
    jmhIterations := Some(5),
    jmhTimeUnit := None, // Each benchmark should determing the appropriate time unit.
    jmhExtraOptions := Some("-jvmArgsAppend -Xmx8G")
    //jmhExtraOptions := Some("-jvmArgsAppend -Xmx8G -prof jmh.extras.JFR")
    //jmhExtraOptions := Some("-jvmArgsAppend -prof geotrellis.bench.GeotrellisFlightRecordingProfiler")
  )

  lazy val cassandra = Seq(
    name := "geotrellis-cassandra",
    libraryDependencies ++= Seq(
      cassandraDriverCore
        excludeAll(
        ExclusionRule("org.jboss.netty"), ExclusionRule("io.netty"),
        ExclusionRule("org.slf4j"), ExclusionRule("com.typesafe.akka")
      ) exclude("org.apache.hadoop", "hadoop-client"),
      spire,
      scalatest % Test
    ),
    console / initialCommands :=
      """
      import geotrellis.proj4._
      import geotrellis.vector._
      import geotrellis.layer._
      import geotrellis.raster._
      import geotrellis.store.util._
      import geotrellis.store.cassandra._
      """
  ) ++ commonSettings ++ noForkInTests

  lazy val `cassandra-spark` = Seq(
    name := "geotrellis-cassandra-spark",
    libraryDependencies ++= Seq(
      cassandraDriverCore
        excludeAll(
        ExclusionRule("org.jboss.netty"), ExclusionRule("io.netty"),
        ExclusionRule("org.slf4j"), ExclusionRule("com.typesafe.akka")
      ) exclude("org.apache.hadoop", "hadoop-client"),
      sparkCore % Provided, sparkSql % Test,
      spire,
      scalatest % Test
    ),
    console / initialCommands :=
      """
      import geotrellis.raster._
      import geotrellis.vector._
      import geotrellis.proj4._
      import geotrellis.layer._
      import geotrellis.store.cassandra._
      import geotrellis.spark._
      import geotrellis.spark.util._
      import geotrellis.spark.store.cassandra._
      """
  ) ++ noForkInTests ++ commonSettings


  lazy val `doc-examples` = Seq(
    name := "geotrellis-doc-examples",
    publish / skip := true,
    scalacOptions ++= commonScalacOptions,
    libraryDependencies ++= Seq(
      sparkCore,
      logging,
      scalatest % Test,
      sparkSql % Test
    )
  )

  lazy val geomesa = Seq(
    name := "geotrellis-geomesa",
    libraryDependencies ++= Seq(
      geomesaAccumuloJobs,
      geomesaAccumuloDatastore,
      geomesaUtils,
      spire,
      scalatest % Test
    ),
    resolvers ++= Seq(
      Repositories.boundlessgeo,
      Repositories.locationtechReleases
    ),
    console / initialCommands :=
      """
      import geotrellis.raster._
      import geotrellis.vector._
      import geotrellis.proj4._
      import geotrellis.layer._
      import geotrellis.spark._
      import geotrellis.spark.util._
      import geotrellis.spark.io.geomesa._
      """
  ) ++ commonSettings ++ noForkInTests

  lazy val geotools = Seq(
    name := "geotrellis-geotools",
    libraryDependencies ++= Seq(
      geotoolsCoverage,
      geotoolsHsql,
      geotoolsMain,
      geotoolsReferencing,
      jts,
      spire,
      geotoolsGeoTiff % Test,
      geotoolsShapefile % Test,
      scalatest % Test,
      // This is one finicky dependency. Being explicit in hopes it will stop hurting Travis.
      jaiCore % Test from "https://download.osgeo.org/webdav/geotools/javax/media/jai_core/1.1.3/jai_core-1.1.3.jar"
    ),
    externalResolvers ++= Seq(
      Repositories.boundlessgeo
    ),
    console / initialCommands :=
      """
      import geotrellis.geotools._
      import geotrellis.raster._
      import geotrellis.vector._
      import org.locationtech.jts.{geom => jts}
      import org.geotools.coverage.grid._
      import org.geotools.coverage.grid.io._
      import org.geotools.gce.geotiff._
      """,
    Test / testOptions += Tests.Setup { () => Unzip.geoTiffTestFiles() }
  ) ++ commonSettings ++ noForkInTests

  lazy val geowave = Seq(
    name := "geotrellis-geowave",
    libraryDependencies ++= Seq(
      accumuloCore
        exclude("org.jboss.netty", "netty")
        exclude("org.apache.hadoop", "hadoop-client"),
      geowaveRaster
        excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet")),
      geowaveVector
        excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet")),
      geowaveStore
        excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet")),
      geowaveGeotime
        excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet")),
      geowaveAccumulo
        excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet")),
      hadoopClient % Provided
        excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet")),
      geotoolsCoverage % Provided
        excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet")),
      geotoolsHsql % Provided
        excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet")),
      geotoolsMain % Provided
        excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet")),
      geotoolsReferencing % Provided
        excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet")),
      scalaArm,
      kryoSerializers exclude("com.esotericsoftware", "kryo"),
      kryoShaded,
      sparkCore % Provided,
      spire,
      sparkSql % Test,
      scalatest % Test
    ),
    resolvers ++= Seq(
      Repositories.boundlessgeoRelease,
      Repositories.geosolutions,
      Repositories.osgeo
    ),
    assembly / assemblyMergeStrategy := {
      case "reference.conf" => MergeStrategy.concat
      case "application.conf" => MergeStrategy.concat
      case PathList("META-INF", xs@_*) =>
        xs match {
          case ("MANIFEST.MF" :: Nil) => MergeStrategy.discard
          // Concatenate everything in the services directory to keep GeoTools happy.
          case ("services" :: _ :: Nil) =>
            MergeStrategy.concat
          // Concatenate these to keep JAI happy.
          case ("javax.media.jai.registryFile.jai" :: Nil) | ("registryFile.jai" :: Nil) | ("registryFile.jaiext" :: Nil) =>
            MergeStrategy.concat
          case (name :: Nil) => {
            // Must exclude META-INF/*.([RD]SA|SF) to avoid "Invalid signature file digest for Manifest main attributes" exception.
            if (name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".SF"))
              MergeStrategy.discard
            else
              MergeStrategy.first
          }
          case _ => MergeStrategy.first
        }
      case _ => MergeStrategy.first
    }
  ) ++ commonSettings ++ noForkInTests

  lazy val hbase = Seq(
    name := "geotrellis-hbase",
    libraryDependencies ++= Seq(
      hbaseCommon exclude("javax.servlet", "servlet-api"),
      hbaseClient exclude("javax.servlet", "servlet-api"),
      hbaseMapReduce exclude("javax.servlet", "servlet-api"),
      hbaseServer exclude("org.mortbay.jetty", "servlet-api-2.5"),
      hbaseHadoopCompact exclude("javax.servlet", "servlet-api"),
      hbaseHadoop2Compact exclude("javax.servlet", "servlet-api"),
      hbaseMetrics exclude("javax.servlet", "servlet-api"),
      hbaseMetricsApi exclude("javax.servlet", "servlet-api"),
      hbaseZooKeeper exclude("javax.servlet", "servlet-api"),
      jacksonCoreAsl,
      spire,
      sparkSql % Test,
      scalatest % Test
    ),
    /** https://github.com/lucidworks/spark-solr/issues/179 */
    dependencyOverrides ++= {
      val deps = Seq(
        "com.fasterxml.jackson.core" % "jackson-core" % "2.6.7",
        "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.7",
        "com.fasterxml.jackson.core" % "jackson-annotations" % "2.6.7"
      )
      CrossVersion.partialVersion(scalaVersion.value) match {
        // if Scala 2.12+ is used
        case Some((2, scalaMajor)) if scalaMajor >= 12 => deps
        case _ => deps :+ "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.6.7"
      }
    },
    console / initialCommands :=
      """
      import geotrellis.raster._
      import geotrellis.vector._
      import geotrellis.proj4._
      import geotrellis.layer._
      import geotrellis.store.util._
      import geotrellis.store.hbase._
      """
  ) ++ commonSettings ++ noForkInTests

  lazy val `hbase-spark` = Seq(
    name := "geotrellis-hbase-spark",
    libraryDependencies ++= Seq(
      hbaseCommon exclude("javax.servlet", "servlet-api"),
      hbaseClient exclude("javax.servlet", "servlet-api"),
      hbaseMapReduce exclude("javax.servlet", "servlet-api"),
      hbaseServer exclude("org.mortbay.jetty", "servlet-api-2.5"),
      hbaseHadoopCompact exclude("javax.servlet", "servlet-api"),
      hbaseHadoop2Compact exclude("javax.servlet", "servlet-api"),
      hbaseMetrics exclude("javax.servlet", "servlet-api"),
      hbaseMetricsApi exclude("javax.servlet", "servlet-api"),
      hbaseZooKeeper exclude("javax.servlet", "servlet-api"),
      jacksonCoreAsl,
      sparkCore % Provided,
      spire,
      sparkSql % Test,
      scalatest % Test
    ),
    /** https://github.com/lucidworks/spark-solr/issues/179 */
    dependencyOverrides ++= {
      val deps = Seq(
        "com.fasterxml.jackson.core" % "jackson-core" % "2.6.7",
        "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.7",
        "com.fasterxml.jackson.core" % "jackson-annotations" % "2.6.7"
      )
      CrossVersion.partialVersion(scalaVersion.value) match {
        // if Scala 2.12+ is used
        case Some((2, scalaMajor)) if scalaMajor >= 12 => deps
        case _ => deps :+ "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.6.7"
      }
    },
    console / initialCommands :=
      """
      import geotrellis.raster._
      import geotrellis.vector._
      import geotrellis.proj4._
      import geotrellis.layer._
      import geotrellis.store.util._
      import geotrellis.spark._
      import geotrellis.spark.store.hbase._
      import geotrellis.store.hbase._
      """
  ) ++ commonSettings ++ noForkInTests

  lazy val macros = Seq(
    name := "geotrellis-macros",
    Compile / sourceGenerators += (Compile / sourceManaged).map(Boilerplate.genMacro).taskValue,
    libraryDependencies ++= Seq(
      spireMacro,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
    ),
    resolvers += Resolver.sonatypeRepo("snapshots")
  ) ++ commonSettings

  lazy val proj4 = Seq(
    name := "geotrellis-proj4",
    libraryDependencies ++= Seq(
      proj4j,
      openCSV,
      parserCombinators,
      scalatest % Test,
      scalacheck % Test,
      scaffeine
    ),
    // https://github.com/sbt/sbt/issues/4609
    Test / fork := true
  ) ++ commonSettings

  lazy val raster = Seq(
    name := "geotrellis-raster",
    libraryDependencies ++= Seq(
      pureconfig,
      jts,
      cats("core").value,
      spire,
      squants,
      monocle("core").value, monocle("macro").value,
      scalaXml,
      scalaURI.value,
      scalatest % Test,
      scalacheck % Test
    ),
    mimaPreviousArtifacts := Set(
      "org.locationtech.geotrellis" %% "geotrellis-raster" % Version.previousVersion
    ),
    Compile / sourceGenerators += (Compile / sourceManaged).map(Boilerplate.genRaster).taskValue,
    console / initialCommands :=
      """
      import geotrellis.raster._
      import geotrellis.raster.resample._
      import geotrellis.vector._
      import geotrellis.raster.io.geotiff._
      import geotrellis.raster.render._
      """,
    Test / testOptions += Tests.Setup { () =>
      val testArchive = "raster/data/geotiff-test-files.zip"
      val testDirPath = "raster/data/geotiff-test-files"
      if (!(new File(testDirPath)).exists) {
        Unzip(testArchive, "raster/data")
      }
    }
  ) ++ commonSettings

  lazy val `raster-testkit` = Seq(
    name := "geotrellis-raster-testkit",
    libraryDependencies += scalatest
  ) ++ commonSettings

  lazy val s3 = Seq(
    name := "geotrellis-s3",
    libraryDependencies ++= Seq(
      awsSdkS3,
      spire,
      scaffeine,
      scalatest % Test
    ),
    dependencyOverrides ++= {
      val deps = Seq(
        "com.fasterxml.jackson.core" % "jackson-core" % "2.6.7",
        "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.7",
        "com.fasterxml.jackson.core" % "jackson-annotations" % "2.6.7"
      )
      CrossVersion.partialVersion(scalaVersion.value) match {
        // if Scala 2.12+ is used
        case Some((2, scalaMajor)) if scalaMajor >= 12 => deps
        case _ => deps :+ "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.6.7"
      }
    },
    mimaPreviousArtifacts := Set(
      "org.locationtech.geotrellis" %% "geotrellis-s3" % Version.previousVersion
    ),
    console / initialCommands :=
      """
      import geotrellis.raster._
      import geotrellis.vector._
      import geotrellis.proj4._
      import geotrellis.layer._
      import geotrellis.store.s3._
      """
  ) ++ noForkInTests ++ commonSettings

  lazy val `s3-spark` = Seq(
    name := "geotrellis-s3-spark",
    libraryDependencies ++= Seq(
      sparkCore % Provided,
      awsSdkS3,
      spire,
      scaffeine,
      sparkSql % Test,
      scalatest % Test
    ),
    dependencyOverrides ++= {
      val deps = Seq(
        "com.fasterxml.jackson.core" % "jackson-core" % "2.6.7",
        "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.7",
        "com.fasterxml.jackson.core" % "jackson-annotations" % "2.6.7"
      )
      CrossVersion.partialVersion(scalaVersion.value) match {
        // if Scala 2.12+ is used
        case Some((2, scalaMajor)) if scalaMajor >= 12 => deps
        case _ => deps :+ "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.6.7"
      }
    },
    mimaPreviousArtifacts := Set(
      "org.locationtech.geotrellis" %% "geotrellis-s3" % Version.previousVersion
    ),
    console / initialCommands :=
      """
      import geotrellis.raster._
      import geotrellis.vector._
      import geotrellis.proj4._
      import geotrellis.layer._
      import geotrellis.store.s3._
      import geotrellis.store.util._
      import geotrellis.spark._
      import geotrellis.spark.store.s3._
      """
  ) ++ noForkInTests ++ commonSettings

  lazy val shapefile = Seq(
    name := "geotrellis-shapefile",
    libraryDependencies ++= Seq(
      geotoolsShapefile,
      // This is one finicky dependency. Being explicit in hopes it will stop hurting Travis.
      jaiCore from "https://download.osgeo.org/webdav/geotools/javax/media/jai_core/1.1.3/jai_core-1.1.3.jar"
    ),
    resolvers += Repositories.osgeo,
    Test / fork := false
  ) ++ commonSettings

  lazy val spark = Seq(
    name := "geotrellis-spark",
    libraryDependencies ++= Seq(
      sparkCore % Provided,
      hadoopClient % Provided,
      uzaygezenCore,
      scalaj,
      avro,
      spire,
      chronoscala,
      sparkSql % Test,
      scalatest % Test,
      logging,
      scaffeine
    ),
    mimaPreviousArtifacts := Set(
      "org.locationtech.geotrellis" %% "geotrellis-spark" % Version.previousVersion
    ),
    Test / testOptions += Tests.Argument("-oD"),
    console / initialCommands :=
      """
      import geotrellis.raster._
      import geotrellis.vector._
      import geotrellis.proj4._
      import geotrellis.layer._
      import geotrellis.spark._
      import geotrellis.spark.util._
      """
  ) ++ noForkInTests ++ commonSettings

  lazy val `spark-pipeline` = Seq(
    name := "geotrellis-spark-pipeline",
    libraryDependencies ++= Seq(
      circe("core").value, circe("generic").value, circe("generic-extras").value, circe("parser").value,
      sparkCore % Provided, sparkSql % Test,
      scalatest % Test
    ),
    assembly / test := {},
    assembly / assemblyShadeRules := {
      val shadePackage = "com.azavea.shaded.demo"
      Seq(
        ShadeRule.rename("com.google.common.**" -> s"$shadePackage.google.common.@1")
          .inLibrary("com.azavea.geotrellis" %% "geotrellis-cassandra" % version.value).inAll,
        ShadeRule.rename("io.netty.**" -> s"$shadePackage.io.netty.@1")
          .inLibrary("com.azavea.geotrellis" %% "geotrellis-hbase" % version.value).inAll,
        ShadeRule.rename("com.fasterxml.jackson.**" -> s"$shadePackage.com.fasterxml.jackson.@1")
          .inLibrary(jsonSchemaValidator).inAll,
        ShadeRule.rename("shapeless.**" -> s"$shadePackage.shapeless.@1").inAll
      )
    },
    assembly / assemblyMergeStrategy := {
      case s if s.startsWith("META-INF/services") => MergeStrategy.concat
      case "reference.conf" | "application.conf"  => MergeStrategy.concat
      case "META-INF/MANIFEST.MF" | "META-INF\\MANIFEST.MF" => MergeStrategy.discard
      case "META-INF/ECLIPSEF.RSA" | "META-INF/ECLIPSEF.SF" => MergeStrategy.discard
      case _ => MergeStrategy.first
    }
  ) ++ commonSettings

  lazy val `spark-testkit` = Seq(
    name := "geotrellis-spark-testkit",
    libraryDependencies ++= Seq(
      sparkCore % Provided, sparkSql % Provided,
      hadoopClient % Provided,
      scalatest,
      chronoscala
    )
  ) ++ commonSettings

  lazy val util = Seq(
    name := "geotrellis-util",
    libraryDependencies ++= Seq(
      logging,
      pureconfig,
      spire,
      scalatest % Test
    )
  ) ++ commonSettings

  lazy val vector = Seq(
    name := "geotrellis-vector",
    libraryDependencies ++= Seq(
      jts,
      pureconfig,
      circe("core").value, circe("generic").value, circe("parser").value,
      apacheMath,
      spire,
      scalatest % Test,
      scalacheck % Test
    )
  ) ++ commonSettings

  lazy val `vector-testkit` = Seq(
    name := "geotrellis-vector-testkit",
    libraryDependencies += scalatest
  ) ++ commonSettings

  lazy val vectortile = Seq(
    name := "geotrellis-vectortile",
    libraryDependencies ++= Seq(
      scalatest % Test,
      scalapbRuntime % "protobuf"
    ),
    Compile / PB.protoSources:= Seq(file("vectortile/data")),
    Compile / PB.targets := Seq(
      scalapb.gen(flatPackage = true, grpc = false) -> (Compile / sourceManaged).value
    )
  ) ++ commonSettings

  lazy val layer = Seq(
    name := "geotrellis-layer",
    libraryDependencies ++= Seq(
      hadoopClient % Provided,
      apacheIO,
      avro,
      spire,
      chronoscala,
      logging,
      scaffeine,
      uzaygezenCore,
      pureconfig,
      scalactic,
      scalatest % Test
    ),
    console / initialCommands :=
      """
      import geotrellis.raster._
      import geotrellis.vector._
      import geotrellis.proj4._
      import geotrellis.layer._
      """
  ) ++ commonSettings

  lazy val store = Seq(
    name := "geotrellis-store",
    libraryDependencies ++= Seq(
      hadoopClient % Provided,
      apacheIO,
      avro,
      spire,
      chronoscala,
      spire,
      fs2("core").value, fs2("io").value,
      logging,
      scaffeine,
      uzaygezenCore,
      pureconfig,
      scalaXml,
      scalatest % Test
    )
  ) ++ commonSettings

  lazy val gdal = Seq(
    name := "geotrellis-gdal",
    libraryDependencies ++= Seq(
      gdalWarp,
      scalatest % Test,
      gdalBindings % Test
    ),
    resolvers += Repositories.azaveaBintray,
    Test / fork := true,
    Test / parallelExecution := false,
    Test / testOptions += Tests.Argument("-oDF"),
    javaOptions ++= Seq("-Djava.library.path=/usr/local/lib")
  ) ++ commonSettings

  lazy val `gdal-spark` = Seq(
    name := "geotrellis-gdal-spark",
    libraryDependencies ++= Seq(
      gdalWarp,
      sparkCore % Provided, sparkSql % Test,
      scalatest % Test
    ),
    // caused by the AWS SDK v2
    dependencyOverrides ++= {
      val deps = Seq(
        jacksonCore,
        jacksonDatabind,
        jacksonAnnotations
      )
      CrossVersion.partialVersion(scalaVersion.value) match {
        // if Scala 2.12+ is used
        case Some((2, scalaMajor)) if scalaMajor >= 12 => deps
        case _ => deps :+ jacksonModuleScala
      }
    },
    resolvers += Repositories.azaveaBintray,
    Test / fork := true,
    Test / parallelExecution := false,
    Test / testOptions += Tests.Argument("-oDF"),
    javaOptions ++= Seq("-Djava.library.path=/usr/local/lib")
  ) ++ commonSettings
}

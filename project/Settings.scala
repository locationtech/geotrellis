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
import sbtassembly.AssemblyPlugin.autoImport._
import com.typesafe.tools.mima.plugin.MimaKeys._
import de.heikoseeberger.sbtheader.{CommentStyle, FileType}
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.{HeaderLicense, headerLicense, headerMappings}
import sbtprotoc.ProtocPlugin.autoImport.PB
import mdoc.MdocPlugin.autoImport._

import java.io.File

object Settings {
  object Repositories {
    val apacheSnapshots = "apache-snapshots" at "https://repository.apache.org/content/repositories/snapshots/"
    val eclipseReleases = "eclipse-releases" at "https://repo.eclipse.org/content/groups/releases"
    val osgeoReleases   = "osgeo-releases" at "https://repo.osgeo.org/repository/release/"
    val geosolutions    = "geosolutions" at "https://maven.geo-solutions.it/"
    val jitpack         = "jitpack" at "https://jitpack.io" // for https://github.com/everit-org/json-schema
    val ivy2Local       = Resolver.file("local", file(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns)
    val mavenLocal      = Resolver.mavenLocal
    val maven           = DefaultMavenRepository
    val local           = Seq(ivy2Local, mavenLocal)
    val external        = Seq(osgeoReleases, maven, eclipseReleases, geosolutions, jitpack, apacheSnapshots)
    val all             = external ++ local
  }

  lazy val noForkInTests = Seq(
    Test / fork := false
  )

  lazy val forkInTests = Seq(
    Test / fork := true
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
    // "-Yrangepos",            // required by SemanticDB compiler plugin
    // "-Ywarn-unused-import",  // required by `RemoveUnused` rule
    "-target:jvm-1.8"
  )

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
    Global / cancelable := true,
    Test / parallelExecution := false,

    publishTo := {
      val sonatype = "https://oss.sonatype.org/"
      val locationtech = "https://repo.eclipse.org/content/repositories"

      System.getProperty("release") match {
        case "locationtech" if isSnapshot.value =>
          Some("LocationTech Snapshot Repository" at s"${locationtech}/geotrellis-snapshots")
        case "locationtech" =>
          Some("LocationTech Release Repository" at s"${locationtech}/geotrellis-releases")
        case "sonatype" =>
          Some("Sonatype Release Repository" at s"${sonatype}service/local/staging/deploy/maven2")
        case _ => publishTo.value
      }
    },

    credentials ++= List(
      Path.userHome / ".ivy2" / ".credentials",
      Path.userHome / ".sbt" / ".credentials"
    ).filter(_.asFile.canRead).map(Credentials(_)),

    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full),
    addCompilerPlugin("org.scalameta" % "semanticdb-scalac" % "4.5.9" cross CrossVersion.full),

    libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 13)) => Nil
      case Some((2, 12)) => Seq(
        compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
        "org.scala-lang.modules" %% "scala-collection-compat" % "2.8.0"
      )
        case x => sys.error(s"Encountered unsupported Scala version ${x.getOrElse("undefined")}")
    }),
    Compile / scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 13)) => Seq("-Ymacro-annotations") // replaces paradise in 2.13
        case Some((2, 12)) => Seq("-Ypartial-unification") // required by Cats
        case x => sys.error(s"Encountered unsupported Scala version ${x.getOrElse("undefined")}")
    }),

    libraryDependencies ++= Seq(
      scalaReflect(scalaVersion.value),
      log4jbridge % Test // CVE-2021-4104, CVE-2020-8908
    ),

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
        <developer>
          <id>pomadchin</id>
          <name>Grigory Pomadchin</name>
          <url>https://github.com/pomadchin/</url>
        </developer>
      </developers>
    ),

    externalResolvers := Settings.Repositories.all,
    headerLicense := Some(HeaderLicense.ALv2(java.time.Year.now.getValue.toString, "Azavea")),
    headerMappings := Map(
      FileType.scala -> CommentStyle.cStyleBlockComment.copy(
        commentCreator = { (text, existingText) => {
          // preserve year of old headers
          val newText = CommentStyle.cStyleBlockComment.commentCreator.apply(text, existingText)
          existingText.flatMap(_ => existingText.map(_.trim)).getOrElse(newText)
        } }
      )
    )
  )

  lazy val sparkCompatDependencies = Def.setting { CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 13)) => Seq("org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.3") // spark uses it as a par collections compat
    case Some((2, 12)) => Nil
    case x => sys.error(s"Encountered unsupported Scala version ${x.getOrElse("undefined")}")
  } }

  // excluded dependencies due to license issue
  // exclusion list is inspired by https://github.com/locationtech/geomesa/blob/geomesa_2.11-3.1.2/pom.xml#L1031
  lazy val excludedDependencies = List(
    ExclusionRule("javax.media", "jai_core"),
    ExclusionRule("javax.media", "jai_codec"),
    ExclusionRule("javax.media", "jai_imageio"),
    ExclusionRule("it.geosolutions.imageio-ext"),
    ExclusionRule("jgridshift", "jgridshift"),
    ExclusionRule("jgridshift", "jgridshift-core"),
    ExclusionRule("org.jaitools", "jt-zonalstats"),
    ExclusionRule("org.jaitools", "jt-utils")
  )

  lazy val accumulo = Seq(
    name := "geotrellis-accumulo",
    libraryDependencies ++= Seq(
      accumuloCore
        exclude("org.jboss.netty", "netty")
        exclude("org.apache.hadoop", "hadoop-client"),
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
  ) ++ commonSettings ++ forkInTests

  lazy val `accumulo-spark` = Seq(
    name := "geotrellis-accumulo-spark",
    libraryDependencies ++= Seq(
      accumuloCore
        exclude("org.jboss.netty", "netty")
        exclude("org.apache.hadoop", "hadoop-client"),
      hadoopClient % Provided,
      apacheSpark("core").value % Provided,
      apacheSpark("sql").value % Test,
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
  ) ++ commonSettings

  lazy val cassandra = Seq(
    name := "geotrellis-cassandra",
    libraryDependencies ++= Seq(
      cassandraDriverCore,
      cassandraDriverQueryBuilder
    ) map (_ excludeAll(
      ExclusionRule("io.netty"),
      ExclusionRule("org.slf4j"),
      ExclusionRule("com.fasterxml.jackson.core")
    )),
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
      cassandraDriverCore,
      cassandraDriverQueryBuilder
    ) map (_ excludeAll(
      ExclusionRule("io.netty"),
      ExclusionRule("org.slf4j"),
      ExclusionRule("com.fasterxml.jackson.core")
    )),
    libraryDependencies ++= Seq(
      hadoopClient % Provided,
      apacheSpark("core").value % Provided,
      apacheSpark("sql").value % Test,
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
    scalacOptions ++= commonScalacOptions,
    libraryDependencies ++= Seq(
      apacheSpark("core").value,
      log4jbridge, // CVE-2021-4104, CVE-2020-8908
      scalatest % Test,
      apacheSpark("sql").value % Test
    ),
    libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
  )

  lazy val geotools = Seq(
    name := "geotrellis-geotools",
    libraryDependencies ++= Seq(
      geotoolsCoverage,
      geotoolsHsql,
      geotoolsMain,
      geotoolsReferencing,
      geotoolsMetadata,
      geotoolsOpengis
    ).map(_ excludeAll(excludedDependencies: _*)),
    libraryDependencies ++= Seq(
      unitApi,
      geotoolsGeoTiff % Test,
      geotoolsShapefile % Test,
      scalatest % Test
    ) ++ worksWithDependencies,
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

  lazy val hbase = Seq(
    name := "geotrellis-hbase",
    libraryDependencies +=
      hbaseMapReduce
        exclude("javax.servlet", "servlet-api")
        exclude("org.mortbay.jetty", "servlet-api-2.5"),
    libraryDependencies += jacksonCoreAsl,
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
      hadoopClient % Provided,
      woodstoxCore % Provided,
      stax2Api % Provided,
      commonsConfiguration2 % Provided,
      apacheSpark("core").value % Provided,
      apacheSpark("sql").value % Test,
      scalatest % Test
    ),
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
    libraryDependencies += spireMacro
  ) ++ commonSettings

  lazy val mdoc = Seq(
    name := "geotrellis-mdoc",
    mdocIn := new File("docs-mdoc"),
    mdocOut := new File("website/docs"),
    mdocVariables := Map("VERSION" -> (ThisBuild / version).value),
    libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
  )

  lazy val proj4 = Seq(
    name := "geotrellis-proj4",
    libraryDependencies ++= Seq(
      proj4j,
      openCSV,
      parserCombinators,
      scalatest % Test,
      scalacheck % Test
    ),
    // https://github.com/sbt/sbt/issues/4609
    Test / fork := true
  ) ++ commonSettings

  lazy val raster = Seq(
    name := "geotrellis-raster",
    libraryDependencies ++= Seq(
      squants,
      monocle("core").value,
      monocle("macro").value,
      scalaXml,
      scalaURI,
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
      awsSdkS3 excludeAll ExclusionRule("com.fasterxml.jackson.core"),
      scalatest % Test
    ),
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
      hadoopClient % Provided,
      apacheSpark("core").value % Provided,
      apacheSpark("sql").value % Test,
      scalatest % Test
    ),
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
      geotoolsMain,
      geotoolsOpengis,
      geotoolsShapefile
    ).map(_ excludeAll(excludedDependencies: _*)),
    libraryDependencies ++= Seq(scalatest % Test) ++ worksWithDependencies,
    Test / fork := false
  ) ++ commonSettings

  lazy val spark = Seq(
    name := "geotrellis-spark",
    libraryDependencies ++= Seq(
      woodstoxCore % Provided,
      stax2Api % Provided,
      commonsConfiguration2 % Provided,
      re2j % Provided,
      apacheSpark("core").value % Provided,
      hadoopClient % Provided,
      apacheSpark("sql").value % Test,
      scalatest % Test
    ) ++ sparkCompatDependencies.value,
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
      circe("generic-extras").value,
      hadoopClient % Provided,
      apacheSpark("core").value % Provided,
      apacheSpark("sql").value % Test,
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
        ShadeRule.rename("shapeless.**" -> s"$shadePackage.shapeless.@1").inAll,
        ShadeRule.rename("cats.kernel.**" -> s"$shadePackage.cats.kernel.@1").inAll
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
      hadoopClient % Provided,
      apacheSpark("core").value % Provided,
      apacheSpark("sql").value % Provided,
      scalatest
    )
  ) ++ commonSettings

  lazy val util = Seq(
    name := "geotrellis-util",
    libraryDependencies ++= Seq(
      log4s,
      scalaj,
      spire,
      scalatest % Test
    )
  ) ++ commonSettings

  lazy val vector = Seq(
    name := "geotrellis-vector",
    libraryDependencies ++= Seq(
      jts,
      shapeless,
      pureconfig,
      circe("core").value,
      circe("generic").value,
      circe("parser").value,
      cats("core").value,
      apacheMath,
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
      scalapbRuntime % "protobuf",
      scalapbLenses,
      protobufJava,
      scalatest % Test
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
      avro,
      chronoscala,
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
      scaffeine,
      caffeine,
      uzaygezenCore,
      scalaXml,
      apacheLang3,
      fs2("core").value,
      fs2("io").value,
      cats("effect").value,
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
    Test / fork := true,
    Test / parallelExecution := false,
    Test / testOptions += Tests.Argument("-oDF"),
    javaOptions ++= Seq("-Djava.library.path=/usr/local/lib")
  ) ++ commonSettings

  lazy val `gdal-spark` = Seq(
    name := "geotrellis-gdal-spark",
    libraryDependencies ++= Seq(
      gdalWarp,
      hadoopClient % Provided,
      apacheSpark("core").value % Provided,
      apacheSpark("sql").value % Test,
      scalatest % Test
    ),
    Test / fork := true,
    Test / parallelExecution := false,
    Test / testOptions += Tests.Argument("-oDF"),
    javaOptions ++= Seq("-Djava.library.path=/usr/local/lib")
  ) ++ commonSettings
}

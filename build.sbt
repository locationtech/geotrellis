import Dependencies._
import de.heikoseeberger.sbtheader._
import sbt.Keys._

ThisBuild / scalaVersion := "2.12.8"
ThisBuild / crossScalaVersions := List("2.12.8", "2.11.12")
ThisBuild / organization := "org.locationtech.geotrellis"

lazy val commonSettings = Seq(
  description := Info.description,
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  homepage := Some(url("http://geotrellis.io")),
  scmInfo := Some(ScmInfo(
    url("https://github.com/locationtech/geotrellis"), "scm:git:git@github.com:locationtech/geotrellis.git"
  )),
  scalacOptions ++= Seq(
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
    "-target:jvm-1.8"
  ),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  autoAPIMappings := true,

  publishTo := {
    val sonatype = "https://oss.sonatype.org/"
    val locationtech = "https://repo.locationtech.org/content/repositories"
    // a default repository would be Sonatype
    System.getProperty("release") match {
      case "locationtech" if isSnapshot.value =>
        Some("LocationTech Snapshot Repository" at s"${locationtech}/geotrellis-snapshots")
      case "locationtech" =>
        Some("LocationTech Release Repository" at s"${locationtech}/geotrellis-releases")
      case _ =>
        Some("Sonatype Release Repository" at s"${sonatype}service/local/staging/deploy/maven2")
    }
  },

  credentials ++= List(Path.userHome / ".ivy2" / ".credentials")
    .filter(_.asFile.canRead)
    .map(Credentials(_)),

  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3" cross CrossVersion.binary),
  addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.1" cross CrossVersion.full),

  pomExtra := (
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
    </developers>),

  shellPrompt := { s => Project.extract(s).currentProject.id + " > " },
  resolvers ++= Seq(
    Resolver.mavenLocal,
    Settings.Repositories.geosolutions,
    Settings.Repositories.osgeo,
    Settings.Repositories.locationtechReleases,
    Settings.Repositories.locationtechSnapshots
  ),
  headerLicense := Some(HeaderLicense.ALv2("2019", "Azavea")),
  // preserve year of old headers
  headerMappings :=
    Map(FileType.scala -> CommentStyle.cStyleBlockComment.copy(commentCreator = new CommentCreator() {
      val Pattern = "(?s).*?(\\d{4}(-\\d{4})?).*".r
      def findYear(header: String): Option[String] = header match {
        case Pattern(years, _) => Some(years)
        case _                 => None
      }
      override def apply(text: String, existingText: Option[String]): String = {
        val newText = CommentStyle.cStyleBlockComment.commentCreator.apply(text, existingText)
        existingText.flatMap { text =>
          if (text.contains("Azavea")) {
            findYear(text).map(year => newText.replace("2019", year))
          } else {
            existingText.map(_.trim)
          }
        }.getOrElse(newText)
      } } )),
  updateOptions := updateOptions.value.withGigahorse(false)
)

lazy val root = Project("geotrellis", file("."))
  .aggregate(
    accumulo,
    `accumulo-spark`,
    cassandra,
    `cassandra-spark`,
    `doc-examples`,
    gdal,
    `gdal-spark`,
    geotools,
    hbase,
    `hbase-spark`,
    layer,
    macros,
    proj4,
    raster,
    `raster-testkit`,
    s3,
    `s3-spark`,
    shapefile,
    spark,
    `spark-pipeline`,
    `spark-testkit`,
    store,
    util,
    vector,
    `vector-testkit`,
    vectortile
  )
  .enablePlugins(ScalaUnidocPlugin)
  .settings(unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject)

lazy val macros = project
  .settings(commonSettings)
  .settings(Settings.macros)

lazy val vectortile = project
  .dependsOn(vector)
  .settings(commonSettings)
  .settings(Settings.vectortile)

lazy val vector = project
  .dependsOn(proj4, util)
  .settings(commonSettings)
  .settings(Settings.vector)
  .settings(
    unmanagedClasspath in Test ++= (fullClasspath in (LocalProject("vector-testkit"), Compile)).value
  )

lazy val `vector-testkit` = project
  .dependsOn(vector % Provided)
  .settings(commonSettings)
  .settings(Settings.`vector-testkit`)

lazy val proj4 = project
  .settings(commonSettings)
  .settings(Settings.proj4)
  .settings(javacOptions ++= Seq("-encoding", "UTF-8"))

lazy val raster = project
  .dependsOn(util, macros, vector)
  .settings(commonSettings)
  .settings(Settings.raster)
  .settings(
    unmanagedClasspath in Test ++= (fullClasspath in (LocalProject("raster-testkit"), Compile)).value
  )
  .settings(
    unmanagedClasspath in Test ++= (fullClasspath in (LocalProject("vector-testkit"), Compile)).value
  )

lazy val `raster-testkit` = project
  .dependsOn(raster % Provided, vector % Provided)
  .settings(commonSettings)
  .settings(Settings.`raster-testkit`)

lazy val spark = project
  .dependsOn(util, raster, `raster-testkit` % Test, `vector-testkit` % Test, layer, store)
  .settings(commonSettings)
  .settings(Settings.spark)
  .settings(
    // This takes care of a pseudo-cyclic dependency between the `spark` test scope, `spark-testkit`,
    // and `spark` main (compile) scope. sbt is happy with this. IntelliJ requires that `spark-testkit`
    // be added to the `spark` module dependencies manually (via "Open Module Settings" context menu for "spark" module).
    unmanagedClasspath in Test ++= (fullClasspath in (LocalProject("spark-testkit"), Compile)).value
  )

lazy val `spark-testkit` = project
  .dependsOn(`raster-testkit`, spark)
  .settings(commonSettings)
  .settings(Settings.`spark-testkit`)

lazy val s3 = project
  .dependsOn(store)
  .settings(commonSettings)
  .settings(Settings.s3)

lazy val `s3-spark` = project
  .dependsOn(
    spark % "compile->compile;test->test",  // <-- spark-testkit update should simplify this
    s3,
    `spark-testkit` % Test
  )
  .settings(commonSettings)
  .settings(Settings.`s3-spark`)

lazy val accumulo = project
  .dependsOn(store)
  .settings(commonSettings)
  .settings(Settings.accumulo)

lazy val `accumulo-spark` = project
  .dependsOn(
    `accumulo`,
    spark % "compile->compile;test->test", // <-- spark-testkit update should simplify this
    `spark-testkit` % Test
  )
  .settings(commonSettings)
  .settings(Settings.`accumulo-spark`)

lazy val cassandra = project
  .dependsOn(store)
  .settings(commonSettings)
  .settings(Settings.cassandra)

lazy val `cassandra-spark` = project
  .dependsOn(
    cassandra,
    spark % "compile->compile;test->test", // <-- spark-testkit update should simplify this
    `spark-testkit` % Test
  )
  .settings(commonSettings)
  .settings(Settings.`cassandra-spark`)

lazy val hbase = project
  .dependsOn(store)
  .settings(commonSettings) // HBase depends on its own protobuf version
  .settings(Settings.hbase)
  .settings(projectDependencies := { Seq((projectID in layer).value.exclude("com.google.protobuf", "protobuf-java")) })

lazy val `hbase-spark` = project
  .dependsOn(
    hbase,
    spark % "compile->compile;test->test", // <-- spark-testkit update should simplify this
    `spark-testkit` % Test
  )
  .settings(commonSettings) // HBase depends on its own protobuf version
  .settings(Settings.`hbase-spark`)
  .settings(projectDependencies := { Seq((projectID in spark).value.exclude("com.google.protobuf", "protobuf-java")) })

lazy val `spark-pipeline` = Project(id = "spark-pipeline", base = file("spark-pipeline")).
  dependsOn(spark, `s3-spark`, `spark-testkit` % "test").
  settings(commonSettings)
  .settings(Settings.`spark-pipeline`)

lazy val geotools = project
  .dependsOn(raster, vector, proj4, `vector-testkit` % Test, `raster-testkit` % Test,
    raster % "test->test" // <-- to get rid  of this, move `GeoTiffTestUtils` to the testkit.
  )
  .settings(commonSettings)
  .settings(Settings.geotools)

// lazy val geomesa = project
//   .dependsOn(`spark-testkit` % Test, spark, geotools, `accumulo-spark`)
//   .settings(commonSettings)
//   .settings(Settings.geomesa)
//   .settings(crossScalaVersions := Seq(scalaVersion.value))

// lazy val geowave = project
//   .dependsOn(
//     proj4, raster, layer, store, accumulo,
//     `spark-testkit` % Test, geotools
//   )
//   .settings(commonSettings)
//   .settings(Settings.geowave)
//   .settings(crossScalaVersions := Seq(scalaVersion.value))

lazy val shapefile = project
  .dependsOn(raster, `raster-testkit` % Test)
  .settings(commonSettings)
  .settings(Settings.shapefile)

lazy val util = project
  .settings(commonSettings)
  .settings(Settings.util)

lazy val `doc-examples` = project
  .dependsOn(spark, `s3-spark`, `accumulo-spark`, `cassandra-spark`, `hbase-spark`, spark, `spark-testkit`, `spark-pipeline`)
  .settings(commonSettings)
  .settings(Settings.`doc-examples`)

lazy val bench = project
  .dependsOn(raster, spark)
  .enablePlugins(GTBenchmarkPlugin)
  .settings(commonSettings)
  .settings(Settings.bench)

lazy val layer = project
  .dependsOn(raster, `raster-testkit` % Test)
  .settings(commonSettings)
  .settings(Settings.layer)

lazy val store = project
  .dependsOn(layer)
  .settings(commonSettings)
  .settings(Settings.store)

lazy val gdal = project
  .dependsOn(raster, `raster-testkit` % Test)
  .settings(commonSettings)
  .settings(Settings.gdal)

lazy val `gdal-spark` = project
  .dependsOn(gdal, spark, `spark-testkit` % Test)
  .settings(commonSettings)
  .settings(publish / skip := true) // at this point we need this project only for tests
  .settings(Settings.`gdal-spark`)

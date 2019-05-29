import Dependencies._
import de.heikoseeberger.sbtheader._
import sbt.Keys._

scalaVersion := Version.scala

scalaVersion in ThisBuild := Version.scala

lazy val commonSettings = Seq(
  version := Version.geotrellis,
  scalaVersion := Version.scala,
  crossScalaVersions := Version.crossScala,
  description := Info.description,
  organization := "org.locationtech.geotrellis",
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  homepage := Some(url(Info.url)),
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

  publishTo := {
    val sonatype = "https://oss.sonatype.org/"
    val locationtech = "https://repo.locationtech.org/content/repositories"
    if (isSnapshot.value) {
      // Publish snapshots to LocationTech
      Some("LocationTech Snapshot Repository" at s"${locationtech}/geotrellis-snapshots")
    } else {
      val milestoneRx = """-(M|RC)\d+$""".r
      milestoneRx.findFirstIn(Version.geotrellis) match {
        case Some(v) =>
          // Public milestones to LocationTech
          Some("LocationTech Release Repository" at s"${locationtech}/geotrellis-releases")
        case None =>
          // Publish releases to Sonatype
          Some("Sonatype Release Repository" at s"${sonatype}service/local/staging/deploy/maven2")
      }
    }
  },

  credentials ++= List(Path.userHome / ".ivy2" / ".credentials")
    .filter(_.asFile.canRead)
    .map(Credentials(_)),

  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.10" cross CrossVersion.binary),
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
    "geosolutions" at "http://maven.geo-solutions.it/",
    "osgeo" at "http://download.osgeo.org/webdav/geotools/"
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
        existingText
          .flatMap(findYear)
          .map(year => newText.replace("2018", year))
          .getOrElse(newText)
      } } )),
  scapegoatVersion in ThisBuild := "1.3.8",
  updateOptions := updateOptions.value.withGigahorse(false)
)

lazy val root = Project("geotrellis", file(".")).
  aggregate(
    accumulo,
    `layers-accumulo`,
    cassandra,
    `layers-cassandra`,
    `doc-examples`,
    geomesa,
    geotools,
    geowave,
    `hbase-store`,
    `hbase-spark`,
    macros,
    proj4,
    raster,
    `raster-testkit`,
    s3,
    shapefile,
    spark,
    `spark-etl`,
    `spark-pipeline`,
    `spark-testkit`,
    util,
    vector,
    `vector-testkit`,
    vectortile
  ).
  settings(commonSettings: _*).
  enablePlugins(ScalaUnidocPlugin).
  settings(
    initialCommands in console :=
      """
      import geotrellis.raster._
      import geotrellis.vector._
      import geotrellis.proj4._
      import geotrellis.spark._
      """
  ).
  settings(unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(geowave))

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
  .dependsOn(raster % Provided, vector % Provided)
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
  .dependsOn(util, raster, `raster-testkit` % Test, `vector-testkit` % Test, tiling, layers)
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
  .dependsOn(
    spark % "compile->compile;test->test",  // <-- spark-testkit update should simplify this
    `spark-testkit` % Test
  )
  .settings(commonSettings)
  .settings(Settings.s3)

lazy val accumulo = project
  .dependsOn(
    `layers-accumulo`,
    spark % "compile->compile;test->test", // <-- spark-testkit update should simplify this
    `spark-testkit` % Test
  )
  .settings(commonSettings)
  .settings(Settings.accumulo)

lazy val `layers-accumulo` = project
  .dependsOn(layers)
  .settings(commonSettings)
  .settings(Settings.`layers-accumulo`)

lazy val `layers-cassandra` = project
  .dependsOn(layers)
  .settings(commonSettings)
  .settings(Settings.`layers-cassandra`)

lazy val cassandra = project
  .dependsOn(
    `layers-cassandra`,
    spark % "compile->compile;test->test", // <-- spark-testkit update should simplify this
    `spark-testkit` % Test
  )
  .settings(commonSettings)
  .settings(Settings.cassandra)

lazy val `hbase-store` = project
  .dependsOn(
    spark % "compile->compile;test->test", // <-- spark-testkit update should simplify this
    `spark-testkit` % Test
  )
  .settings(commonSettings) // HBase depends on its own protobuf version
  .settings(Settings.hbase)
  .settings(projectDependencies := { Seq((projectID in spark).value.exclude("com.google.protobuf", "protobuf-java")) })

lazy val `hbase-spark` = project
  .dependsOn(
    `hbase-store` % "compile->compile;test->test",
    spark % "compile->compile;test->test", // <-- spark-testkit update should simplify this
    `spark-testkit` % Test
  )
  .settings(commonSettings) // HBase depends on its own protobuf version
  .settings(Settings.hbase)
  .settings(projectDependencies := { Seq((projectID in spark).value.exclude("com.google.protobuf", "protobuf-java")) })

lazy val `spark-etl` = Project(id = "spark-etl", base = file("spark-etl"))
  .dependsOn(spark, s3, accumulo, cassandra, `hbase-store`, `hbase-spark`)
  .settings(commonSettings)
  .settings(Settings.`spark-etl`)

lazy val `spark-pipeline` = Project(id = "spark-pipeline", base = file("spark-pipeline")).
  dependsOn(spark, s3, `spark-testkit` % "test").
  settings(commonSettings)
  .settings(Settings.`spark-pipeline`)

lazy val geotools = project
  .dependsOn(raster, vector, proj4, `vector-testkit` % Test, `raster-testkit` % Test,
    `raster` % "test->test" // <-- to get rid  of this, move `GeoTiffTestUtils` to the testkit.
  )
  .settings(commonSettings)
  .settings(Settings.geotools)

lazy val geomesa = project
  .dependsOn(`spark-testkit` % Test, spark, geotools, accumulo)
  .settings(commonSettings)
  .settings(Settings.geomesa)
  .settings(crossScalaVersions := Seq(scalaVersion.value))

lazy val geowave = project
  .dependsOn(
    spark % "compile->compile;test->test", // <-- spark-testkit update should simplify this
    `spark-testkit` % Test, geotools, accumulo
  )
  .settings(commonSettings)
  .settings(Settings.geowave)
  .settings(crossScalaVersions := Seq(scalaVersion.value))

lazy val shapefile = project
  .dependsOn(raster, `raster-testkit` % Test)
  .settings(commonSettings)
  .settings(Settings.shapefile)

lazy val util = project
  .settings(commonSettings)
  .settings(Settings.util)

lazy val `doc-examples` = project
  .dependsOn(spark, s3, accumulo, cassandra, `hbase-store`, `hbase-spark`, spark, `spark-testkit`, `spark-pipeline`)
  .settings(commonSettings)
  .settings(Settings.`doc-examples`)

lazy val bench = project
  .dependsOn(spark)
  .settings(commonSettings)
  .settings(Settings.bench)

lazy val tiling = project
  .dependsOn(raster, vector, proj4, util)
  .settings(commonSettings)
  .settings(Settings.tiling)

lazy val layers = project
  .dependsOn(tiling)
  .settings(commonSettings)
  .settings(Settings.layers)

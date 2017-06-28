import Dependencies._
import sbt.Keys._
import de.heikoseeberger.sbtheader.license.Apache2_0

lazy val commonSettings = Seq(
  version := Version.geotrellis,
  scalaVersion := Version.scala,
  description := Info.description,
  organization := "org.locationtech.geotrellis",
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  homepage := Some(url(Info.url)),
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
    "-feature"),
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
      // Publish releases to Sonatype
      Some("Sonatype Release Repository" at s"${sonatype}service/local/staging/deploy/maven2")
    }
  },

  credentials ++= List(Path.userHome / ".ivy2" / ".credentials")
    .filter(_.asFile.canRead)
    .map(Credentials(_)),

  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.4" cross CrossVersion.binary),
  addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.full),

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
      </developers>),
  shellPrompt := { s => Project.extract(s).currentProject.id + " > " },
  dependencyUpdatesExclusions := moduleFilter(organization = "org.scala-lang"),

  resolvers ++= Seq(
    "geosolutions" at "http://maven.geo-solutions.it/",
    "osgeo" at "http://download.osgeo.org/webdav/geotools/"
  ),
  headers := Map(
    "scala" -> Apache2_0("2016", "Azavea"),
    "conf" -> Apache2_0("2016", "Azavea", "#")
  )
)

lazy val root = Project("geotrellis", file(".")).
  aggregate(
    accumulo,
    cassandra,
    `doc-examples`,
    geomesa,
    geotools,
    geowave,
    hbase,
    macros,
    proj4,
    raster,
    `raster-test`,
    `raster-testkit`,
    s3,
    `s3-test`,
    `s3-testkit`,
    shapefile,
    slick,
    spark,
    `spark-etl`,
    `spark-testkit`,
    util,
    vector,
    `vector-test`,
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

lazy val vectortile = project
  .dependsOn(vector)
  .settings(commonSettings)

lazy val vector = project
  .dependsOn(proj4, util)
  .settings(commonSettings)

lazy val `vector-test` = project
  .dependsOn(vector, `vector-testkit`)
  .settings(commonSettings)

lazy val `vector-testkit` = project
  .dependsOn(raster, vector)
  .settings(commonSettings)

lazy val proj4 = project
  .settings(commonSettings)
  .settings(javacOptions ++= Seq("-encoding", "UTF-8"))

lazy val raster = project
  .dependsOn(util, macros, vector)
  .settings(commonSettings)

lazy val `raster-test` = project
  .dependsOn(raster, `raster-testkit`, `vector-testkit`)
  .settings(commonSettings)

lazy val `raster-testkit` = project
  .dependsOn(raster, vector)
  .settings(commonSettings)

lazy val slick = project
  .dependsOn(vector)
  .settings(commonSettings)

lazy val spark = project
  .dependsOn(util, vectortile, raster, `raster-testkit` % "test")
  .settings(commonSettings)
  .settings(
    // This takes care of a pseudo-cyclic dependency between the `spark` test scope, `spark-testkit`,
    // and `spark` main (compile) scope. sbt is happy with this. IntelliJ requires that `spark-testkit`
    // be added to the `spark` module dependencies manually (via "Open Module Settings" context menu for "spark" module).
    unmanagedClasspath in Test ++= (fullClasspath in (LocalProject("spark-testkit"), Compile)).value
  )

lazy val `spark-testkit` = project
  .dependsOn(`raster-testkit`, spark)
  .settings(commonSettings)

lazy val s3 = project
  .dependsOn(spark)
  .settings(commonSettings)

lazy val `s3-test` = project
  .dependsOn(
    s3, `s3-testkit`,
    spark % "compile->compile;test->test", // <-- spark-testkit update should simplify this
    `spark-testkit`
  )
  .settings(commonSettings)

lazy val `s3-testkit` = project
  .dependsOn(s3, spark)
  .settings(commonSettings)

lazy val accumulo = project
  .dependsOn(
    spark % "compile->compile;test->test", // <-- spark-testkit update should simplify this
    `spark-testkit` % "test"
  )
  .settings(commonSettings)

lazy val cassandra = project
  .dependsOn(
    spark % "compile->compile;test->test", // <-- spark-testkit update should simplify this
    `spark-testkit` % "test"
  )
  .settings(commonSettings)

lazy val hbase = project
  .dependsOn(
    spark % "compile->compile;test->test", // <-- spark-testkit update should simplify this
    `spark-testkit` % "test"
  )
  .settings(commonSettings) // HBase depends on its own protobuf version
  .settings(projectDependencies := { Seq((projectID in spark).value.exclude("com.google.protobuf", "protobuf-java")) })

lazy val `spark-etl` = Project(id = "spark-etl", base = file("spark-etl")).
  dependsOn(spark, s3, accumulo, cassandra, hbase).
  settings(commonSettings)

lazy val geotools = project
  .dependsOn(raster, vector, proj4, `vector-testkit` % "test", `raster-testkit` % "test",
    `raster-test` % "test->test" // <-- to get rid  of this, move `GeoTiffTestUtils` to the testkit.
  )
  .settings(commonSettings)

lazy val geomesa = project
  .dependsOn(`spark-testkit` % "test", spark, geotools, accumulo)
  .settings(commonSettings)

lazy val geowave = project
  .dependsOn(
    spark % "compile->compile;test->test", // <-- spark-testkit update should simplify this
    `spark-testkit` % "test", geotools, accumulo
  )
  .settings(commonSettings)

lazy val shapefile = project
  .dependsOn(raster, `raster-testkit` % "test")
  .settings(commonSettings)

lazy val util = project
  .settings(commonSettings)

lazy val `doc-examples` = project
  .dependsOn(spark, s3, accumulo, cassandra, hbase, spark, `spark-testkit`)
  .settings(commonSettings)

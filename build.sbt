import Dependencies._
import UnidocKeys._

lazy val commonSettings = Seq(
  version := Version.geotrellis,
  scalaVersion := Version.scala,
  crossScalaVersions := Version.crossScala,
  description := Info.description,
  organization := "com.azavea.geotrellis",
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

  bintrayOrganization := Some("azavea"),
  bintrayRepository := "geotrellis",
  bintrayVcsUrl := Some("https://github.com/geotrellis/geotrellis.git"),
  bintrayPackageLabels := Info.tags,

  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.7.1" cross CrossVersion.binary),

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
  )
)

lazy val root = Project("geotrellis", file(".")).
  aggregate(
    raster,
    rasterTest,
    vector,
    vectorTest,
    proj4,
    spark,
    sparkEtl,
    s3,
    accumulo,
    cassandra,
    hbase,
    geotools,
    slick
  ).
  settings(commonSettings: _*).
  settings(
    scalacOptions in (ScalaUnidoc, unidoc) += "-Ymacro-expand:none",
    initialCommands in console :=
      """
      import geotrellis.raster._
      import geotrellis.vector._
      import geotrellis.proj4._
      import geotrellis.spark._
      """
  )
  .settings(unidocSettings: _*)

lazy val macros = Project("macros", file("macros")).
  settings(commonSettings: _*)

lazy val vector = Project("vector", file("vector")).
  dependsOn(proj4, util).
  settings(commonSettings: _*)

lazy val vectorTest = Project("vector-test", file("vector-test")).
  dependsOn(vector, vectorTestkit)

lazy val vectorTestkit = Project("vector-testkit", file("vector-testkit")).
  dependsOn(raster, vector).
  settings(commonSettings: _*)

lazy val proj4 = Project("proj4", file("proj4")).
  settings(commonSettings: _*).
  settings(javacOptions ++= Seq("-encoding", "UTF-8"))

lazy val raster = Project("raster", file("raster")).
  dependsOn(util, macros, vector).
  settings(commonSettings: _*)

lazy val rasterTest = Project("raster-test", file("raster-test")).
  dependsOn(raster, rasterTestkit, vectorTestkit).
  settings(commonSettings: _*)

lazy val rasterTestkit = Project("raster-testkit", file("raster-testkit")).
  dependsOn(raster, vector).
  settings(commonSettings: _*)

lazy val slick = Project("slick", file("slick")).
  dependsOn(vector).
  settings(commonSettings: _*)

lazy val spark = Project("spark", file("spark")).
  dependsOn(util, raster, rasterTestkit % "provided;test->test").
  settings(commonSettings: _*)

lazy val sparkTestkit: Project = Project("spark-testkit", file("spark-testkit")).
  dependsOn(rasterTestkit, spark % "provided").
  settings(commonSettings: _*)

lazy val s3 = Project("s3", file("s3")).
  dependsOn(sparkTestkit % "test->test", spark % "provided;test->test").
  settings(commonSettings: _*)

lazy val accumulo = Project("accumulo", file("accumulo")).
  dependsOn(sparkTestkit % "test->test", spark % "provided;test->test").
  settings(commonSettings: _*)

lazy val cassandra = Project("cassandra", file("cassandra")).
  dependsOn(sparkTestkit % "test->test", spark % "provided;test->test").
  settings(commonSettings: _*)

lazy val hbase = Project("hbase", file("hbase")).
  dependsOn(sparkTestkit % "test->test", spark % "provided;test->test").
  settings(commonSettings: _*)

lazy val sparkEtl = Project(id = "spark-etl", base = file("spark-etl")).
  dependsOn(spark, s3, accumulo, cassandra, hbase).
  settings(commonSettings: _*)

lazy val geotools = Project("geotools", file("geotools")).
  dependsOn(raster, vector, proj4, vectorTestkit % "test->test", rasterTest % "test->test").
  settings(commonSettings: _*)

lazy val shapefile = Project("shapefile", file("shapefile")).
  dependsOn(raster, rasterTestkit % "test").
  settings(commonSettings: _*)

lazy val util = Project("util", file("util")).
  settings(commonSettings: _*)

lazy val docExamples = Project("doc-examples", file("doc-examples")).
  dependsOn(spark, s3, accumulo, cassandra, hbase, spark % "test->test", sparkTestkit % "test->test").
  settings(commonSettings: _*)

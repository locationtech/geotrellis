import Dependencies._

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
    "-Yinline-warnings",
    "-language:implicitConversions",
    "-language:reflectiveCalls",
    "-language:higherKinds",
    "-language:postfixOps",
    "-language:existentials",
    "-feature"),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },

  bintrayOrganization := Some("azavea"),
  bintrayRepository := "geotrellis",
  bintrayVcsUrl := Some("https://github.com/geotrellis/geotrellis.git"),
  bintrayPackageLabels := Info.tags,

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
  shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
) ++ net.virtualvoid.sbt.graph.Plugin.graphSettings

lazy val root = Project("geotrellis", file(".")).
  dependsOn(raster, vector, proj4).
  settings(
    initialCommands in console :=
      """
      import geotrellis.raster._
      import geotrellis.vector._
      import geotrellis.proj4._
      """
  )

lazy val macros = Project("macros", file("macros")).
  settings(commonSettings: _*)

lazy val vector = Project("vector", file("vector")).
  dependsOn(proj4).
  settings(commonSettings: _*)

lazy val vectorTest = Project("vector-test", file("vector-test")).
  dependsOn(vector, testkit)

lazy val proj4 = Project("proj4", file("proj4")).
  settings(commonSettings: _*)

lazy val raster = Project("raster", file("raster")).
  dependsOn(macros, vector).
  settings(commonSettings: _*)

lazy val rasterTest = Project("raster-test", file("raster-test")).
  dependsOn(raster, testkit).
  settings(commonSettings: _*)

lazy val engine = Project("engine", file("engine")).
  dependsOn(raster).
  settings(commonSettings: _*)

lazy val engineTest = Project("engine-test", file("engine-test")).
  dependsOn(engine, testkit).
  settings(commonSettings: _*)

lazy val testkit = Project("testkit", file("testkit")).
  dependsOn(raster, engine).
  settings(commonSettings: _*)

lazy val services = Project("services", file("services")).
  dependsOn(raster, vector, engine).
  settings(commonSettings: _*)

lazy val jetty = Project("jetty", file("jetty")).
  dependsOn(services).
  settings(commonSettings: _*)

lazy val geotrellisSlick = Project("slick", file("slick")).
  dependsOn(vector).
  settings(commonSettings: _*)

lazy val examples = Project("examples", file("examples")).
  dependsOn(raster, vector).
  settings(commonSettings: _*)

lazy val admin = Project("admin", file("admin")).
  dependsOn(raster, services, vector).
  settings(commonSettings: _*)

lazy val spark = Project("spark", file("spark")).
  dependsOn(raster, gdal).
  settings(commonSettings: _*)

lazy val sparkEtl = Project(id = "spark-etl", base = file("spark-etl")).
  dependsOn(spark).
  settings(commonSettings: _*)
  
lazy val graph = Project("graph", file("graph")).
  dependsOn(spark % "test->test;compile->compile").
  settings(commonSettings: _*)

lazy val index = Project("index", file("index")).
  settings(commonSettings: _*)

lazy val gdal: Project = Project("gdal", file("gdal")).
  dependsOn(raster, geotools % "test").
  settings(commonSettings: _*)

lazy val geotools = Project("geotools", file("geotools")).
  dependsOn(raster, engine, testkit % "test").
  settings(commonSettings: _*)

lazy val dev = Project("dev", file("dev")).
  dependsOn(raster, engine).
  settings(commonSettings: _*)

lazy val demo = Project("demo", file("demo")).
  dependsOn(jetty).
  settings(commonSettings: _*)

lazy val vectorBenchmark: Project = Project("vector-benchmark", file("vector-benchmark")).
  dependsOn(vectorTest % "compile->test").
  settings(commonSettings: _*)

lazy val benchmark: Project = Project("benchmark", file("benchmark")).
  dependsOn(raster, engine, geotools, jetty).
  settings(commonSettings: _*)

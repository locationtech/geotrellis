import sbt.Keys._

import java.io.File

ThisBuild / scalaVersion := "2.12.15"
ThisBuild / organization := "org.locationtech.geotrellis"
ThisBuild / crossScalaVersions := List("2.12.15", "2.13.6")

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
    // geowave,
    hbase,
    `hbase-spark`,
    layer,
    macros,
    mdoc,
    proj4,
    raster,
    `raster-testkit`,
    `raster-tests`,
    s3,
    `s3-spark`,
    shapefile,
    spark,
    `spark-pipeline`,
    `spark-testkit`,
    `spark-tests`,
    store,
    util,
    vector,
    `vector-testkit`,
    `vector-tests`,
    vectortile
  )
  .enablePlugins(ScalaUnidocPlugin)
  .settings(Settings.commonSettings)
  .settings(publish / skip := true)
  .settings(ScalaUnidoc / unidoc / unidocProjectFilter := inAnyProject -- inProjects(mdoc))
  .settings(
    initialize := {
      val curr = VersionNumber(sys.props("java.specification.version"))
      val req = SemanticSelector("=1.8")
      if (!curr.matchesSemVer(req)) {
        val log = Keys.sLog.value
        log.warn(s"Java $req required for GeoTools compatibility. Found Java $curr.\n" +
          "Please change the version of Java running sbt.")
      }
    }
  )

lazy val mdoc = project
  .dependsOn(raster)
  .enablePlugins(MdocPlugin)
  .settings(publish / skip := true)
  .settings(Settings.mdoc)

lazy val macros = project
  .settings(Settings.macros)

lazy val vectortile = project
  .dependsOn(vector)
  .settings(Settings.vectortile)

lazy val vector = project
  .dependsOn(proj4, util)
  .settings(Settings.vector)

lazy val `vector-testkit` = project
  .dependsOn(vector)
  .settings(Settings.`vector-testkit`)

lazy val `vector-tests` = project
  .dependsOn(vector, `vector-testkit`)
  .settings(Settings.testModuleSettings)

lazy val proj4 = project
  .settings(Settings.proj4)
  .settings(javacOptions ++= Seq("-encoding", "UTF-8"))

lazy val raster = project
  .dependsOn(util, macros, vector)
  .settings(Settings.raster)

lazy val `raster-testkit` = project
  .dependsOn(raster, vector)
  .settings(Settings.`raster-testkit`)

lazy val `raster-tests` = project
  .dependsOn(raster, `raster-testkit`, `vector-testkit`)
  .settings(Settings.testModuleSettings)
  .settings(Settings.geoTiffTestFiles)

lazy val spark = project
  .dependsOn(util, raster, `raster-testkit` % Test, `vector-testkit` % Test, layer, store)
  .settings(Settings.spark)

lazy val `spark-testkit` = project
  .dependsOn(spark, `raster-testkit`, `vector-testkit`)
  .settings(Settings.`spark-testkit`)

lazy val `spark-tests` = project
  .dependsOn(spark, `spark-testkit`, `vector-testkit`)
  .settings(Settings.testModuleSettings)
  .settings(
    Test / testOptions += Tests.Argument("-oD"),
  )

lazy val s3 = project
  .dependsOn(store)
  .settings(Settings.s3)

lazy val `s3-spark` = project
  .dependsOn(
    spark,
    s3,
    `spark-testkit` % Test,
    `spark-tests` % "test->test"
  )
  .settings(Settings.`s3-spark`)

lazy val accumulo = project
  .dependsOn(store)
  .settings(Settings.accumulo)

lazy val `accumulo-spark` = project
  .dependsOn(
    `accumulo`,
    spark,
    `spark-testkit` % Test,
    `spark-tests` % "test->test"
  )
  .settings(Settings.`accumulo-spark`)

lazy val cassandra = project
  .dependsOn(store)
  .settings(Settings.cassandra)

lazy val `cassandra-spark` = project
  .dependsOn(
    cassandra,
    spark,
    `spark-testkit` % Test,
    `spark-tests` % "test->test"
  )
  .settings(Settings.`cassandra-spark`)

lazy val hbase = project
  .dependsOn(store)
  .settings(projectDependencies := { Seq((store / projectID).value.exclude("com.google.protobuf", "protobuf-java")) })
  .settings(Settings.hbase)

lazy val `hbase-spark` = project
  .dependsOn(
    hbase,
    spark,
    `spark-testkit` % Test,
    `spark-tests` % "test->test"
  )
  .settings(projectDependencies := { Seq((hbase / projectID).value, (spark / projectID).value.exclude("com.google.protobuf", "protobuf-java")) })
  .settings(Settings.`hbase-spark`)

lazy val `spark-pipeline` = project.
  dependsOn(spark, `s3-spark`, `spark-testkit` % "test").
  settings(Settings.`spark-pipeline`)

lazy val geotools = project
  .dependsOn(raster, vector, proj4, `vector-testkit` % Test, `raster-testkit` % Test,
    raster
  )
  .settings(Settings.geotools)

lazy val geowave = project
  .dependsOn(raster, store, `raster-testkit` % Test)
  .settings(Settings.geowave)

lazy val `geowave-benchmark` = (project in file("geowave/benchmark"))
  .dependsOn(geowave)
  .enablePlugins(JmhPlugin)
  .settings(Settings.geowaveBenchmark)
  .settings(publish / skip := true)

lazy val shapefile = project
  .dependsOn(raster, `raster-testkit` % Test)
  .settings(Settings.shapefile)

lazy val util = project
  .settings(Settings.util)

lazy val `doc-examples` = project
  .dependsOn(spark, `s3-spark`, `accumulo-spark`, `cassandra-spark`, `hbase-spark`, spark, `spark-testkit`, `spark-pipeline`)
  .settings(publish / skip := true)
  .settings(Settings.`doc-examples`)

lazy val bench = project
  .dependsOn(raster, spark)
  .enablePlugins(GTBenchmarkPlugin)
  .settings(Settings.bench)

lazy val layer = project
  .dependsOn(raster, `raster-testkit` % Test)
  .settings(Settings.layer)

lazy val store = project
  .dependsOn(layer)
  .settings(Settings.store)

lazy val gdal = project
  .dependsOn(raster, `raster-testkit` % Test)
  .settings(Settings.gdal)

lazy val `gdal-spark` = project
  .dependsOn(gdal, spark, `spark-testkit` % Test)
  .settings(publish / skip := true) // at this point we need this project only for tests
  .settings(Settings.`gdal-spark`)

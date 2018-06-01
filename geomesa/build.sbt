import Dependencies._

name := "geotrellis-geomesa"
libraryDependencies ++= Seq(
  "org.locationtech.geomesa" %% "geomesa-jobs" % Version.geomesa,
  "org.locationtech.geomesa" %% "geomesa-accumulo-jobs" % Version.geomesa,
  "org.locationtech.geomesa" %% "geomesa-accumulo-datastore" % Version.geomesa,
  "org.locationtech.geomesa" %% "geomesa-utils" % Version.geomesa,
  sparkCore % Provided,
  spire,
  scalatest % Test
)

resolvers ++= Seq(
  "locationtech-releases" at "https://repo.locationtech.org/content/repositories/releases/",
  "locationtech-snapshots" at "https://repo.locationtech.org/content/repositories/snapshots/",
  "boundlessgeo" at "http://repo.boundlessgeo.com/main/"
)

fork in Test := false
parallelExecution in Test := false

initialCommands in console :=
  """
  import geotrellis.raster._
  import geotrellis.vector._
  import geotrellis.proj4._
  import geotrellis.spark._
  import geotrellis.spark.util._
  import geotrellis.spark.tiling._
  import geotrellis.spark.io.geomesa._
  """

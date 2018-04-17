import Dependencies._

name := "geotrellis-geotools"

libraryDependencies ++= Seq(
  "org.geotools" % "gt-coverage" % Version.geotools,
  "org.geotools" % "gt-epsg-hsql" % Version.geotools,
  "org.geotools" % "gt-main" % Version.geotools,
  "org.geotools" % "gt-referencing" % Version.geotools,
  jts,
  spire,
  "org.geotools" % "gt-geotiff" % Version.geotools % Test,
  "org.geotools" % "gt-shapefile" % Version.geotools % Test,
  scalatest % Test,
  // This is one finicky dependency. Being explicit in hopes it will stop hurting Travis.
  "javax.media" % "jai_core" % "1.1.3" % Test from "http://download.osgeo.org/webdav/geotools/javax/media/jai_core/1.1.3/jai_core-1.1.3.jar"
)

externalResolvers := Seq(
  "geosolutions" at "http://maven.geo-solutions.it/",
  "osgeo" at "http://download.osgeo.org/webdav/geotools/",
  "boundless" at "https://repo.boundlessgeo.com/main/",
  DefaultMavenRepository,
  Resolver.file("local", file(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns)
)

fork in Test := false
parallelExecution in Test := false

initialCommands in console :=
  """
  import geotrellis.geotools._
  import geotrellis.raster._
  import geotrellis.vector._
  import org.locationtech.jts.{geom => jts}
  import org.geotools.coverage.grid._
  import org.geotools.coverage.grid.io._
  import org.geotools.gce.geotiff._
  """

testOptions in Test += Tests.Setup{ () => Unzip.geoTiffTestFiles() }
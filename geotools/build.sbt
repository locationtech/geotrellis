import Dependencies._

name := "geotrellis-geotools"
libraryDependencies ++= Seq(
  "java3d" % "j3d-core" % "1.3.1",
  "org.geotools" % "gt-main" % Version.geotools,
  "org.geotools" % "gt-coverage" % Version.geotools,
  "org.geotools" % "gt-shapefile" % Version.geotools,
  "org.geotools" % "gt-geotiff" % Version.geotools,
  "org.geotools" % "gt-epsg-hsql" % Version.geotools,
  "javax.media" % "jai_core" % "1.1.3" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_core/1.1.3/jai_core-1.1.3.jar")
resolvers += "Geotools" at "http://download.osgeo.org/webdav/geotools/"
fork in test := false
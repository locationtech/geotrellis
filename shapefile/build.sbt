import Dependencies._

name := "geotrellis-shapefile"
libraryDependencies ++= Seq(
  "org.geotools" % "gt-shapefile" % Version.geotools,
  "javax.media" % "jai_core" % "1.1.3"
)

resolvers += "Geotools" at "http://download.osgeo.org/webdav/geotools/"

fork in Test := false

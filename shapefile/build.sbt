import Dependencies._

name := "geotrellis-shapefile"
libraryDependencies ++= Seq(
  "org.geotools" % "gt-shapefile" % Version.geotools
)

resolvers += "Geotools" at "http://download.osgeo.org/webdav/geotools/"

fork in Test := false

import Dependencies._

name := "geotrellis-shapefile"
libraryDependencies ++= Seq(
  "org.geotools" % "gt-shapefile" % Version.geotools,
  // This is one finicky dependency. Being explicit in hopes it will stop hurting Travis.
  "javax.media" % "jai_core" % "1.1.3" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_core/1.1.3/jai_core-1.1.3.jar"
)

resolvers += "Geotools" at "http://download.osgeo.org/webdav/geotools/"

fork in Test := false

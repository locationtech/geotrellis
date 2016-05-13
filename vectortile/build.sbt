import Dependencies._

name := "geotrellis-vector-tile"

libraryDependencies ++= Seq(
  scalatest % "test",
  "net.sandrogrzicic" %% "scalabuff-runtime" % "1.4.0"
  )

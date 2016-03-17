name := "geotrellis-vector-tile"

resolvers += Resolver.bintrayRepo("azavea", "geotrellis")

libraryDependencies ++= Seq(
  "com.azavea.geotrellis" %% "geotrellis-spark" % Version.geotrellis,
  "org.scalatest" %%  "scalatest" % "2.2.0" % "test",
  "net.sandrogrzicic" %% "scalabuff-runtime" % "1.4.0"
  )
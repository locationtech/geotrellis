import Dependencies._

name := "geotrellis-admin"
fork := true
libraryDependencies ++= Seq(
  scalatest % "test",
  sprayTestkit % "test",
  sprayRouting,
  sprayCan,
  sprayHttpx)

// disable annoying warnings about 2.10.x quasiquotes
conflictWarning in ThisBuild := ConflictWarning.disable
import Dependencies._
import de.heikoseeberger.sbtheader.license.Apache2_0

name := "geotrellis-proj4"

libraryDependencies ++= Seq(
  openCSV,
  parserCombinators,
  scalatest   % "test",
  scalacheck  % "test")

headers := Map(
  "scala" -> Apache2_0("2016", "Azavea"),
  "java" -> Apache2_0("2016", "Martin Davis, Azavea"),
  "conf" -> Apache2_0("2016", "Azavea", "#")
)

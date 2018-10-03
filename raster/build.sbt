import Dependencies._

name := "geotrellis-raster"

libraryDependencies ++= Seq(
  pureconfig,
  jts,
  catsCore,
  spire,
  monocleCore,
  monocleMacro,
  scalatest % Test,
  scalacheck % Test
)


import com.typesafe.tools.mima.core._

mimaPreviousArtifacts := Set(
  "org.locationtech.geotrellis" %% "geotrellis-raster" % Version.previousVersion
)
mimaBinaryIssueFilters ++= Seq(
  ProblemFilters.exclude[ReversedMissingMethodProblem]("geotrellis.raster")
)

sourceGenerators in Compile += (sourceManaged in Compile).map(Boilerplate.genRaster).taskValue

initialCommands in console :="""
import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.vector._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.render._
"""

testOptions in Test += Tests.Setup{ () =>
  val testArchive = "raster/data/geotiff-test-files.zip"
  val testDirPath = "raster/data/geotiff-test-files"
  if(!(new File(testDirPath)).exists) {
    Unzip(testArchive, "raster/data")
  }
}

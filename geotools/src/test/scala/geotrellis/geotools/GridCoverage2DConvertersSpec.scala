package geotrellis.geotools

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.vector._

import org.geotools.coverage.grid._
import org.geotools.coverage.grid.io._
import org.geotools.gce.geotiff._
import org.opengis.parameter.GeneralParameterValue
import org.scalatest._

class GridCoverage2DConvertersSpec extends FunSpec with Matchers {

  case class TestFile(description: String, path: String, isMultiband: Boolean) {
    def gridCoverage2D: GridCoverage2D =
      new GeoTiffReader(new java.io.File(path)).read(null)

    def singlebandRaster: ProjectedRaster[Tile] =
      SinglebandGeoTiff(path).projectedRaster

    def multibandRaster: ProjectedRaster[MultibandTile] =
      MultibandGeoTiff(path).projectedRaster
  }

  val testFiles =
    List(
      TestFile("first", "/Users/rob/proj/gt/geotrellis-benchmark/data/geotiff/SBN_inc_percap.tif", false)
    )

  def assertEqual(actual: ProjectedRaster[Tile], expected: ProjectedRaster[Tile]): Unit = {
    val ProjectedRaster(Raster(actualTile, actualExtent), actualCrs) = actual
    val ProjectedRaster(Raster(expectedTile, expectedExtent), expectedCrs) = expected

    actualExtent should be (expectedExtent)
    actualCrs should be (expectedCrs)

    actualTile.cellType should be (expectedTile.cellType)

    actualTile.cols should be (expectedTile.cols)
    actualTile.rows should be (expectedTile.rows)

    expectedTile.foreachDouble { (col, row, z) =>
      val z2 = actualTile.getDouble(col, row)
      withClue(s"ACTUAL: ${z2}  EXPECTED: $z  COL $col ROW $row") {
        if(isNoData(z)) { isNoData(z2) should be (true) }
        else { z2 should be (z) }
      }
    }
  }

  def assertEqual(actual: ProjectedRaster[MultibandTile], expected: ProjectedRaster[MultibandTile])(implicit d: DummyImplicit): Unit = {
    val ProjectedRaster(Raster(actualTile, actualExtent), actualCrs) = actual
    val ProjectedRaster(Raster(expectedTile, expectedExtent), expectedCrs) = expected

    actualExtent should be (expectedExtent)
    actualCrs should be (expectedCrs)

    actualTile.cellType should be (expectedTile.cellType)

    actualTile.cols should be (expectedTile.cols)
    actualTile.rows should be (expectedTile.rows)

  }

  def assertEqual(pr1: GridCoverage2D, expected: GridCoverage2D): Unit = {

  }

  for(testFile @ TestFile(description, path, isMultiband) <- testFiles) {

    if(isMultiband) {
      describe(s"Conversions (multiband): $description") {
        it("should convert the GridCoverage2D to a ProjectedRaster[MultibandTile]") {
          val (gridCoverage2D, projectedRaster) = (testFile.gridCoverage2D, testFile.multibandRaster)
          assertEqual(gridCoverage2D.toProjectedRaster(), projectedRaster)
        }

        it("should convert a ProjectedRaster to a GridCoverage2D") {
          val (gridCoverage2D, projectedRaster) = (testFile.gridCoverage2D, testFile.multibandRaster)
          assertEqual(projectedRaster.toGridCoverage2D, gridCoverage2D)
        }
      }
    } else {
      val (gridCoverage2D, singlebandProjectedRaster) = (testFile.gridCoverage2D, testFile.singlebandRaster)

      describe(s"Conversions (singleband): $description") {
        it("should convert the GridCoverage2D to a ProjectedRaster[Tile]") {
          val (gridCoverage2D, projectedRaster) = (testFile.gridCoverage2D, testFile.singlebandRaster)
          assertEqual(gridCoverage2D.toProjectedRaster(0), projectedRaster)
        }

        it("should convert the GridCoverage2D to a ProjectedRaster[MultibandTile]") {
          val (gridCoverage2D, projectedRaster) = (testFile.gridCoverage2D, testFile.multibandRaster)
          assertEqual(gridCoverage2D.toProjectedRaster(), projectedRaster)
        }

        it("should convert a ProjectedRaster to a GridCoverage2D") {
          val (gridCoverage2D, projectedRaster) = (testFile.gridCoverage2D, testFile.multibandRaster)
          assertEqual(projectedRaster.toGridCoverage2D, gridCoverage2D)
        }
      }
    }
  }
}

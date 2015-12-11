package geotrellis.spark.testfiles

import com.github.nscala_time.time.Imports._
import geotrellis.spark.tiling._
import geotrellis.raster.{GridBounds, TileLayout, TypeFloat, Tile}
import geotrellis.spark.tiling.{LayoutDefinition, MapKeyTransform}
import org.apache.spark._
import geotrellis.spark._
import geotrellis.proj4._
import org.apache.spark.rdd.RDD
import org.joda.time.DateTime

object TestFiles extends Logging {
  val ZOOM_LEVEL = 8


  val rasterMetaData: RasterMetaData = {
    val cellType = TypeFloat
    val crs = LatLng
    val tileLayout = TileLayout(8, 8, 3, 4)
    val mapTransform = MapKeyTransform(crs, tileLayout.layoutDimensions)
    val gridBounds = GridBounds(1, 1, 6, 7)
    val extent = mapTransform(gridBounds)

    RasterMetaData(cellType, LayoutDefinition(crs.worldExtent, tileLayout), extent, crs)
  }

  def generateSpatial(md: RasterMetaData)(implicit sc: SparkContext): Map[String, RasterRDD[SpatialKey]] = {
    val gridBounds = md.gridBounds
    val tileLayout = md.tileLayout
    // Spatial Tiles
    val spatialTestFiles = List(
      new ConstantSpatialTiles(tileLayout, 1) -> "all-ones",
      new ConstantSpatialTiles(tileLayout, 2) -> "all-twos",
      new ConstantSpatialTiles(tileLayout, 100) -> "all-hundreds",
      new IncreasingSpatialTiles(tileLayout, gridBounds) -> "increasing",
      new DecreasingSpatialTiles(tileLayout, gridBounds) -> "decreasing",
      new EveryOtherSpatialTiles(tileLayout, gridBounds, Double.NaN, 0.0) -> "every-other-undefined",
      new EveryOtherSpatialTiles(tileLayout, gridBounds, 0.99, 1.01) -> "every-other-0.99-else-1.01",
      new EveryOtherSpatialTiles(tileLayout, gridBounds, -1, 1) -> "every-other-1-else-1",
      new ModSpatialTiles(tileLayout, gridBounds, 10000) -> "mod-10000"
    )

    for((tfv, name) <- spatialTestFiles) yield {
      val tiles =
        for(
          row <- gridBounds.rowMin to gridBounds.rowMax;
          col <- gridBounds.colMin to gridBounds.colMax
        ) yield {
          val key = SpatialKey(col, row)
          val tile = tfv(key)
          (key, tile)
        }


      val rdd =
        asRasterRDD(md) {
          sc.parallelize(tiles)
        }


      (name, rdd)
    }
  }.toMap

  def generateSpaceTime(md: RasterMetaData)(implicit sc: SparkContext): Map[String, RasterRDD[SpaceTimeKey]] = {
    val gridBounds = md.gridBounds
    val tileLayout = md.tileLayout

    val times =
      (0 to 4).map(i => new DateTime(2010 + i, 1, 1, 0, 0, 0, DateTimeZone.UTC)).toArray

    val spaceTimeTestFiles = List(
      new ConstantSpaceTimeTestTiles(tileLayout, 1) -> "spacetime-all-ones",
      new ConstantSpaceTimeTestTiles(tileLayout, 2) -> "spacetime-all-twos",
      new ConstantSpaceTimeTestTiles(tileLayout, 100) -> "spacetime-all-hundreds",
      new CoordinateSpaceTimeTestTiles(tileLayout) -> "spacetime-coordinates"
    )

    for((tfv, name) <- spaceTimeTestFiles) yield {
      val tiles =
        for(
          row <- gridBounds.rowMin to gridBounds.rowMax;
          col <- gridBounds.colMin to gridBounds.colMax;
          (time, timeIndex) <- times.zipWithIndex
        ) yield {
          val key = SpaceTimeKey(col, row, time)
          val tile = tfv(key, timeIndex)
          (key, tile)

        }

      val rdd =
        asRasterRDD(md) {
          sc.parallelize(tiles)
        }

      (name, rdd)
    }
  }.toMap
}

trait TestFiles { self: TestSparkContext =>
  lazy val spatialTestFile = TestFiles.generateSpatial(TestFiles.rasterMetaData)

  lazy val spaceTimeTestFile = TestFiles.generateSpaceTime(TestFiles.rasterMetaData)

  def AllOnesTestFile =
    spatialTestFile("all-ones")

  def AllTwosTestFile =
    spatialTestFile("all-twos")

  def AllHundredsTestFile =
    spatialTestFile("all-hundreds")

  def IncreasingTestFile =
    spatialTestFile("increasing")

  def DecreasingTestFile =
    spatialTestFile("decreasing")

  def EveryOtherUndefinedTestFile =
    spatialTestFile("every-other-undefined")

  def EveryOther0Point99Else1Point01TestFile =
    spatialTestFile("every-other-0.99-else-1.01")

  def EveryOther1ElseMinus1TestFile =
    spatialTestFile("every-other-1-else-1")

  def Mod10000TestFile =
    spatialTestFile("mod-10000")

  def AllOnesSpaceTime =
    spaceTimeTestFile("spacetime-all-ones")

  def AllTwosSpaceTime =
    spaceTimeTestFile("spacetime-all-twos")

  def AllHundredsSpaceTime =
    spaceTimeTestFile("spacetime-all-hundreds")

  /** Coordinates are CCC,RRR.TTT where C = column, R = row, T = time (year in 2010 + T).
    * So 34,025.004 would represent col 34, row 25, year 2014
    */
  def CoordinateSpaceTime =
    spaceTimeTestFile("spacetime-coordinates")
}

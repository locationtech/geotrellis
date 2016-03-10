package geotrellis.spark.testfiles

import com.github.nscala_time.time.Imports._
import geotrellis.spark.tiling._
import geotrellis.raster.{GridBounds, TileLayout, FloatConstantNoDataCellType}
import geotrellis.spark.tiling.{LayoutDefinition, MapKeyTransform}
import org.apache.spark._
import geotrellis.spark._
import geotrellis.proj4._
import org.joda.time.DateTime

object TestFiles extends Logging {
  val ZOOM_LEVEL = 8
  val partitionCount = 4

  def generateSpatial(layerName: String)(implicit sc: SparkContext): RasterRDD[GridKey] = {
    val md = {
      val cellType = FloatConstantNoDataCellType
      val crs = LatLng
      val tileLayout = TileLayout(8, 8, 3, 4)
      val mapTransform = MapKeyTransform(crs, tileLayout.layoutDimensions)
      val gridBounds = GridBounds(1, 1, 6, 7)
      val extent = mapTransform(gridBounds)
      val keyBounds = KeyBounds(GridKey(1,1), GridKey(6,7))
      RasterMetadata(cellType, LayoutDefinition(crs.worldExtent, tileLayout), extent, crs, keyBounds)
    }

    val gridBounds = md.gridBounds
    val tileLayout = md.tileLayout

    val gridKeyTestFile = layerName match {
      case "all-ones" => new ConstantGridKeyTiles (tileLayout, 1)
      case "all-twos" => new ConstantGridKeyTiles (tileLayout, 2)
      case "all-hundreds" => new ConstantGridKeyTiles (tileLayout, 100)
      case "increasing" => new IncreasingGridKeyTiles (tileLayout, gridBounds)
      case "decreasing" => new DecreasingGridKeyTiles (tileLayout, gridBounds)
      case "every-other-undefined" => new EveryOtherGridKeyTiles (tileLayout, gridBounds, Double.NaN, 0.0)
      case "every-other-0.99-else-1.01" => new EveryOtherGridKeyTiles (tileLayout, gridBounds, 0.99, 1.01)
      case "every-other-1-else-1" => new EveryOtherGridKeyTiles (tileLayout, gridBounds, - 1, 1)
      case "mod-10000" => new ModGridKeyTiles (tileLayout, gridBounds, 10000)
    }

    val tiles =
      for(
        row <- gridBounds.rowMin to gridBounds.rowMax;
        col <- gridBounds.colMin to gridBounds.colMax
      ) yield {
        val key = GridKey(col, row)
        val tile = gridKeyTestFile(key)
        (key, tile)
      }

    new ContextRDD(sc.parallelize(tiles, partitionCount), md)
  }

  def generateSpaceTime(layerName: String)(implicit sc: SparkContext): RasterRDD[GridTimeKey] = {
    val times =
      (0 to 4).map(i => new DateTime(2010 + i, 1, 1, 0, 0, 0, DateTimeZone.UTC)).toArray


    val md = {
      val cellType = FloatConstantNoDataCellType
      val crs = LatLng
      val tileLayout = TileLayout(8, 8, 3, 4)
      val mapTransform = MapKeyTransform(crs, tileLayout.layoutDimensions)
      val gridBounds = GridBounds(1, 1, 6, 7)
      val extent = mapTransform(gridBounds)
      val keyBounds = KeyBounds(GridTimeKey(1,1,times.min), GridTimeKey(6,7, times.max))
      RasterMetadata(cellType, LayoutDefinition(crs.worldExtent, tileLayout), extent, crs, keyBounds)
    }

    val gridBounds = md.gridBounds
    val tileLayout = md.tileLayout

    val gridTimeKeyTestTiles = layerName match {
      case "gridtimekey-all-ones" => new ConstantGridTimeKeyTestTiles(tileLayout, 1)
      case "gridtimekey-all-twos" => new ConstantGridTimeKeyTestTiles(tileLayout, 2)
      case "gridtimekey-all-hundreds" => new ConstantGridTimeKeyTestTiles(tileLayout, 100)
      case "gridtimekey-coordinates" => new CoordinateGridTimeKeyTestTiles(tileLayout)
    }

    val tiles =
      for(
        row <- gridBounds.rowMin to gridBounds.rowMax;
        col <- gridBounds.colMin to gridBounds.colMax;
        (time, timeIndex) <- times.zipWithIndex
      ) yield {
        val key = GridTimeKey(col, row, time)
        val tile = gridTimeKeyTestTiles(key, timeIndex)
        (key, tile)

      }

    new ContextRDD(sc.parallelize(tiles, partitionCount), md)
  }
}

trait TestFiles { self: TestEnvironment =>
  def gridKeyTestFile(name: String) = TestFiles.generateSpatial(name)

  def gridTimeKeyTestFile(name: String) = TestFiles.generateSpaceTime(name)

  lazy val AllOnesTestFile =
    gridKeyTestFile("all-ones")

  lazy val AllTwosTestFile =
    gridKeyTestFile("all-twos")

  lazy val AllHundredsTestFile =
    gridKeyTestFile("all-hundreds")

  lazy val IncreasingTestFile =
    gridKeyTestFile("increasing")

  lazy val DecreasingTestFile =
    gridKeyTestFile("decreasing")

  lazy val EveryOtherUndefinedTestFile =
    gridKeyTestFile("every-other-undefined")

  lazy val EveryOther0Point99Else1Point01TestFile =
    gridKeyTestFile("every-other-0.99-else-1.01")

  lazy val EveryOther1ElseMinus1TestFile =
    gridKeyTestFile("every-other-1-else-1")

  lazy val Mod10000TestFile =
    gridKeyTestFile("mod-10000")

  lazy val AllOnesSpaceTime =
    gridTimeKeyTestFile("gridtimekey-all-ones")

  lazy val AllTwosSpaceTime =
    gridTimeKeyTestFile("gridtimekey-all-twos")

  lazy val AllHundredsSpaceTime =
    gridTimeKeyTestFile("gridtimekey-all-hundreds")

  /** Coordinates are CCC,RRR.TTT where C = column, R = row, T = time (year in 2010 + T).
    * So 34,025,004 would represent col 34, row 25, year 2014
    */
  lazy val CoordinateSpaceTime =
    gridTimeKeyTestFile("gridtimekey-coordinates")
}
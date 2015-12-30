package geotrellis.spark.op.local.temporal

import geotrellis.raster._
import geotrellis.raster.op.local._

import geotrellis.spark._

import com.github.nscala_time.time.Imports._

import spire.syntax.cfor._

import org.scalatest.FunSpec

class LocalTemporalSpec extends FunSpec with TestEnvironment
    with RasterRDDBuilders
    with RasterRDDMatchers
    with TestSparkContext {

  describe("Local Temporal Operations") {

    def createIncreasingTemporalRasterRDD(dates: Seq[DateTime], f: DateTime => Int = { dt => dt.getYear }) = {
      val tileLayout = TileLayout(3, 3, 3, 3)

      val datesZippedWithIndex = dates.zipWithIndex

      val rasterRDDs = for (dateTime <- dates) yield {
        val idx = f(dateTime)
        val rasterRDD = createRasterRDD(
          sc,
          ArrayTile((idx to (idx + 80)).toArray, 9, 9),
          tileLayout
        )

        val metaData = rasterRDD.metaData
        val rdd = rasterRDD.map { case(spatialKey, tile) =>
          (SpaceTimeKey(spatialKey, TemporalKey(dateTime)), tile)
        }

        new ContextRDD(rdd, metaData)
      }

      val metaData = rasterRDDs.head.metaData
      val combinedRDDs = rasterRDDs.map(_.rdd).reduce(_ ++ _)

      new ContextRDD(combinedRDDs, metaData)
    }

    def groupRasterRDDToRastersByTemporalKey(rasterRDD: RasterRDD[SpaceTimeKey]): Map[DateTime, Tile] = {
      val metaData = rasterRDD.metaData
      val gridBounds = metaData.mapTransform(metaData.extent)
      val tileLayout = 
        TileLayout(gridBounds.width, gridBounds.height, metaData.tileLayout.tileCols, metaData.tileLayout.tileRows)

      rasterRDD
        .groupBy { case (key, tile) =>
          val SpaceTimeKey(_, _, time) = key
          time
         }
        .collect
        .sortWith { (x, y) => x._1.isBefore(y._1) }
        .map { case (time, iter) =>
          val tiles = 
            iter
              .toSeq
              .sorted(Ordering.by[(SpaceTimeKey, Tile), (Int, Int)] { case (key, tile) =>
                val SpatialKey(col, row) = key.spatialComponent
                (row, col)
              })
              .map(_._2)

          (time, CompositeTile(tiles.toSeq, tileLayout))
         }
        .toMap
    }

    it("should work with min for a 9 year period where the window is 3 years.") {
      val dates = (1 until 10).map(i => new DateTime(i, 1, 1, 0, 0, 0, DateTimeZone.UTC))
      val rasterRDD = createIncreasingTemporalRasterRDD(dates)

      val start = new DateTime(1, 1, 1, 0, 0, 0, DateTimeZone.UTC)
      val end = new DateTime(9, 1, 1, 0, 0, 0, DateTimeZone.UTC)
      val res = rasterRDD.minimum.per (3) ("years") from (start) to (end)

      val rasters = groupRasterRDDToRastersByTemporalKey(res)

      rasters.size should be (3)

      // Years 1, 4 and 7 have the mins.
      rasters.zip(List(1, 4, 7)).foreach { case((date, tile), idx) =>
        date.getYear should be (idx)
        val tileArray = tile.toArray
        val correct = (idx to (idx + 80)).toArray
        tileArray should be(correct)
      }
    }

    it("should work with max for a 12 month period where the window is 5 months.") {
      val dates = (1 to 12).map(i => new DateTime(1, i, 1, 0, 0, 0, DateTimeZone.UTC))
      val rasterRDD = createIncreasingTemporalRasterRDD(dates, { date => date.getMonthOfYear })

      val start = new DateTime(1, 1, 1, 0, 0, 0, DateTimeZone.UTC)
      val end = new DateTime(2, 1, 1, 0, 0, 0, DateTimeZone.UTC)
      val periodStep = 5
      val res = rasterRDD.maximum.per (periodStep) ("months") from (start) to (end)

      val rasters = groupRasterRDDToRastersByTemporalKey(res)

      rasters.size should be (3)

      // Months 5, 10 and 12 have the maxs.

      rasters.zip(List(5, 10, 12)).foreach { case((date, tile), idx) =>
        val tileArray = tile.toArray
        val correct = (idx to (idx + 80)).toArray
        tileArray should be(correct)
      }
    }

    it("should work with mean for a 25 days period where the window is 7 days.") {
      val dates = (1 to 25).map(i => new DateTime(1, 1, i, 0, 0, 0, DateTimeZone.UTC))
      val rasterRDD = createIncreasingTemporalRasterRDD(dates, { date => date.getDayOfMonth })

      val start = new DateTime(1, 1, 1, 0, 0, 0, DateTimeZone.UTC)
      val end = new DateTime(2, 1, 1, 0, 0, 0, DateTimeZone.UTC)
      val windowSize = 7
      val res = rasterRDD.average.per (windowSize) ("days") from (start) to (end)

      val rasters = groupRasterRDDToRastersByTemporalKey(res)

      rasters.size should be (4)

      val firsts = (1 to 25) grouped 7 map { seq => seq.sum / seq.size }
      for( ((time, tile), expected) <- rasters.zip(firsts.toSeq)) {
        tile.getDouble(0, 0) should be (expected)
      }
    }

    it("should work with variance for a 12 hours period where the window is 3 hours.") {
      val dates = (0 to 11).map(i => new DateTime(1, 1, 1, i, 0, 0, DateTimeZone.UTC))
      val rasterRDD = createIncreasingTemporalRasterRDD(dates, { date => date.getHourOfDay })

      val start = new DateTime(1, 1, 1, 0, 0, 0, DateTimeZone.UTC)
      val end = new DateTime(2, 1, 1, 0, 0, 0, DateTimeZone.UTC)
      val windowSize = 3
      val res = rasterRDD.variance.per (windowSize) ("hours") from (start) to (end)

      val rasters = groupRasterRDDToRastersByTemporalKey(res)

      rasters.size should be (4)

      val inputRasters = groupRasterRDDToRastersByTemporalKey(rasterRDD).values.toList
      val expectedTiles = 
        for(indicies <- (0 to 11).grouped(3)) yield {
          Variance(indicies.map { inputRasters(_) })
        }
      expectedTiles.size should be (4)

      rasters.zip(expectedTiles.toSeq) foreach { case ((_, tile), expected) =>
        tile.toArrayDouble should be (expected.toArrayDouble)
      }
    }
  }
}

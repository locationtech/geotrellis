package geotrellis.spark.op.local.temporal

import geotrellis.raster._

import geotrellis.spark._

import org.joda.time.{DateTime, DateTimeZone}

import spire.syntax.cfor._

import org.scalatest.FunSpec

class LocalTemporalSpec extends FunSpec with TestEnvironment
    with RasterRDDBuilders
    with RasterRDDMatchers
    with OnlyIfCanRunSpark {

  ifCanRunSpark {

    describe("Local Temporal Operations") {

      def createIncreasingTemporalRasterRDD(dates: Seq[DateTime]) = {
        val tileLayout = TileLayout(3, 3, 3, 3)

        val datesZippedWithIndex = dates.zipWithIndex

        val rasterRDDs = for ((dateTime, idx) <- dates.zipWithIndex) yield {
          val rasterRDD = createRasterRDD(
            sc,
            ArrayTile((idx to (idx + 80)).toArray, 9, 9),
            tileLayout
          )

          val metaData = rasterRDD.metaData
          val rdd = rasterRDD.map { case(spatialKey, tile) =>
            (SpaceTimeKey(spatialKey, TemporalKey(dateTime)), tile)
          }

          new RasterRDD(rdd, metaData)
        }

        val metaData = rasterRDDs.head.metaData
        val combinedRDDs = rasterRDDs.map(_.rdd).reduce(_ ++ _)

        new RasterRDD(combinedRDDs, metaData)
      }

      def groupRasterRDDToRastersByTemporalKey(rasterRDD: RasterRDD[SpaceTimeKey]) = {
        val metaData = rasterRDD.metaData

        rasterRDD.groupBy { case (key, tile) =>
          val SpaceTimeKey(_, TemporalKey(time)) = key
          time
        }
          .collect
          .sortWith((x, y) => x._1.isBefore(y._1))
          .map(x => {
            val rdd = sc.parallelize(x._2.toSeq.map {
              case (key, tile) =>
                val SpaceTimeKey(newKey, _) = key
                (newKey, tile)
            })

            new RasterRDD(rdd, metaData).stitch
          })
      }

      it("should work with min for a 9 year period where the window is 3 years.") {
        val dates = (1 to 10).map(i => new DateTime(i, 1, 1, 0, 0, 0, DateTimeZone.UTC))
        val rasterRDD = createIncreasingTemporalRasterRDD(dates)

        val start = new DateTime(1, 1, 1, 0, 0, 0, DateTimeZone.UTC)
        val end = new DateTime(9, 1, 1, 0, 0, 0, DateTimeZone.UTC)
        val res = rasterRDD.minimum.per (3) ("years") from (start) to (end)

        val rasters = groupRasterRDDToRastersByTemporalKey(res)

        rasters.size should be (9)

        rasters.zipWithIndex.foreach { case(tile, idx) =>
          val tileArray = tile.toArray
          val correct = (idx to (idx + 80)).toArray
          tileArray should be(correct)
        }
      }

      it("should work with max for a 10 month period where the window is 5 months.") {
        val dates = (1 to 10).map(i => new DateTime(1, i, 1, 0, 0, 0, DateTimeZone.UTC))
        val rasterRDD = createIncreasingTemporalRasterRDD(dates)

        val start = new DateTime(1, 1, 1, 0, 0, 0, DateTimeZone.UTC)
        val end = new DateTime(2, 1, 1, 0, 0, 0, DateTimeZone.UTC)
        val periodStep = 5
        val res = rasterRDD.maximum.per (periodStep) ("months") from (start) to (end)

        val rasters = groupRasterRDDToRastersByTemporalKey(res)

        rasters.size should be (10)

        rasters.zipWithIndex.foreach { case(tile, idx) =>
          val tileArray = tile.toArray
          val start = math.min(rasters.size - 1, idx + periodStep - 1)
          val end = math.min(80 + rasters.size - 1, idx + 79 + periodStep)
          val correct = (start to end).toArray
          tileArray should be(correct)
        }
      }

      it("should work with mean for a 25 days period where the window is 7 days.") {
        val dates = (1 to 25).map(i => new DateTime(1, 1, i, 0, 0, 0, DateTimeZone.UTC))
        val rasterRDD = createIncreasingTemporalRasterRDD(dates)

        val start = new DateTime(1, 1, 1, 0, 0, 0, DateTimeZone.UTC)
        val end = new DateTime(2, 1, 1, 0, 0, 0, DateTimeZone.UTC)
        val windowSize = 7
        val res = rasterRDD.average.per (windowSize) ("days") from (start) to (end)

        val rasters = groupRasterRDDToRastersByTemporalKey(res)

        rasters.size should be (25)

        rasters.zipWithIndex.foreach { case(tile, idx) =>
          val tileArray = tile.toArray
          val base = (idx to (idx + 80)).toSeq

          val adjustedWindowSize = math.min(windowSize, 25 - idx)
          val matrix = for (i <- 0 until adjustedWindowSize) yield (base.map(_ + i))
          val correct = Array.ofDim[Int](81)

          cfor(0)(_ < 81, _ + 1) { i =>
            var acc = 0
            cfor(0)(_ < adjustedWindowSize, _ + 1) { j =>
              acc += matrix(j)(i)
            }

            correct(i) = acc / adjustedWindowSize
          }

          tileArray should be(correct)
        }
      }
    }

  }

}

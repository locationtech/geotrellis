package geotrellis.spark.op.local.temporal

import geotrellis.raster._

import geotrellis.spark._

import org.joda.time.{DateTime, DateTimeZone}

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

      it("should work for a ten year period where the window is 3 years.") {
        val dates = (1 to 10).map(i => new DateTime(i, 1, 1, 0, 0, 0, DateTimeZone.UTC))
        val rasterRDD = createIncreasingTemporalRasterRDD(dates)

        val start = new DateTime(1, 1, 1, 0, 0, 0, DateTimeZone.UTC)
        val res = rasterRDD.minimum.per (3) ("years") from (start)

        val metaData = res.metaData

        val rasters = res.groupBy { case (key, tile) =>
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


        rasters.zipWithIndex.foreach { case(tile, idx) =>
          val tileArray = tile.toArray
          val correct = (idx to (idx + 80)).toArray
          tileArray should be(correct)
        }
      }
    }

  }

}

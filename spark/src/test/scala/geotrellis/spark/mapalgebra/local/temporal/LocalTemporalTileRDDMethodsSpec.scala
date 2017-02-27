/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.mapalgebra.local.temporal

import geotrellis.raster._
import geotrellis.raster.mapalgebra.local._
import geotrellis.spark._
import geotrellis.util._
import geotrellis.spark.testkit._

import spire.syntax.cfor._
import jp.ne.opt.chronoscala.Imports._
import java.time.{ZoneOffset, ZonedDateTime}

import org.scalatest.FunSpec

class LocalTemporalSpec extends FunSpec with TestEnvironment {

  describe("Local Temporal Operations") {

    def createIncreasingTemporalTileLayerRDD(dates: Seq[ZonedDateTime], f: ZonedDateTime => Int = { dt => dt.getYear }) = {
      val tileLayout = TileLayout(3, 3, 3, 3)

      val datesZippedWithIndex = dates.zipWithIndex

      val rasterRDDs = for (dateTime <- dates) yield {
        val idx = f(dateTime)
        val rasterRDD = createTileLayerRDD(
          sc,
          ArrayTile((idx to (idx + 80)).toArray, 9, 9),
          tileLayout
        )

        val kb = rasterRDD.metadata.bounds.get
        val metadata = rasterRDD.metadata.copy(bounds =
          KeyBounds(SpaceTimeKey(kb.minKey, TemporalKey(dates.min)),
            SpaceTimeKey(kb.maxKey, TemporalKey(dates.max)))
        )
        val rdd = rasterRDD.map { case (spatialKey, tile) =>
          (SpaceTimeKey(spatialKey, TemporalKey(dateTime)), tile)
        }

        new ContextRDD(rdd, metadata)
      }

      val metadata = rasterRDDs.head.metadata
      val combinedRDDs = rasterRDDs.map(_.rdd).reduce(_ ++ _)

      new ContextRDD(combinedRDDs, metadata)
    }

    def groupTileLayerRDDToRastersByTemporalKey(rasterRDD: TileLayerRDD[SpaceTimeKey]): Map[ZonedDateTime, Tile] = {
      val metadata = rasterRDD.metadata
      val gridBounds = metadata.mapTransform(metadata.extent)
      val tileLayout =
        TileLayout(gridBounds.width, gridBounds.height, metadata.tileLayout.tileCols, metadata.tileLayout.tileRows)

      rasterRDD
        .groupBy { case (key, tile) =>
          key.time
        }
        .collect
        .sortWith { (x, y) => x._1.isBefore(y._1) }
        .map { case (time, iter) =>
          val tiles =
            iter
              .toSeq
              .sorted(Ordering.by[(SpaceTimeKey, Tile), (Int, Int)] { case (key, tile) =>
                val SpatialKey(col, row) = key.getComponent[SpatialKey]
                (row, col)
              })
              .map(_._2)

          (time, CompositeTile(tiles.toSeq, tileLayout))
        }
        .toMap
    }

    it("should work with min for a 9 year period where the window is 3 years.") {
      val dates = (1 until 10).map(i => ZonedDateTime.of(i, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
      val rasterRDD: TileLayerRDD[SpaceTimeKey] = createIncreasingTemporalTileLayerRDD(dates)

      val start = ZonedDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
      val end = ZonedDateTime.of(9, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
      val res = rasterRDD.withContext(_.minimum.per(3)("years") from (start) to (end))

      val rasters = groupTileLayerRDDToRastersByTemporalKey(res)

      rasters.size should be(3)

      // Years 1, 4 and 7 have the mins.
      rasters.zip(List(1, 4, 7)).foreach { case ((date, tile), idx) =>
        date.getYear should be(idx)
        val tileArray = tile.toArray
        val correct = (idx to (idx + 80)).toArray
        tileArray should be(correct)
      }
    }

    it("should work with max for a 12 month period where the window is 5 months.") {
      val dates = (1 to 12).map(i => ZonedDateTime.of(1, i, 1, 0, 0, 0, 0, ZoneOffset.UTC))
      val rasterRDD = createIncreasingTemporalTileLayerRDD(dates, { date => date.getMonthValue })

      val start = ZonedDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
      val end = ZonedDateTime.of(2, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
      val periodStep = 5
      val res = rasterRDD.withContext(_.maximum.per(periodStep)("months") from (start) to (end))

      val rasters = groupTileLayerRDDToRastersByTemporalKey(res)

      rasters.size should be(3)

      // Months 5, 10 and 12 have the maxs.

      rasters.zip(List(5, 10, 12)).foreach { case ((date, tile), idx) =>
        val tileArray = tile.toArray
        val correct = (idx to (idx + 80)).toArray
        tileArray should be(correct)
      }
    }

    it("should work with mean for a 25 days period where the window is 7 days.") {
      val dates = (1 to 25).map(i => ZonedDateTime.of(1, 1, i, 0, 0, 0, 0, ZoneOffset.UTC))
      val rasterRDD = createIncreasingTemporalTileLayerRDD(dates, { date => date.getDayOfMonth })

      val start = ZonedDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
      val end = ZonedDateTime.of(2, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
      val windowSize = 7
      val res = rasterRDD.withContext(_.average.per(windowSize)("days") from (start) to (end))

      val rasters = groupTileLayerRDDToRastersByTemporalKey(res)

      rasters.size should be(4)

      val firsts = (1 to 25) grouped 7 map { seq => seq.sum / seq.size }
      for (((time, tile), expected) <- rasters.zip(firsts.toSeq)) {
        tile.getDouble(0, 0) should be(expected)
      }
    }

    it("should work with variance for a 12 hours period where the window is 3 hours.") {
      val dates = (0 to 11).map(i => ZonedDateTime.of(1, 1, 1, i, 0, 0, 0, ZoneOffset.UTC))
      val rasterRDD = createIncreasingTemporalTileLayerRDD(dates, { date => date.getHour })

      val start = ZonedDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
      val end = ZonedDateTime.of(2, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
      val windowSize = 3
      val res = rasterRDD.withContext(_.variance.per(windowSize)("hours") from (start) to (end))

      val rasters = groupTileLayerRDDToRastersByTemporalKey(res)

      rasters.size should be(4)

      val inputRasters = groupTileLayerRDDToRastersByTemporalKey(rasterRDD).values.toList
      val expectedTiles =
        for (indicies <- (0 to 11).grouped(3)) yield {
          Variance(indicies.map {
            inputRasters(_)
          })
        }
      expectedTiles.size should be(4)

      rasters.zip(expectedTiles.toSeq) foreach { case ((_, tile), expected) =>
        tile.toArrayDouble should be(expected.toArrayDouble)
      }
    }
  }
}

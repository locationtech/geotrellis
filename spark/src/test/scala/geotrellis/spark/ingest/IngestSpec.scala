/*
 * Copyright (c) 2014 DigitalGlobe.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.ingest

import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.raster.mosaic._
import geotrellis.raster.io.arg.ArgReader
import geotrellis.vector.Extent
import geotrellis.proj4._
import geotrellis.testkit._

import geotrellis.spark._
import geotrellis.spark.rdd._
import geotrellis.spark.tiling._

import org.apache.spark.rdd._

import org.scalatest._

class IngestSpec extends FunSpec 
                    with TestEngine
                    with TestEnvironment 
                    with SharedSparkContext 
                    with OnlyIfCanRunSpark {
  TestEngine.setCatalogPath("../raster-test/data/catalog.json")
  val ingest = new Ingest(sc)

  describe("reprojection") {
    // TODO: Failing.
    ignore("should reproject from WebMercator to LatLng") {
      val srcCRS = WebMercator
//      val destCRS = LatLng
      val destCRS = WebMercator

      val reproject = ingest.reproject(srcCRS, destCRS)

      val totalCols = 1000
      val totalRows = 1500
      val tileCols = 2
      val tileRows = 1
      val pixelCols = totalCols / tileCols
      val pixelRows = totalRows / tileRows

      val (tile, extent) = {
        val Raster(t, e) = ArgReader.read("../raster-test/data/sbn/SBN_inc_percap.json")
        (t.resample(e, totalCols, totalRows), e)
      }

      val (expectedTile, expectedExtent) = tile.reproject(extent, srcCRS, destCRS)

      val tileLayout = TileLayout(tileCols, tileRows, pixelCols, pixelRows)

      val rdd: RDD[(Extent, Tile)] = {
        val tileExtents = TileExtents(extent, tileLayout)
        val tiles = CompositeTile.wrap(tile, tileLayout).tiles
        sc.parallelize(tiles.zipWithIndex.map { case (tile, i) => (tileExtents(i), tile) })
      }

//      val reprojected = reproject(rdd).collect
      val reprojected = rdd.collect

      val actualTile = ArrayTile.empty(expectedTile.cellType, expectedTile.cols, expectedTile.rows)
      for((rExtent, rTile) <- reprojected) {
        actualTile.merge(expectedExtent, rExtent, rTile)
      }

      var count = 0
      val sCol = scala.collection.mutable.Set[Int]()
      val sRow = scala.collection.mutable.Set[Int]()
      for(row <- 0 until actualTile.rows) {
        for(col <- 0 until actualTile.cols) {
          val actual = actualTile.get(col, row)
          val expected = expectedTile.get(col, row)
          if(actual != expected) {
            count += 1
            sCol += col
            sRow += row
          }
        }
      }

      println(sCol.toSeq.sorted)
      println(sRow.toSeq.sorted)
      count should be (0)
//      assertEqual(actualTile, expectedTile)

      // val distinctColsRows = reprojected.map(_._2).map(t => (t.cols, t.rows)).distinct
      // println(distinctColsRows.toSeq)
      // distinctColsRows.size should be (1)
      // val (newCols, newRows) = distinctColsRows.head
      // val newTileLayout = TileLayout(tileCols, tileRows, newCols, newRows)
      
      // val actualTile = CompositeTile(reprojected.map(_._2), newTileLayout).toArrayTile
      // actualTile.rows should be (expectedTile.rows += 1)
      // actualTile.cols should be (expectedTile.cols)

      // val actualExtent = reprojected.map(_._1).reduce(_.combine(_))

      // actualExtent should be (expectedExtent)

    }
  }
}

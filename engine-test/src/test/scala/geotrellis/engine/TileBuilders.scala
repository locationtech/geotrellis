/*
 * Copyright (c) 2014 Azavea.
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

package geotrellis.engine

import geotrellis.vector.Extent
import geotrellis.raster._

trait TileBuilders {
  val nd = NODATA
  val NaN = Double.NaN



  def createRasterSource(arr: Array[Int], pixelCols: Int, pixelRows: Int): RasterSource =
    createRasterSource(arr, 1, 1, pixelCols, pixelRows)

  def createRasterSource(arr: Array[Int], tileCols: Int, tileRows: Int, pixelCols: Int, pixelRows: Int): RasterSource =
    createRasterSource(arr, tileCols, tileRows, pixelCols, pixelRows, 10.0, 1.0)

  def createRasterSource(arr: Array[Int], tileCols: Int, tileRows: Int, pixelCols: Int, pixelRows: Int, cellwidth: Double, cellheight: Double): RasterSource = {
    if(tileCols * pixelCols * tileRows * pixelRows != arr.length) {
      sys.error("Tile and pixel col rows do not match array length")
    }
    val tiles =
      (for(j <- 0 until tileRows) yield {
        (for(i <- 0 until tileCols) yield { Array.ofDim[Int](pixelCols * pixelRows) }).toArray
      }).toArray

    for(tR <- 0 until tileRows) {
      for(pR <- 0 until pixelRows) {
        for(tC <- 0 until tileCols) {
          for(pC <- 0 until pixelCols) {
            val col = tC * pixelCols + pC
            val row = tR * pixelRows + pR
            val v = arr(row * tileCols * pixelCols + col)
            tiles(tR)(tC)(pR * pixelCols + pC) = v
          }
        }
      }
    }

    val rasters =
      (for(r <- 0 until tileRows;
        c <- 0 until tileCols) yield {
        ArrayTile(tiles(r)(c), pixelCols, pixelRows)
      }).toSeq

    val xmin = 0
    val xmax = tileCols * pixelCols * cellwidth
    val ymin = -1 * tileRows * pixelRows * cellheight
    val ymax = 0

    val ops = rasters.map(Literal(_))
    val re = RasterExtent(Extent(xmin, ymin, xmax, ymax), tileCols * pixelCols, tileRows * pixelRows)
    val tileLayout = TileLayout(tileCols, tileRows, pixelCols, pixelRows)

    RasterSource(RasterDefinition(LayerId("test"), re, tileLayout, TypeInt), ops)
  }

  def createRasterSource(arr: Array[Double], tileCols: Int, tileRows: Int, pixelCols: Int, pixelRows: Int): RasterSource =
    createRasterSource(arr, tileCols, tileRows, pixelCols, pixelRows, 10.0, 1.0)

  def createRasterSource(arr: Array[Double], tileCols: Int, tileRows: Int, pixelCols: Int, pixelRows: Int, cellwidth: Double, cellheight: Double): RasterSource = {
    if(tileCols * pixelCols * tileRows * pixelRows != arr.length) {
      sys.error("Tile and pixel col rows do not match array length")
    }
    val tiles =
      (for(j <- 0 until tileRows) yield {
        (for(i <- 0 until tileCols) yield { Array.ofDim[Double](pixelCols * pixelRows) }).toArray
      }).toArray

    for(tR <- 0 until tileRows) {
      for(pR <- 0 until pixelRows) {
        for(tC <- 0 until tileCols) {
          for(pC <- 0 until pixelCols) {
            val col = tC * pixelCols + pC
            val row = tR * pixelRows + pR
            val v = arr(row * tileCols * pixelCols + col)
            tiles(tR)(tC)(pR * pixelCols + pC) = v
          }
        }
      }
    }

    val rasters =
      (for(r <- 0 until tileRows;
        c <- 0 until tileCols) yield {
        val xmin = c * pixelCols * cellwidth
        val xmax = xmin + (pixelCols * cellwidth)
        val ymin = r * (-pixelRows) * cellheight
        val ymax = ymin + (pixelRows * cellheight)
        ArrayTile(tiles(r)(c), pixelCols, pixelRows)
      }).toSeq

    val xmin = 0
    val xmax = tileCols * pixelCols * cellwidth
    val ymin = -1 * tileRows * pixelRows * cellheight
    val ymax = 0

    val ops = rasters.map(Literal(_))
    val re = RasterExtent(Extent(xmin, ymin, xmax, ymax), tileCols * pixelCols, tileRows * pixelRows)

    val tileLayout = TileLayout(tileCols, tileRows, pixelCols, pixelRows)

    RasterSource(RasterDefinition(LayerId("test"), re, tileLayout, TypeDouble), ops)
  }
}

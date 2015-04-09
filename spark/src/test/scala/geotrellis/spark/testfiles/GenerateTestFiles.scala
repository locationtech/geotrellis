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

package geotrellis.spark.testfiles

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.tiling._
import geotrellis.spark.utils._
import geotrellis.raster._
import geotrellis.vector._
import geotrellis.proj4._

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark._

import com.github.nscala_time.time.Imports._

/** Use this command to create test files when there's a breaking change to the files (i.e. SpatialKeyWritable package move) */
object GenerateTestFiles {
  def generate(catalog: HadoopRasterCatalog, sc: SparkContext): Unit = {
    val cellType = TypeFloat
    val layoutLevel = ZoomedLayoutScheme(3).levelFor(TestFiles.ZOOM_LEVEL)
    val tileLayout = layoutLevel.tileLayout
    /** HACK: each "cell" in RasterExtent is actually a tile from tileLayout
      * now I can use this to snap any Extent to my worldtile grid
      */
    val re = RasterExtent(LatLng.worldExtent, tileLayout.layoutCols, tileLayout.layoutRows)
    val extent = Extent(141.7066666666667, -18.373333333333342, 142.56000000000003, -17.52000000000001)
    val tileBounds = re.gridBoundsFor(extent)

    println(s"Tile Bounds: $tileBounds")

    /**
     * What I need now is a RasterExtent for the tile that will cover all the tiles in 'extent'
     * - tileBounds to find out how many tiles I have to cover
     * - tileLayout will tell me how many pixels each tile has
     * - re and tileBounds will tell me what extent for my overall tile is
     */
    val rasterExtent =
      RasterExtent(
        extent = re.extentFor(tileBounds),
        cols = tileBounds.width * tileLayout.tileCols,
        rows = tileBounds.height * tileLayout.tileRows
      )

    val (tileCols, tileRows) =  (rasterExtent.cols, rasterExtent.rows)

    // Generate Spatial Layers

    val spatialTestFiles = List(
      new ConstantSpatialTestFileValues(1) -> "all-ones",
      new ConstantSpatialTestFileValues(2) -> "all-twos",
      new ConstantSpatialTestFileValues(100) -> "all-hundreds",
      new IncreasingSpatialTestFileValues(tileCols, tileRows) -> "increasing",
      new DecreasingSpatialTestFileValues(tileCols, tileRows) -> "decreasing",
      new EveryOtherUndefined(tileCols) -> "every-other-undefined",
      new EveryOther0Point99Else1Point01(tileCols) -> "every-other-0.99-else-1.01",
      new EveryOther1ElseMinus1(tileCols) -> "every-other-1-else-1",
      new Mod(tileCols, tileRows, 10000) -> "mod-10000"
    )

    for((tfv, name) <- spatialTestFiles) {
      val cols = rasterExtent.cols
      val rows = rasterExtent.rows
      val tile = ArrayTile(tfv(cols, rows), cols, rows)

      val tmsTiles =
        tileBounds.coords.map { case (col, row) =>
          val targetRasterExtent =
            RasterExtent(
              extent = re.extentFor(GridBounds(col, row, col, row)),
              cols = tileLayout.tileCols,
              rows = tileLayout.tileRows
            )

          val subTile: Tile = tile.resample(rasterExtent.extent, targetRasterExtent)
          (SpatialKey(col, row), subTile)
        }

      val rdd =
        asRasterRDD(RasterMetaData(cellType, rasterExtent.extent, LatLng, tileLayout)) {
          sc.parallelize(tmsTiles)
        }

      catalog.writer[SpatialKey](clobber = true).write(LayerId(s"$name", TestFiles.ZOOM_LEVEL), rdd)
    }

    // Generate SpaceTime layers

    // Yearly from 2010 - 2014
    val times = 
      (0 to 4).map(i => new DateTime(2010 + i, 1, 1, 0, 0, 0, DateTimeZone.UTC)).toArray

    val spaceTimeTestFiles = List(
      new ConstantSpaceTimeTestFileValues(1) -> "spacetime-all-ones",
      new ConstantSpaceTimeTestFileValues(2) -> "spacetime-all-twos",
      new ConstantSpaceTimeTestFileValues(100) -> "spacetime-all-hundreds",
      new CoordinateSpaceTimeTestFileValues -> "spacetime-coordinates"
    )

    println(tileBounds)

    for((tfv, name) <- spaceTimeTestFiles) {
      val cols = rasterExtent.cols
      val rows = rasterExtent.rows

      val tmsTiles =
        times.zipWithIndex.flatMap { case (time, i) =>
          val tile = ArrayTile(tfv(cols, rows, i), cols, rows)

          tileBounds.coords.map { case (col, row) =>
            val targetRasterExtent =
              RasterExtent(
                extent = re.extentFor(GridBounds(col, row, col, row)),
                cols = tileLayout.tileCols,
                rows = tileLayout.tileRows
              )

            val subTile: Tile = tile.resample(rasterExtent.extent, targetRasterExtent)
            (SpaceTimeKey(col, row, time), subTile)
          }
        }

      val rdd =
        asRasterRDD(RasterMetaData(cellType, rasterExtent.extent, LatLng, tileLayout)) {
          sc.parallelize(tmsTiles)
        }

      catalog.writer[SpaceTimeKey](clobber = true).write(LayerId(s"$name", TestFiles.ZOOM_LEVEL), rdd)
    }

  }

  def main(args: Array[String]): Unit = {
    val sc = new SparkContext("local", "create-test-files")
    val catalog = TestFiles.catalog(sc)

    generate(catalog, sc)
  }
}

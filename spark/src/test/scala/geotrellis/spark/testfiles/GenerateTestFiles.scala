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
import geotrellis.spark.io.index._
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
  def generate(catalog: HadoopRasterCatalog)(implicit sc: SparkContext): Unit = {
    val cellType = TypeFloat
    val crs = LatLng
    val tileLayout = TileLayout(8, 8, 3, 4)
    val mapTransform = MapKeyTransform(crs, tileLayout.layoutDimensions)
    val gridBounds = GridBounds(1, 1, 6, 7)
    val extent = mapTransform(gridBounds)

    val md = RasterMetaData(cellType, LayoutDefinition(crs.worldExtent, tileLayout), extent, crs)

    generateSpatial(catalog, md)
    generateSpaceTime(catalog, md)
  }

  def generateSpatial(catalog: HadoopRasterCatalog, md: RasterMetaData)(implicit sc: SparkContext): Unit = {
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

    for((tfv, name) <- spatialTestFiles) {
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

//      println(rdd.stitch.asciiDraw)

      catalog.writer[SpatialKey](RowMajorKeyIndexMethod, clobber = true).write(LayerId(s"$name", TestFiles.ZOOM_LEVEL), rdd)
    }

  }

  def generateSpaceTime(catalog: HadoopRasterCatalog, md: RasterMetaData)(implicit sc: SparkContext): Unit = {
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

    for((tfv, name) <- spaceTimeTestFiles) {
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

//      println(rdd.stitch.asciiDraw)

      catalog.writer[SpaceTimeKey](ZCurveKeyIndexMethod.byYear, clobber = true).write(LayerId(s"$name", TestFiles.ZOOM_LEVEL), rdd)
    }

  }
  def main(args: Array[String]): Unit = {
    implicit val sc = SparkUtils.createLocalSparkContext("local", "create-test-files")
    val localFS = TestFiles.catalogPath.getFileSystem(sc.hadoopConfiguration)
    if(localFS.exists(TestFiles.catalogPath)) {
      println("Deleting old catalog")
      localFS.delete(TestFiles.catalogPath, true)
    }

    // This will cause the catalog to be generated.
    val catalog = TestFiles.catalog(sc)
  }
}

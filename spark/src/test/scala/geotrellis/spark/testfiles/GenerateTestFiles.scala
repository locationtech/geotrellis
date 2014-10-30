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

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.proj4._
import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.tiling._
import geotrellis.spark.utils._
import org.apache.spark._

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

/** Use this command to create test files when there's a breaking change to the files (i.e. SpatialKeyWritable package move) */
object GenerateTestFiles {

  def main(args: Array[String]): Unit = {
    val cellType = TypeFloat
    val extent = Extent(141.7066666666667, -18.373333333333342, 142.56000000000003, -17.52000000000001)
    val layoutLevel = TilingScheme.TMS.level(10)
    val metaData = LayerMetaData(cellType, extent, LatLng, layoutLevel, RowIndexScheme)
    val (tileCols, tileRows) =  (metaData.tileLayout.tileCols, metaData.tileLayout.tileRows)
    val tileSize = tileCols * tileRows

    val testFiles = List(
      ConstantTestFileValues(1) -> "all-ones",
      ConstantTestFileValues(2) -> "all-twos",
      ConstantTestFileValues(100) -> "all-hundreds",
      IncreasingTestFileValues(tileCols, tileRows) -> "increasing",
      DecreasingTestFileValues(tileCols, tileRows) -> "decreasing",
      EveryOtherUndefined(tileCols) -> "every-other-undefined",
      EveryOther0Point99Else1Point01(tileCols) -> "every-other-0.99-else-1.01",
      EveryOther1ElseMinus1(tileCols) -> "every-other-1-else-1"
    )

    val sc = new SparkContext("local", "create-test-files")
    val conf = sc.hadoopConfiguration
    val localFS = new Path(System.getProperty("java.io.tmpdir")).getFileSystem(conf)
    val prefix = new Path(localFS.getWorkingDirectory, "src/test/resources")

    for((tfv, name) <- testFiles) {
      println(s"Creating test RasterRDD $name")
      val tmsTiles =
        metaData.tileIds.map { tileId =>
          val arr = tfv(tileCols, tileRows)
          (tileId, ArrayTile(arr, tileCols, tileRows))
        }

      val path = new Path(prefix, s"$name/${metaData.level.id}")
      localFS.delete(path, true)

      val partitioner = {
        val tileSizeBytes = metaData.tileLayout.tileCols * metaData.tileLayout.tileRows * cellType.bytes
        val blockSizeBytes = HdfsUtils.defaultBlockSize(path, conf)
        val splitGenerator =
          RasterSplitGenerator(metaData.gridBounds, metaData.transform, tileSizeBytes, blockSizeBytes)
        TileIdPartitioner(splitGenerator.splits)
      }

      val rdd =
        sc.parallelize(tmsTiles)
          .partitionBy(partitioner)
          .toRasterRDD(metaData)

      rdd.saveAsHadoopRasterRDD(path)
    }
  }

}

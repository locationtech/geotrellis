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
import geotrellis.vector.Extent
import geotrellis.proj4._

import geotrellis.spark._
import geotrellis.spark.utils._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.tiling._

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io._
import org.scalatest._

import java.awt.image.DataBuffer

/*
 * Tests both local and spark ingest mode
 */
class IngestSpec extends FunSpec
                    with TestEnvironment
                    with RasterVerifyMethods
                    with OnlyIfCanRunSpark {

  // subdirectories under the test directory for each of the two modes
  val sparkTestOutput = new Path(outputLocal, "spark")

  clearTestDirectory()

  describe("Spark Ingest") {
    ifCanRunSpark {
      val allOnes = new Path(inputHome, "all-ones.tif")

      val cmd = s"--input ${allOnes.toString} --outputpyramid ${sparkTestOutput} --sparkMaster local"
      HadoopIngestCommand.main(cmd.split(' '))

      val rasterPath = new Path(sparkTestOutput, "10")
      val metaData = HadoopUtils.readLayerMetaData(rasterPath, conf)

      it("should create the correct metadata") {
        verifyMetadata(metaData)
      }

      it("should have the right zoom level directory") {
        verifyZoomLevelDirectory(rasterPath)
      }

      it("should have the right number of splits for the base zoom level") {
        verifyPartitions(rasterPath)
      }

      it("should have the correct tiles (checking tileIds)") {
        verifyTiles(rasterPath, metaData)
      }

      it("should have its data files compressed") {
        verifyCompression(rasterPath)
      }

      it("should have its block size set correctly") {
        verifyBlockSize(rasterPath)
      }
    }
  }

  private def verifyMetadata(actualMeta: LayerMetaData): Unit = {
    val expectedMeta = LayerMetaData(
      TypeFloat,
      Extent(141.7066666666667, -18.373333333333342, 142.56000000000003, -17.52000000000001),
      LatLng,
      TilingScheme.TMS.level(10),
      RowIndexScheme
    )

    actualMeta should equal(expectedMeta)
  }
}

trait RasterVerifyMethods extends ShouldMatchers { self: TestEnvironment =>
  def verifyZoomLevelDirectory(raster: Path): Unit =
    localFS.exists(raster) should be(true)

  def verifyPartitions(raster: Path): Unit = {
    val partitioner = TileIdPartitioner(HadoopUtils.readSplits(raster, conf))
    partitioner.numPartitions should be(1)
  }

  def verifyTiles(raster: Path, meta: LayerMetaData): Unit = {
    val expectedSpatialKeys = meta.transform.mapToIndex(meta.extent)

    val reader = RasterReader(raster, conf)
    val actualSpatialKeys = reader.map { case (tw, aw) => tw.get }.toList
    reader.close()

    actualSpatialKeys should be(expectedSpatialKeys)
  }

  def verifyCompression(raster: Path): Unit = {
    val dataFile = new Path(new Path(raster, "part-00000"), "data")
    val dataReader =
      HdfsUtils.getSequenceFileReader(localFS, dataFile, conf)
    val isCompressed = dataReader.isCompressed()
    dataReader.close()
    isCompressed should be(true)
  }

  def verifyBlockSize(raster: Path): Unit = {
    val expectedBlockSize = localFS.getDefaultBlockSize(raster)
    val actualBlockSize = localFS.getFileStatus(raster).getBlockSize()
    actualBlockSize should be(expectedBlockSize)
  }
}

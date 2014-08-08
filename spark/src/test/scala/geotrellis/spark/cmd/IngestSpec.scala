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

package geotrellis.spark.cmd
import geotrellis.vector.Extent
import geotrellis.spark.TestEnvironment
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.metadata.RasterMetadata
import geotrellis.spark.rdd.TileIdPartitioner
import geotrellis.spark.storage.RasterReader
import geotrellis.spark.tiling.PixelExtent
import geotrellis.spark.tiling.TileExtent
import geotrellis.spark.tiling.TmsTiling

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.SequenceFile
import org.scalatest._

import java.awt.image.DataBuffer

/*
 * Tests both local and spark ingest mode
 */
class IngestSpec extends FunSpec with TestEnvironment with RasterVerifyMethods {

  // subdirectories under the test directory for each of the two modes
  val localTestOutput = new Path(outputLocal, "local")
  val sparkTestOutput = new Path(outputLocal, "spark")

  // describe("Local Ingest") {
  //   val allOnes = new Path(inputHome, "all-ones.tif")

  //   // do the actual ingest in local mode
  //   val cmd = s"--input ${allOnes.toString} --outputpyramid ${localTestOutput}"
  //   IngestCommand.main(cmd.split(' '))

  //   val raster = new Path(localTestOutput, "10")
  //   val meta = PyramidMetadata(localTestOutput, conf)

  //   it("should create the correct metadata") {
  //     verifyMetadata(meta)
  //   }

  //   it("should have the right zoom level directory") {
  //     verifyZoomLevelDirectory(raster)
  //   }

  //   it("should have the right number of splits for the base zoom level") {
  //     verifyPartitions(raster)
  //   }

  //   it("should have the correct tiles (checking tileIds)") {
  //     verifyTiles(raster, meta)
  //   }

  //   it("should have its data files compressed") {
  //     verifyCompression(raster)
  //   }

  //   it("should have its block size set correctly") {
  //     verifyBlockSize(raster)
  //   }
  // }

  describe("Spark Ingest") {

    val allOnes = new Path(inputHome, "all-ones.tif")

    // do the actual ingest in spark mode
    val cmd = s"--input ${allOnes.toString} --outputpyramid ${sparkTestOutput} --sparkMaster local"
    IngestCommand.main(cmd.split(' '))

    val raster = new Path(sparkTestOutput, "10")
    val meta = PyramidMetadata(sparkTestOutput, conf)

    it("should create the correct metadata") {
      verifyMetadata(meta)
    }

    it("should have the right zoom level directory") {
      verifyZoomLevelDirectory(raster)
    }

    it("should have the right number of splits for the base zoom level") {
      verifyPartitions(raster)
    }

    it("should have the correct tiles (checking tileIds)") {
      verifyTiles(raster, meta)
    }

    it("should have its data files compressed") {
      verifyCompression(raster)
    }

    it("should have its block size set correctly") {
      verifyBlockSize(raster)
    }
  }

  private def verifyMetadata(actualMeta: PyramidMetadata): Unit = {
    val expectedMeta = PyramidMetadata(
      Extent(141.7066666666667, -18.373333333333342, 142.56000000000003, -17.52000000000001),
      512,
      1,
      -9999.0,
      DataBuffer.TYPE_FLOAT,
      10,
      Map("10" -> new RasterMetadata(PixelExtent(0, 0, 1243, 1243), TileExtent(915, 203, 917, 206))))

    actualMeta should be(expectedMeta)
  }
}

trait RasterVerifyMethods extends ShouldMatchers { self: TestEnvironment =>
  def verifyZoomLevelDirectory(raster: Path): Unit =
    localFS.exists(raster) should be(true)

  def verifyPartitions(raster: Path): Unit = {
    val partitioner = TileIdPartitioner(raster, conf)
    partitioner.numPartitions should be(1)
  }

  def verifyTiles(raster: Path, meta: PyramidMetadata): Unit = {
    val zoom = raster.getName()
    val reader = RasterReader(raster, conf)
    val actualTileIds = reader.map { case (tw, aw) => tw.get }.toList
    val tileExtent = meta.rasterMetadata(zoom).tileExtent
    val expectedTileIds = for {
      ty <- tileExtent.ymin to tileExtent.ymax
      tx <- tileExtent.xmin to tileExtent.xmax
    } yield TmsTiling.tileId(tx, ty, zoom.toInt)
    reader.close()
    actualTileIds should be(expectedTileIds)
  }

  def verifyCompression(raster: Path): Unit = {
    val dataFile = new Path(new Path(raster, "part-00000"), "data")
    val dataReader = new SequenceFile.Reader(localFS, dataFile, conf)
    val isCompressed = dataReader.isCompressed()
    dataReader.close()
    isCompressed should be(true)
  }

  def verifyBlockSize(raster: Path): Unit = {
    val expectedBlockSize = localFS.getDefaultBlockSize()
    val actualBlockSize = localFS.getFileStatus(raster).getBlockSize()
    actualBlockSize should be(expectedBlockSize)
  }
}

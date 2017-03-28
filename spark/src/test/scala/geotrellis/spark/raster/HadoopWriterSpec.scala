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

package geotrellis.spark.raster

import geotrellis.spark._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.testkit._
import geotrellis.spark.testkit.TestEnvironment

import org.apache.commons.io.IOUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.io.compress.CompressionCodecFactory
import org.scalatest._

import java.io._

object RasterHadoopReader {
  def apply[T](path: Path, conf: Configuration)(dosRead: DataInputStream => T): T = {
    val fs = FileSystem.get(conf)

    val is = {
      val factory = new CompressionCodecFactory(conf)
      val codec = factory.getCodec(path)

      if (codec == null) {
        println(s"No codec found for $path, writing without compression.")
        fs.open(path)
      } else {
        codec.createInputStream(fs.open(path))
      }
    }
    try {
      val dos = new DataInputStream(is)
      try {
        dosRead(dos)
      } finally {
        dos.close
      }
    } finally {
      is.close
    }
  }
}

class HadoopWriterSpec extends FunSpec
  with Matchers
  with BeforeAndAfterAll
  with RasterMatchers
  with TileBuilders
  with TestEnvironment {

  val testDirPath = "raster-test/data/geotiff-test-files"

  describe ("writing GeoTiffs without errors and with correct tiles, crs and extent") {
    val temp = File.createTempFile("geotiff-writer", ".tif")
    val path = temp.getPath

    it("should write GeoTiff with tags") {
      val existencePath = s"$testDirPath/multi-tag.tif"

      val geoTiff = MultibandGeoTiff(existencePath)

      val expected = geoTiff.tile
      val expectedTags = geoTiff.tags

      geoTiff.write(new Path(path))

      val actualTiff = RasterHadoopReader(new Path(path), sc.hadoopConfiguration) { is => MultibandGeoTiff(IOUtils.toByteArray(is)) }
      val actual = actualTiff.tile
      val actualTags = actualTiff.tags

      actual should be (expected)
      actualTags should be (expectedTags)
    }
  }
}

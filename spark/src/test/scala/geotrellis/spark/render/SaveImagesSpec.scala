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

package geotrellis.spark.render

import geotrellis.proj4.CRS
import geotrellis.raster.{Tile, TileLayout}
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.render._
import geotrellis.spark.testkit.testfiles.TestFiles
import geotrellis.spark.store.hadoop._
import geotrellis.spark.testkit._
import org.scalatest._
import org.apache.hadoop.fs._
import org.apache.hadoop.conf._
import org.apache.commons.io.IOUtils
import java.net.URI

import geotrellis.layers.LayerId

class SaveImagesSpec extends FunSpec with TestEnvironment {
  lazy val sample = TestFiles.generateSpatial("all-ones")
  val tmpdir = System.getProperty("java.io.tmpdir")
  val fs = FileSystem.get(new URI(tmpdir), new Configuration)
  def readFile(path: String): Array[Byte] = {
    IOUtils.toByteArray(fs.open(new Path(path)))
  }

  describe("Saving of Rendered Tiles to Hadoop") {
    it ("should work with PNGs") {
      val template = s"${outputLocal}/testFiles/catalog/{name}/{z}/{x}/{y}.png"
      val id = LayerId("sample", 1)
      val keyToPath = SaveToHadoop.spatialKeyToPath(id, template)
      val rdd = sample.renderPng().mapValues(_.bytes)
      rdd.saveToHadoop(keyToPath)
      val ol = outputLocal
      rdd.collect().foreach { case key @ (SpatialKey(col, row), bytes) =>
        val path = s"${ol}/testFiles/catalog/sample/1/$col/$row.png"
        readFile(path) should be (bytes)
      }
    }

    it ("should work with JPGs") {
      val template = s"${outputLocal}/testFiles/catalog/{name}/{z}/{x}/{y}.jpg"
      val id = LayerId("sample", 1)
      val keyToPath = SaveToHadoop.spatialKeyToPath(id, template)
      val rdd = sample.renderJpg().mapValues(_.bytes)
      rdd.saveToHadoop(keyToPath)
      val ol = outputLocal
      rdd.collect().foreach { case key @ (SpatialKey(col, row), bytes) =>
        val path = s"${ol}/testFiles/catalog/sample/1/$col/$row.jpg"
        readFile(path) should be (bytes)
      }
    }

    it ("should work with GeoTiffs") {
      val template = s"${outputLocal}/testFiles/catalog/{name}/{z}/{x}/{y}.tiff"
      val id = LayerId("sample", 1)
      val keyToPath = SaveToHadoop.spatialKeyToPath(id, template)
      val rdd = sample.renderGeoTiff().mapValues(_.toByteArray)
      rdd.saveToHadoop(keyToPath)
      val ol = outputLocal
      rdd.collect().foreach { case key @ (SpatialKey(col, row), bytes) =>
        val path = s"${ol}/testFiles/catalog/sample/1/$col/$row.tiff"
        readFile(path) should be (bytes)
      }
    }
  }
}

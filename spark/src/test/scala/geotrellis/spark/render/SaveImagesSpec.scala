package geotrellis.spark.render

import geotrellis.proj4.CRS
import geotrellis.raster.{Tile, TileLayout}
import geotrellis.spark._
import geotrellis.spark.render._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark.io.hadoop._

import org.scalatest._
import org.apache.hadoop.fs._
import org.apache.hadoop.conf._
import org.apache.commons.io.IOUtils
import java.net.URI

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

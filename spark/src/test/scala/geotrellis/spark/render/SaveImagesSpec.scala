package geotrellis.spark.render

import geotrellis.raster.{Tile, TileLayout}
import geotrellis.spark.{GridKey, LayerId}
import geotrellis.spark.TestEnvironment
import geotrellis.spark.render._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark.io.hadoop._

import org.scalatest._


class SaveImagesSpec extends FunSpec with TestEnvironment {
  lazy val sample = TestFiles.generateSpatial("all-ones")
  val tmpdir = System.getProperty("java.io.tmpdir")

  describe("Saving of Rendered Tiles to Hadoop") {
    it ("should work with PNGs") {
      val template = s"file:${tmpdir}/testFiles/catalog/{name}/{z}/{x}/{y}.png"
      val id = LayerId("sample", 1)
      val keyToPath = SaveToHadoopMethods.spatialKeyToPath(id, template)
      sample.renderPng().saveToHadoop(keyToPath)
    }

    it ("should work with JPGs") {
      val template = s"file:${tmpdir}/testFiles/catalog/{name}/{z}/{x}/{y}.jpg"
      val id = LayerId("sample", 1)
      val keyToPath = SaveToHadoopMethods.spatialKeyToPath(id, template)
      sample.renderJpg().saveToHadoop(keyToPath)
    }

    it ("should work with GeoTiffs") {
      val template = s"file:${tmpdir}/testFiles/catalog/{name}/{z}/{x}/{y}.tiff"
      val id = LayerId("sample", 1)
      val keyToPath = SaveToHadoopMethods.spatialKeyToPath(id, template)
      sample.renderGeoTiff().saveToHadoop(keyToPath)
    }
  }
}

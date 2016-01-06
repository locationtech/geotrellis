package geotrellis.spark.io.slippy

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.proj4._
import geotrellis.vector._

import geotrellis.spark.testfiles._

import org.scalatest._
import java.io.File

class HadoopSlippyTileWriterSpec 
    extends FunSpec 
    with Matchers 
    with TestEnvironment 
    with TestFiles
    with RasterRDDMatchers {
  describe("HadoopSlippyTileWriter") {
    val testPath = new File(outputLocalPath, "slippy-write-test").getPath

    it("can write slippy tiles") {
      val mapTransform = ZoomedLayoutScheme(WebMercator).levelForZoom(TestFiles.ZOOM_LEVEL).layout.mapTransform

      val writer =
        new HadoopSlippyTileWriter[Tile](testPath, "tif")({ (key, tile) =>
          SingleBandGeoTiff(tile, mapTransform(key), WebMercator).toByteArray
        })

      writer.write(TestFiles.ZOOM_LEVEL, AllOnesTestFile)

      val reader =
        new FileSlippyTileReader[Tile](testPath)({ (key, bytes) =>
          SingleBandGeoTiff(bytes).tile
        })

      rastersEqual(reader.read(TestFiles.ZOOM_LEVEL), AllOnesTestFile)

    }
  }
}

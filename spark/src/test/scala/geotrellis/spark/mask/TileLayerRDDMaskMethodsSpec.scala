package geotrellis.spark.mask

import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.spark._
import geotrellis.spark.testkit._
import geotrellis.vector._

import org.scalatest._

class TileLayerRDDMaskMethodsSpec extends FunSpec
    with Matchers
    with TestEnvironment
    with TileLayerRDDBuilders
    with RasterMatchers {
  describe("TileLayerRDDMask") {
    it("should properly mask a float32 user defined nodata layer") {
      val arr = (1 to (6*4)).map(_.toFloat).toArray
      arr(17) = -1.1f
      val sourceTile = FloatArrayTile(arr, 6, 4, FloatUserDefinedNoDataCellType(-1.1f))

      val (tile, layer) =
        createTileLayerRDD(sourceTile, 2, 2)

      val Extent(xmin, ymin, xmax, ymax) = layer.metadata.extent
      val dx = (xmax - xmin) / 3
      val dy = (ymax - ymin) / 2
      val mask = Extent(xmin + dx, ymin, xmax, ymin + dy)

      val n = -1.1f
      assertEqual(layer.mask(mask.toPolygon).stitch.tile,
        FloatArrayTile(
          Array(
            n, n, n,  n, n, n,
            n, n, n,  n, n, n,
            n, n, 15, 16, 17, n,
            n, n, 21, 22, 23, 24
          ), 6, 4, FloatUserDefinedNoDataCellType(-1.1f)
        )
      )
    }
  }

}

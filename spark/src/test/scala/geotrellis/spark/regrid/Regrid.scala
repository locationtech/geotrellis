package geotrellis.spark.regrid

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.spark._
import geotrellis.spark.testkit._
import geotrellis.spark.tiling._
import geotrellis.vector._

import org.scalatest._

class RegridSpec extends FunSpec with TestEnvironment with RasterMatchers {

  val simpleLayer = {
    val tiles = 
      for ( x <- 0 to 3 ;
            y <- 0 to 2
      ) yield {
        val tile = IntArrayTile.ofDim(32, 32)
        (SpatialKey(x, y), tile.map{ (tx, ty, _) => math.max(tx + 32 * x, ty + 32 * y) })
      }
    val rdd = sc.parallelize(tiles)
    val ex = Extent(0,0,12.8,9.6)
    val ld = LayoutDefinition(GridExtent(ex, 0.1, 0.1), 32, 32)
    val md = TileLayerMetadata[SpatialKey](IntConstantNoDataCellType,
                                           ld,
                                           ex,
                                           LatLng,
                                           KeyBounds[SpatialKey](SpatialKey(0,0), SpatialKey(3,2)))
    ContextRDD(rdd, md)
  }

  describe("Regridding") {
    it("should allow chipping into smaller tiles") {
      val newLayer = simpleLayer.regrid(16)

      // import geotrellis.raster.render._
      // val cm = ColorMap((0 to 128).toArray, ColorRamps.Plasma)
      // newLayer.stitch.tile.renderPng(cm).write("regrid_16.png")

      assert(simpleLayer.stitch.dimensions == newLayer.stitch.dimensions)
      assertEqual(simpleLayer.stitch, newLayer.stitch)
    }

    it("should allow joining into larger tiles") {
      val newLayer = simpleLayer.regrid(64)

      // import geotrellis.raster.render._
      // val cm = ColorMap((0 to 128).toArray, ColorRamps.Plasma)
      // newLayer.stitch.tile.renderPng(cm).write("regrid_64.png")

      assert(newLayer.stitch.dimensions == (128, 128))
      assertEqual(simpleLayer.stitch, newLayer.stitch.tile.crop(0,0,127,95))
    }

    it("should allow breaking into non-square tiles") {
      val newLayer = simpleLayer.regrid(50, 25)

      // import geotrellis.raster.render._
      // val cm = ColorMap((0 to 128).toArray, ColorRamps.Plasma)
      // newLayer.stitch.tile.renderPng(cm).write("regrid_50_25.png")

      assert(newLayer.stitch.dimensions == (150, 100))
      assertEqual(simpleLayer.stitch, newLayer.stitch.tile.crop(0,0,127,95))
    }
  }

}

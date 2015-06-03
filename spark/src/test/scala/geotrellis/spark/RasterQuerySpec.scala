package geotrellis.spark

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.spark._
import geotrellis.proj4._
import geotrellis.spark.tiling._
import geotrellis.spark.testfiles._

import org.scalatest.FunSpec

class RasterQuerySpec extends FunSpec
  with TestEnvironment  
{

  describe("RasterQuerySpec") {
    val md = RasterMetaData(
      TypeFloat,
      Extent(-135.00000125, -89.99999, 134.99999125, 67.49999249999999),
      LatLng,
      TileLayout(8,8,3,4))

    val keyBounds = KeyBounds(SpatialKey(1,1), SpatialKey(6,7))

    it("should be better then Java serialization") {
      val query = new RasterRDDQuery[SpatialKey].where(GridBounds(2,2,2,2))
      val outKeyBounds = query(md, keyBounds)
      info(outKeyBounds.toString)
    }
  }
}
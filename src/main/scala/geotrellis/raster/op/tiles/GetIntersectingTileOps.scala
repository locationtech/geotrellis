package geotrellis.raster.op.tiles

import geotrellis._
import geotrellis.raster._
import geotrellis.feature._

/** Get operations to load the tiles of this raster that intersect with a polygon. 
  *
  * Note that the returned tiles are lazily loaded, so that they
  * can still be dispatched across the network without passing data. 
  */
case class GetIntersectingTileOps(r: Op[Raster], p:Op[Polygon[_]]) extends Op2(r,p)({
  (r,p) => { 
    val tileOps = r.data match { 
      case t:TiledRasterData => t.getTileOpList(r.rasterExtent, p)
      case _ => Literal(r) :: Nil
    }
    Result(tileOps)
  }
})

package geotrellis.raster.op.tiles

import geotrellis._
import geotrellis.raster._
import geotrellis.feature._

/** Get operations to load the tiles of this raster. 
  *
  * Note that the returned tiles are lazily loaded, so that they
  * can still be dispatched across the network without passing data. 
  */
// case class GetTileOps(r: Op[Raster]) extends Op1(r)({
//   r => { 
//     val tileOps = r.data match { 
//       case t:TiledRasterData => t.getTileOpList(r.rasterExtent)
//       case _ => Literal(r) :: Nil
//     }
//     Result(tileOps)
//   }
// })

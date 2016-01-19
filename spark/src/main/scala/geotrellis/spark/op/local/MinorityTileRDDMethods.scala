package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.raster._
import geotrellis.raster.op.local.Minority

trait MinorityTileRDDMethods[K] extends TileRDDMethods[K] {
  /**
    * Assigns to each cell the value within the given rasters that is the least
    * numerous.
    */
  def localMinority(others: Traversable[Self]) =
    self.combineValues(others)(Minority.apply)

  /**
    * Assigns to each cell the value within the given rasters that is the least
    * numerous.
    */
  def localMinority(rs: Self*)(implicit d: DI): Self =
    localMinority(rs)

  /**
    * Assigns to each cell the value within the given rasters that is the nth
    * least numerous.
    */
  def localMinority(n: Int, others: Traversable[Self]) =
    self.combineValues(others) { tiles => Minority(n, tiles) }

  /**
    * Assigns to each cell the value within the given rasters that is the nth
    * least numerous.
    */
  def localMinority(n: Int, rs: Self*)(implicit d: DI): Self =
    localMinority(n, rs)
}
